/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.reg

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthFunction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DeleteSubmissionService
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionRegistration
import views.html.reg.RegistrationUnsuccessful

import scala.concurrent.Future

class RegistrationUnsuccessfulControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                                       val keystoreConnector: KeystoreConnector,
                                                       val compRegConnector: CompanyRegistrationConnector,
                                                       val appConfig: FrontendAppConfig,
                                                       val deleteSubService: DeleteSubmissionService,
                                                       val messagesApi: MessagesApi) extends RegistrationUnsuccessfulController {
  lazy val registerCompanyGOVUKLink  = appConfig.getConfString("gov-uk.register-your-company", throw new Exception("Could not find config for key: gov-uk.register-your-company"))
}

trait RegistrationUnsuccessfulController extends FrontendController with AuthFunction with SessionRegistration with I18nSupport {

  implicit val appConfig: FrontendAppConfig

  val deleteSubService: DeleteSubmissionService
  val registerCompanyGOVUKLink: String

  def show = Action.async { implicit request =>
    ctAuthorised {
      Future.successful(Ok(RegistrationUnsuccessful()))
    }
  }

  def submit = Action.async { implicit request =>
    ctAuthorised {
      registered { regId =>
        deleteSubService.deleteSubmission(regId) flatMap {
          case true => keystoreConnector.remove() map {
            _ => Redirect(controllers.reg.routes.SignInOutController.postSignIn(None))
          }
          case false => Future.successful(InternalServerError)
        }
      }
    }
  }

  def rejectionShow: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        Future.successful(Ok(views.html.errors.incorporationRejected()))
      }
  }

  def rejectionSubmit: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        registered { regId =>
          deleteSubService.deleteSubmission(regId) flatMap {
            if(_) {
              keystoreConnector.remove() map {
                _ => Redirect(registerCompanyGOVUKLink)
              }
            }
            else {
              Future.successful(InternalServerError)
            }
          }
        }
      }
  }
}
