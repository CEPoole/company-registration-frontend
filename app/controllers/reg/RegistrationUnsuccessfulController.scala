/*
 * Copyright 2022 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthenticatedController
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeleteSubmissionService
import uk.gov.hmrc.auth.core.PlayAuthConnector
import utils.SessionRegistration
import views.html.reg.{RegistrationUnsuccessful => RegistrationUnsuccessfulView}
import views.html.errors.{incorporationRejected => IncorporationRejectedView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationUnsuccessfulController @Inject()(val authConnector: PlayAuthConnector,
                                                   val keystoreConnector: KeystoreConnector,
                                                   val compRegConnector: CompanyRegistrationConnector,
                                                   val deleteSubService: DeleteSubmissionService,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   viewRegistrationUnsuccessful: RegistrationUnsuccessfulView,
                                                   viewincorporationRejectedView: IncorporationRejectedView)
                                                  (implicit val appConfig: FrontendAppConfig, implicit val ec: ExecutionContext) extends AuthenticatedController with SessionRegistration with I18nSupport {

  lazy val registerCompanyGOVUKLink = appConfig.servicesConfig.getConfString("gov-uk.register-your-company", throw new Exception("Could not find config for key: gov-uk.register-your-company"))

  def show = Action.async { implicit request =>
    ctAuthorised {
      Future.successful(Ok(viewRegistrationUnsuccessful()))
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
        Future.successful(Ok(viewincorporationRejectedView()))
      }
  }

  def rejectionSubmit: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        registered { regId =>
          deleteSubService.deleteSubmission(regId) flatMap {
            if (_) {
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
