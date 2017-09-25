/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import connectors.{CompanyRegistrationConnector, KeystoreConnector, S4LConnector}
import models._
import models.handoff._
import play.api.Logger
import play.api.libs.json.{Format, JsValue}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{DecryptionError, Jwe, SCRSExceptions}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}
import repositories._

object HandBackService extends HandBackService{
  val compRegConnector = CompanyRegistrationConnector
  val keystoreConnector = KeystoreConnector
  val s4LConnector = S4LConnector
  val navModelMongo =  NavModelRepo.repository
}

case object PayloadNotSavedError extends NoStackTrace

trait HandBackService extends CommonService with SCRSExceptions with HandOffNavigator with ServicesConfig {

  val compRegConnector: CompanyRegistrationConnector
  val keystoreConnector: KeystoreConnector
  val s4LConnector : S4LConnector


  private[services] def decryptHandBackRequest[T](request: String)(f: T => Future[Try[T]])(implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Try[T]] = {
    request.isEmpty match {
      case true =>
        Logger.error(s"[HandBackService] [decryptHandBackRequest] Encrypted hand back payload was empty")
        Future.successful(Failure(DecryptionError))
      case false => Jwe.decrypt[T](request) match {
        case Success(payload) => f(payload)
        case Failure(ex) =>
          Logger.error(s"[HandBackService] [decryptHandBackRequest] Payload could not be decrypted: ${ex}")
          Future.successful(Failure(ex))
      }
    }
  }

  def processCompanyNameReverseHandBack(request: String)(implicit user:AuthContext, hc: HeaderCarrier): Future[Try[JsValue]] = {
    decryptHandBackRequest[JsValue](request){ res =>
      //todo: SCRS-3193 - compare journey id against one in session for error handling
      //(res \ "journey_id").as[String]

      Future.successful(Success(res))
    }
  }

  def processBusinessActivitiesHandBack(request: String)(implicit user:AuthContext, hc: HeaderCarrier): Future[Try[JsValue]] = {
    decryptHandBackRequest[JsValue](request){ res =>
      Future.successful(Success(res))
    }
  }

  def processCompanyDetailsHandBack(request : String)(implicit user : AuthContext, hc : HeaderCarrier) : Future[Try[CompanyNameHandOffIncoming]] = {
    decryptHandBackRequest[CompanyNameHandOffIncoming](request){
      payload =>
        fetchNavModel() map {
          model =>
            val jumpLinks = payload.links.as[JumpLinks]
            implicit val updatedNavModel = model.copy(
              receiver = model.receiver.copy(
                nav = model.receiver.nav ++ Map("2" -> payload.links.as[NavLinks]),
                chData = Some(payload.ch),
                jump = Map(
                  "company_name" -> jumpLinks.company_name,
                  "company_address" -> jumpLinks.company_address,
                  "company_jurisdiction" -> jumpLinks.company_jurisdiction
                )
              )
            )
            cacheNavModel
        }

        storeCompanyDetails(payload) map {
//          case false =>
//            Logger.error("[HandBackService] [processCompanyDetailsHandOff] CH handoff payload wasn't stored")
//            Failure(PayloadNotSavedError)
          case _ => Success(payload)
      }
    }
  }

  def processSummaryPage1HandBack(request : String)(implicit user : AuthContext, hc : HeaderCarrier) : Future[Try[SummaryPage1HandOffIncoming]] = {
    decryptHandBackRequest[SummaryPage1HandOffIncoming](request){
      payload =>
        fetchNavModel() map {
          model =>
            implicit val updatedModel = model.copy(
              receiver = {
                model.receiver.copy(
                  nav = model.receiver.nav ++ Map("4" -> payload.links),
                  chData = Some(payload.ch)
                )
              })
            cacheNavModel
        }

        storeSimpleHandOff(payload) map {
          case false =>
            Logger.error("[HandBackService] [processSummaryPage1Handback] CH handoff payload wasn't stored")
            Failure(PayloadNotSavedError)
          case _ => Success(payload)
      }
    }
  }

  def decryptConfirmationHandback(request : String)(implicit user : AuthContext, hc : HeaderCarrier) : Future[Try[RegistrationConfirmationPayload]] = {
    decryptHandBackRequest[RegistrationConfirmationPayload](request){res => Future.successful(Success(res))}
  }

  private[services] def updateCompanyDetails(registrationID: String, handoff: CompanyNameHandOffIncoming)(implicit hc: HeaderCarrier): Future[CompanyDetails] = {
    compRegConnector.retrieveCompanyDetails(registrationID).map{
      case Some(existing) => {
        CompanyDetails.updateFromHandoff(existing, handoff.company_name, handoff.registered_office_address, handoff.jurisdiction)
      }
      case None => {
        CompanyDetails.createFromHandoff(handoff.company_name, handoff.registered_office_address, PPOB("", None), handoff.jurisdiction)
      }
    } flatMap {
      details =>
        compRegConnector.updateCompanyDetails(registrationID, details)
    }
  }

  private[services] def storeCompanyDetails(payload : CompanyNameHandOffIncoming)(implicit user : AuthContext, hc : HeaderCarrier) : Future[CompanyDetails] = {
    for {
      regID <- fetchRegistrationID
      updated <- updateCompanyDetails(regID, payload)
    } yield updated
  }

  private[services] def storeSimpleHandOff(payload : SummaryPage1HandOffIncoming)(implicit user : AuthContext, hc : HeaderCarrier) : Future[Boolean] = {
    for {
      regID <- fetchRegistrationID
      //chUpdated <- handOffConnector.updateCHData(regID, CompanyNameHandOffInformation("full-data", DateTime.now, payload.ch))
      //TODO : Change return
    } yield true
  }

  def storeConfirmationHandOff(payload : RegistrationConfirmationPayload, regID : String)(implicit hc: HeaderCarrier): Future[ConfirmationReferencesResponse] = {
    compRegConnector.updateReferences(regID, RegistrationConfirmationPayload.getReferences(payload))
  }
}