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
import forms.GroupReliefForm
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import services.{GroupReliefService, MetricsService}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionRegistration
import views.html.groups.GroupReliefView

import scala.concurrent.Future

class GroupReliefControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                             val groupReliefService: GroupReliefService,
                                             val metricsService: MetricsService,
                                             val compRegConnector: CompanyRegistrationConnector,
                                             val keystoreConnector: KeystoreConnector,
                                             val appConfig: FrontendAppConfig,
                                             val messagesApi: MessagesApi) extends GroupReliefController

trait GroupReliefController extends FrontendController with AuthFunction with ControllerErrorHandler with SessionRegistration with I18nSupport {
  implicit val appConfig: FrontendAppConfig

  val groupReliefService : GroupReliefService
  val metricsService: MetricsService

  val show = Action.async { implicit request =>
    ctAuthorised {
      checkStatus { regID =>
        for {
          groupRelief <- groupReliefService.retrieveGroupRelief(regID)
        } yield {
          Ok(GroupReliefView(GroupReliefForm.form.fill(groupRelief)))
        }
      }
    }
  }

  val submit = Action.async { implicit request =>
    ctAuthorised {
      registered { a =>
        GroupReliefForm.form.bindFromRequest.fold(
          errors => Future.successful(BadRequest(GroupReliefView(errors))),
          relief => {
//            val context = metricsService.saveTradingDetailsToCRTimer.time()
            groupReliefService.updateGroupRelief(relief).map {
              case GroupReliefSuccessResponse(_) =>
//                context.stop()


                relief.groupRelief match {
                  case "true" =>  Redirect(routes.GroupReliefController.show())
                  case "false" => Redirect(routes.GroupReliefController.show())
                }



//                Redirect(controllers.handoff.routes.BusinessActivitiesController.businessActivities())
              case GroupReliefErrorResponse(_) =>
//                context.stop()
                BadRequest(defaultErrorPage)
              case GroupReliefNotFoundResponse =>
//                context.stop()
                BadRequest(defaultErrorPage)
              case GroupReliefForbiddenResponse =>
//                context.stop()
                BadRequest(defaultErrorPage)
            }
          }
        )
      }
    }
  }
}