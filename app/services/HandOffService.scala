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

import config.FrontendAuthConnector
import models.handoff.{BusinessActivitiesModel, CompanyNameHandOffModel, HandoffPPOB}
import models.{SummaryHandOff, UserDetailsModel, UserIDs}
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import models.handoff._
import play.api.libs.json.{JsObject, JsString, Json}
import repositories.NavModelRepo
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{Jwe, JweEncryptor, SCRSExceptions, SCRSFeatureSwitches}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object HandOffService extends HandOffService {
  val keystoreConnector = KeystoreConnector
  val returnUrl = s"${baseUrl("comp-reg-frontend")}"
  val compRegConnector = CompanyRegistrationConnector
  val encryptor = Jwe
  val authConnector = FrontendAuthConnector
  val navModelMongo =  NavModelRepo.repository
}

trait HandOffService extends CommonService with SCRSExceptions with ServicesConfig with HandOffNavigator {

  val keystoreConnector: KeystoreConnector
  val returnUrl: String
  val compRegConnector: CompanyRegistrationConnector
  val encryptor : JweEncryptor
  val authConnector: AuthConnector

  def buildHandOffUrl(url: String, payload: String) = url match {
    case u if u.endsWith("request=") => s"$url$payload"
    case u if u.endsWith("?") => s"${url}request=$payload"
    case u if u.endsWith("&") => s"${url}request=$payload"
    case u if u.contains("?") => s"$url&request=$payload"
    case _ => s"$url?request=$payload"
  }

  def externalUserId(implicit user: AuthContext, hc: HeaderCarrier): Future[String] = {
    authConnector.getIds[UserIDs](user) map { _.externalId }
  }

  def getURL(path : String) = s"$returnUrl$path"

  def getUserDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[UserDetailsModel] = {
    authConnector.getUserDetails[UserDetailsModel](user)
  }

  def buildBusinessActivitiesPayload(regId: String)(implicit user: AuthContext, hc : HeaderCarrier) : Future[Option[(String, String)]] = {
    val navModel = fetchNavModel() map {
      implicit model =>
        (forwardTo(3), hmrcLinks(3), model.receiver.chData)
    }

    for {
      Some(addressData) <- compRegConnector.retrieveCompanyDetails(regId)
      (url, links, chData) <- navModel
      extUserID <- externalUserId
    } yield {
      val payload = BusinessActivitiesModel(
        authExtId = extUserID,
        regId = regId,
        ppob = Some(HandoffPPOB.fromCorePPOB(addressData.pPOBAddress)),
        ch = chData,
        hmrc = JsObject(Seq()),
        links = links
      )
      encryptor.encrypt[BusinessActivitiesModel](payload) map { (url, _) }
    }
  }

  def companyNamePayload(regId: String)(implicit user : AuthContext, hc : HeaderCarrier) : Future[Option[(String, String)]] = {

    val navModel = fetchNavModel(canCreate = true) map {
      implicit model =>
        (forwardTo(1), hmrcLinks(1), model.receiver.chData)
    }

    for {
      userDetails <- getUserDetails
      (url, links, chData) <- navModel
      extUserID <- externalUserId
    } yield {
      val payload = CompanyNameHandOffModel(
        email_address = userDetails.email,
        is_verified_email_address = None,
        journey_id = Some(regId),
        user_id = extUserID,
        name = userDetails.name,
        hmrc = JsObject(Seq()),
        ch = chData,
        return_url = url,
        links = links)
      encryptor.encrypt[CompanyNameHandOffModel](payload) map { (url, _) }
    }
  }

  def buildLinksObject(navLinks : NavLinks, jumpLinks: Option[JumpLinks]) : JsObject = {
    val obj = Json.obj("forward" -> navLinks.forward,"reverse" -> navLinks.reverse)

    jumpLinks.isDefined match {
      case false => obj
      case true =>
        obj.
          +("company_name" -> JsString(jumpLinks.get.company_name)).
          +("company_address" -> JsString(jumpLinks.get.company_address)).
          +("company_jurisdiction" -> JsString(jumpLinks.get.company_jurisdiction))
    }
  }

  def buildBackHandOff(implicit hc : HeaderCarrier, ac : AuthContext) : Future[BackHandoff] = {
    for {
      regID <- fetchRegistrationID
      extUserId <- externalUserId
      navModel <- fetchNavModel()
    } yield {
      BackHandoff(
        extUserId,
        regID,
        navModel.receiver.chData.get,
        Json.obj(),
        Json.obj()
      )
    }
  }

  def summaryHandOff(implicit hc : HeaderCarrier, ac : AuthContext) : Future[Option[(String, String)]] = {
    val navModel = fetchNavModel() map {
      implicit model =>
        (forwardTo(5), hmrcLinks(5), model.receiver.chData)
    }

    for {
      userID <- externalUserId
      journeyID <- fetchRegistrationID
      _ <- updateRegistrationProgressHO5(journeyID)
      (url, links, chData) <- navModel
    } yield {
      val payloadModel =
        SummaryHandOff(
          userID,
          journeyID,
          Json.obj(),
          chData,
          Json.toJson[NavLinks](links).as[JsObject]
        )
      encryptor.encrypt[SummaryHandOff](payloadModel).map((url, _))
    }
  }

  private[services] def updateRegistrationProgressHO5(registrationId: String)(implicit hc: HeaderCarrier) = {
    import constants.RegistrationProgressValues.HO5
    compRegConnector.updateRegistrationProgress(registrationId, HO5)
  }
}
