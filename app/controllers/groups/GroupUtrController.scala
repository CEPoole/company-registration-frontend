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
import controllers.reg.ControllerErrorHandler
import forms.GroupUtrForm

import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CommonService, GroupUtrService}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{SCRSExceptions,SessionRegistration}
import views.html.groups.GroupUtrView



class GroupUtrControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                                val keystoreConnector: KeystoreConnector,
                                                val groupUtrService: GroupUtrService,
                                                val appConfig: FrontendAppConfig,
                                                val compRegConnector: CompanyRegistrationConnector,
                                                val messagesApi: MessagesApi) extends GroupUtrController


trait GroupUtrController extends FrontendController with AuthFunction with CommonService with ControllerErrorHandler with SCRSExceptions with I18nSupport with SessionRegistration {


  implicit val appConfig: FrontendAppConfig
  val keystoreConnector: KeystoreConnector
  val groupUtrService: GroupUtrService


  val show: Action[AnyContent] = Action.async { implicit request =>
    ctAuthorised {
        checkStatus { regID =>
          for {
            (optUTR,groupParentCompanyName)  <- groupUtrService.retrieveOwningCompanyDetails(regID)
              } yield {
            val formVal = optUTR.fold(GroupUtrForm.form)(utr => GroupUtrForm.form.fill(utr))
            Ok(GroupUtrView(formVal,groupParentCompanyName))
          }

        }
    }
  }


  val submit = Action.async { implicit request =>
    ctAuthorised {
      registered { regID =>
        GroupUtrForm.form.bindFromRequest.fold(
          errors =>
            groupUtrService.retrieveOwningCompanyDetails(regID).map(res => BadRequest(GroupUtrView(errors,res._2))),
          relief => {
            groupUtrService.updateGroupUtr(relief).map {
              case GroupUtrSuccessResponse(_) =>
                Redirect(controllers.groups.routes.GroupUtrController.show())
              case GroupUtrErrorResponse(_) =>
                BadRequest(defaultErrorPage)
              case GroupUtrNotFoundResponse =>
                BadRequest(defaultErrorPage)
              case GroupUtrForbiddenResponse =>
                BadRequest(defaultErrorPage)
            }
          }
        )
      }
    }
  }
}