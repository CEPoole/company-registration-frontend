/*
 * Copyright 2018 HM Revenue & Customs
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

import _root_.connectors._
import config.{FrontendAppConfig, FrontendAuthConnector}
import models._
import models.external.{OtherRegStatus, Statuses}
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.{SCRSExceptions, SCRSFeatureSwitches}

import scala.concurrent.Future
import scala.util.control.NoStackTrace

object DashboardService extends DashboardService with ServicesConfig {
  val keystoreConnector = KeystoreConnector
  val companyRegistrationConnector = CompanyRegistrationConnector
  val incorpInfoConnector = IncorpInfoConnector
  val payeConnector = PAYEConnector
  val vatConnector = VATConnector
  val otrsUrl = getConfString("otrs.url", throw new Exception("Could not find config for key: otrs.url"))
  val payeBaseUrl = getConfString("paye-registration-www.url-prefix", throw new Exception("Could not find config for key: paye-registration-www.url-prefix"))
  val payeUri = getConfString("paye-registration-www.start-url", throw new Exception("Could not find config for key: paye-registration-www.start-url"))
  val vatBaseUrl = getConfString("vat-registration-www.url-prefix", throw new Exception("Could not find config for key: vat-registration-www.url-prefix"))
  val vatUri = getConfString("vat-registration-www.start-url", throw new Exception("Could not find config for key: vat-registration-www.start-url"))
  val featureFlag = SCRSFeatureSwitches
}

sealed trait DashboardStatus
case class DashboardBuilt(d: Dashboard) extends DashboardStatus
case object RejectedIncorp extends DashboardStatus
case object CouldNotBuild extends DashboardStatus

class ConfirmationRefsNotFoundException extends NoStackTrace

trait DashboardService extends SCRSExceptions with CommonService {
  import scala.language.implicitConversions

  val companyRegistrationConnector : CompanyRegistrationConnector
  val payeConnector, vatConnector: ServiceConnector
  val incorpInfoConnector: IncorpInfoConnector
  val otrsUrl: String
  val payeBaseUrl: String
  val payeUri: String
  val vatBaseUrl: String
  val vatUri: String
  val featureFlag: SCRSFeatureSwitches

  implicit def toDashboard(s: OtherRegStatus)(implicit startURL: String, cancelURL: Call): ServiceDashboard = {
    ServiceDashboard(s.status, s.lastUpdate.map(_.toString("d MMMM yyyy")), s.ackRef,
      ServiceLinks(startURL, otrsUrl, s.restartURL, s.cancelURL.map(_ => cancelURL.url)))
  }

  def buildDashboard(regId : String, enrolments : Enrolments)(implicit hc: HeaderCarrier): Future[DashboardStatus] = {
    for {
      incorpCTDash <- buildIncorpCTDashComponent(regId)
      payeDash <- buildPAYEDashComponent(regId, enrolments)
      hasVatCred = hasEnrolment(enrolments, List("HMCE-VATDEC-ORG", "HMCE-VATVAR-ORG"))
      vatDash <- buildVATDashComponent(regId, enrolments)
    } yield {
      incorpCTDash.status match {
        case "draft" => CouldNotBuild
        case "rejected" => RejectedIncorp
        //case _ => getCompanyName(regId) map(cN => DashboardBuilt(Dashboard(incorpCTDash, payeDash, cN)))
        case _ => DashboardBuilt(Dashboard("", incorpCTDash, payeDash, vatDash, hasVatCred)) //todo: leaving company name blank until story gets played to add it back
      }
    }
  }

  private[services] def hasEnrolment(authEnrolments : Enrolments, enrolmentKeys: Seq[String])(implicit hc: HeaderCarrier): Boolean = {
    authEnrolments.enrolments.exists(e => FrontendAppConfig.restrictedEnrolments.contains(e.key))
  }

  private[services] def statusToServiceDashboard(res: Future[StatusResponse], enrolments: Enrolments, payeEnrolments: Seq[String])
                                                (implicit hc: HeaderCarrier, startURL: String, cancelURL: Call): Future[ServiceDashboard] = {
    res map {
      case SuccessfulResponse(status) => status
      case ErrorResponse => OtherRegStatus(Statuses.UNAVAILABLE, None, None, None, None)
      case NotStarted => OtherRegStatus(
        if (hasEnrolment(enrolments, payeEnrolments)) Statuses.NOT_ELIGIBLE else Statuses.NOT_STARTED, None, None, None, None
      )
    }
  }

  private[services] def buildPAYEDashComponent(regId: String, enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[ServiceDashboard] = {
    implicit val startURL: String = s"$payeBaseUrl$payeUri"
    implicit val cancelURL: Call = controllers.dashboard.routes.CancelRegistrationController.showCancelPAYE()

    if (featureFlag.paye.enabled) {
      statusToServiceDashboard(payeConnector.getStatus(regId), enrolments, List("IR-PAYE"))
    } else {
      Future.successful(OtherRegStatus(Statuses.NOT_ENABLED, None, None, None, None))
    }
  }

  private[services] def buildVATDashComponent(regId: String, enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[ServiceDashboard]] = {
    implicit val startURL: String = s"$vatBaseUrl$vatUri"
    implicit val cancelURL: Call = controllers.dashboard.routes.CancelRegistrationController.showCancelVAT()

    if (featureFlag.vat.enabled) {
      statusToServiceDashboard(vatConnector.getStatus(regId), enrolments, List("HMCE-VATDEC-ORG", "HMCE-VATVAR-ORG")).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  private[services] def buildIncorpCTDashComponent(regId: String)(implicit hc: HeaderCarrier): Future[IncorpAndCTDashboard] = {
    companyRegistrationConnector.retrieveCorporationTaxRegistration(regId) flatMap {
      ctReg =>
        (ctReg \ "status").as[String] match {
          case "held" => buildHeld(regId, ctReg)
          case _ => Future.successful(ctReg.as[IncorpAndCTDashboard](IncorpAndCTDashboard.reads(None)))
        }
    }
  }

  private[services] def getCompanyName(regId: String)(implicit hc: HeaderCarrier): Future[String] = {
    for {
      confRefs <- companyRegistrationConnector.fetchConfirmationReferences(regId) map {
        case ConfirmationReferencesSuccessResponse(refs) => refs
        case _ => throw new ConfirmationRefsNotFoundException
      }
      transId = confRefs.transactionId
      companyName <- incorpInfoConnector.getCompanyName(transId)
    } yield companyName
  }

  private[services] def buildHeld(regId: String, ctReg: JsValue)(implicit hc: HeaderCarrier): Future[IncorpAndCTDashboard] = {
    for {
      heldSubmissionDate <- companyRegistrationConnector.fetchHeldSubmissionTime(regId)
      submissionDate = extractSubmissionDate(heldSubmissionDate.get)
    } yield {
      ctReg.as[IncorpAndCTDashboard](IncorpAndCTDashboard.reads(Option(submissionDate)))
    }
  }

  private[services] def extractSubmissionDate(jsonDate: JsValue): String = {

    val dgdt : DateTime = jsonDate.as[DateTime]
    dgdt.toString("d MMMM yyyy")
  }
}
