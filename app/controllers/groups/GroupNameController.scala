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

package controllers.groups

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthFunction
import controllers.reg.{ControllerErrorHandler, routes}
import forms.{GroupNameForm, GroupReliefForm}
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import services.{GroupReliefService, MetricsService}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionRegistration
import views.html.groups.{GroupNameView, GroupReliefView}

import scala.concurrent.Future

class GroupNameControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                             val groupReliefService: GroupReliefService,
                                             val metricsService: MetricsService,
                                             val compRegConnector: CompanyRegistrationConnector,
                                             val keystoreConnector: KeystoreConnector,
                                             val appConfig: FrontendAppConfig,
                                             val messagesApi: MessagesApi) extends GroupNameController

trait GroupNameController extends FrontendController with AuthFunction with ControllerErrorHandler with SessionRegistration with I18nSupport {
  implicit val appConfig: FrontendAppConfig

//  val groupNameService : GroupNameService
  val metricsService: MetricsService

  val show = Action.async { implicit request =>
    ctAuthorised {
      checkStatus { regID =>
        for {
          shareHolderNames <- Future.successful(Shareholders(List("Company1","Company2","Company3"), None))
          companyName    <- compRegConnector.fetchCompanyName(regID)
        } yield {
          Ok(GroupNameView(GroupNameForm.form.fill(shareHolderNames)))
        }
      }
    }
  }

//  val submit = Action.async { implicit request =>
//    ctAuthorised {
//      registered { a =>
//        GroupReliefForm.form.bindFromRequest.fold(
//          errors =>
//          compRegConnector.fetchCompanyName(a).map(cName => BadRequest(GroupReliefView(errors, cName))),
//          relief => {
//            groupReliefService.updateGroupRelief(relief).map {
//              case GroupReliefSuccessResponse(_) =>
//                relief.groupRelief match {
//                  case "true" =>  Redirect(routes.GroupReliefController.show())
//                  case "false" => Redirect(routes.GroupReliefController.show())
//                }
//              case GroupReliefErrorResponse(_) =>
//                BadRequest(defaultErrorPage)
//              case GroupReliefNotFoundResponse =>
//                BadRequest(defaultErrorPage)
//              case GroupReliefForbiddenResponse =>
//                BadRequest(defaultErrorPage)
//            }
//          }
//        )
//      }
//    }
//  }
}