/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.handoff

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthenticatedController
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{HandBackService, HandOffService, NavModelNotFoundException}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.SessionKeys
import utils.{DecryptionError, PayloadError, SessionRegistration}
import views.html.error_template_restart

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CorporationTaxDetailsControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                                    val keystoreConnector: KeystoreConnector,
                                                    val handOffService: HandOffService,
                                                    val appConfig: FrontendAppConfig,
                                                    val compRegConnector: CompanyRegistrationConnector,
                                                    val handBackService: HandBackService,
                                                    val ec: ExecutionContext,
                                                    val controllerComponents: MessagesControllerComponents) extends CorporationTaxDetailsController

trait CorporationTaxDetailsController extends AuthenticatedController with SessionRegistration with I18nSupport {
  val handOffService: HandOffService

  val handBackService: HandBackService
  implicit val appConfig: FrontendAppConfig

  //HO2
  def corporationTaxDetails(requestData: String): Action[AnyContent] = Action.async {
    implicit _request => {
      val optHcAuth = hc.authorization
      val optSessionAuthToken = _request.session.get(SessionKeys.authToken)
      Logger.warn(s"[CorporationTaxDetailsController][HO2] mdtp cookie present? ${_request.cookies.get("mdtp").isDefined}")
      (optHcAuth, optSessionAuthToken) match {
        case (Some(hcAuth),Some(sAuth)) => if (hcAuth.value == sAuth) {Logger.warn("[CorporationTaxDetailsController][HO2] hcAuth and session auth present and equal")}
        else {Logger.warn("[CorporationTaxDetailsController][HO2] hcAuth and session auth present but not equal")}
        case (Some(hcAuth),None) => Logger.warn("[CorporationTaxDetailsController][HO2] hcAuth present, session auth not")
        case (None,Some(sAuth)) => Logger.warn("[CorporationTaxDetailsController][HO2] session auth present, hcAuth auth not")
        case (None, None) => Logger.warn("[CorporationTaxDetailsController][HO2] neither session auth or hcAuth present")
      }
      ctAuthorisedHandoff("HO2", requestData) {
        registeredHandOff("HO2", requestData) { _ =>
          handBackService.processCompanyDetailsHandBack(requestData).map {
            case Success(_) => Redirect(controllers.reg.routes.PPOBController.show())
            case Failure(PayloadError) => BadRequest(error_template_restart("2", "PayloadError"))
            case Failure(DecryptionError) => BadRequest(error_template_restart("2", "DecryptionError"))
            case unknown => {
              Logger.warn(s"[CorporationTaxDetailsController][corporationTaxDetails] HO2 Unexpected result, sending to post-sign-in : ${unknown}")
              Redirect(controllers.reg.routes.SignInOutController.postSignIn(None))
            }
          } recover {
            case ex: NavModelNotFoundException => Redirect(controllers.reg.routes.SignInOutController.postSignIn(None))
          }
        }
      }
  }}
}