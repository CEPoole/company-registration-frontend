/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthFunction
import models.handoff.GroupHandBackModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{HandBackService, HandOffService, NavModelNotFoundException}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionRegistration
import views.html.error_template_restart

import scala.util.Success

class GroupControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                    val keystoreConnector: KeystoreConnector,
                                    val handOffService: HandOffService,
                                    val appConfig: FrontendAppConfig,
                                    val compRegConnector: CompanyRegistrationConnector,
                                    val handBackService: HandBackService,
                                    val messagesApi: MessagesApi) extends GroupController

trait GroupController extends FrontendController with AuthFunction with I18nSupport with SessionRegistration {

  val handBackService: HandBackService
  val handOffService: HandOffService
  implicit val appConfig: FrontendAppConfig

  // 3.1 handback
  def groupHandBack(requestData: String): Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorisedHandoff("HO3-1", requestData) {
        registeredHandOff("HO3-1", requestData) { _ =>
          handBackService.processGroupsHandBack(requestData).map {
            case Success(GroupHandBackModel(_, _, _, _, _, Some(_))) => Redirect(controllers.handoff.routes.GroupController.PSCGroupHandOff)
            case Success(_) => Redirect(controllers.handoff.routes.CorporationTaxSummaryController.corporationTaxSummary(requestData))
            case _ => BadRequest(error_template_restart("3-1", "PayloadError"))
          }
        }
      }
  }

  // 3.2 hand off
  val PSCGroupHandOff: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorisedOptStr(Retrievals.externalId) { externalID =>
        registered { regId =>
          handOffService.buildPSCPayload(regId, externalID) map {
            case Some((url, payload)) => Redirect(handOffService.buildHandOffUrl(url, payload))
            case None => BadRequest(error_template_restart("3-2", "EncryptionError"))
          } recover {
            case ex: NavModelNotFoundException => Redirect(controllers.reg.routes.SignInOutController.postSignIn(None))
          }
        }
      }
  }
}