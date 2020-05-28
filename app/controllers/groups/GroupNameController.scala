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

package controllers.groups

import config.FrontendAppConfig
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthFunction
import controllers.reg.ControllerErrorHandler
import forms.GroupNameForm
import javax.inject.{Inject, Singleton}
import models.Groups
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{GroupPageEnum, GroupService}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionRegistration
import views.html.groups.GroupNameView

import scala.concurrent.Future

@Singleton
class GroupNameController @Inject()(val authConnector: PlayAuthConnector,
                                    val groupService: GroupService,
                                    val compRegConnector: CompanyRegistrationConnector,
                                    val keystoreConnector: KeystoreConnector,
                                    val messagesApi: MessagesApi
                                   )(implicit val appConfig: FrontendAppConfig)
  extends FrontendController with AuthFunction with ControllerErrorHandler with SessionRegistration with I18nSupport {

  val show: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        checkStatus { regID =>
          groupService.retrieveGroups(regID).flatMap {
            case Some(groups@Groups(true, _, _, _)) =>
              groupService.returnValidShareholdersAndUpdateGroups(groups, regID).map { res =>
              val (listOfShareholders, updatedGroups) = res
              Ok(GroupNameView(
                groups.nameOfCompany.fold(GroupNameForm.form)(gName => GroupNameForm.form.fill(gName)),
                updatedGroups.nameOfCompany,
                listOfShareholders
              ))
            }
            case _ => Future.successful(Redirect(controllers.reg.routes.SignInOutController.postSignIn()))
          }
        }
      }
  }

  val submit: Action[AnyContent] = Action.async { implicit request =>
    ctAuthorised {
      checkStatus { regID =>
        groupService.retrieveGroups(regID).flatMap {
          case Some(groups@Groups(true, _, _, _)) =>
              groupService.returnValidShareholdersAndUpdateGroups(groups, regID).flatMap { res =>
                val (listOfShareholders, updatedGroups) = res
                GroupNameForm.form.bindFromRequest.fold(
                  errors => {
                    Future.successful(BadRequest(GroupNameView(errors, updatedGroups.nameOfCompany, listOfShareholders)))
                  },
                  name => {
                    groupService.updateGroupName(name, updatedGroups, regID).map { _ =>
                      Redirect(controllers.groups.routes.GroupAddressController.show())
                    }
                  }
                )
            }
          case _ =>
            Future.failed(new InternalServerException("[GroupNameController] [submit] Missing prerequisite group data"))
        }
      }
    }
  }
}



