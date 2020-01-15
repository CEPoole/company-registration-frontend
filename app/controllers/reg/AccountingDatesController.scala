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
import forms.AccountingDatesForm
import models.{AccountingDatesModel, AccountingDetailsNotFoundResponse, AccountingDetailsSuccessResponse}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import services.{AccountingService, MetricsService, TimeService}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{SessionRegistration, SystemDate}
import views.html.reg.AccountingDates

import scala.concurrent.Future

class AccountingDatesControllerImpl @Inject()(val authConnector: PlayAuthConnector,
                                              val compRegConnector: CompanyRegistrationConnector,
                                              val keystoreConnector: KeystoreConnector,
                                              val appConfig: FrontendAppConfig,
                                              val accountingService: AccountingService,
                                              val metricsService: MetricsService,
                                              val timeService: TimeService,
                                              val messagesApi: MessagesApi) extends AccountingDatesController

trait AccountingDatesController extends FrontendController with AuthFunction with ControllerErrorHandler with SessionRegistration with I18nSupport {

  val accountingService : AccountingService
  val metricsService: MetricsService
  val timeService: TimeService
  lazy val accDForm = new AccountingDatesForm(timeService)

  implicit val appConfig: FrontendAppConfig
  implicit lazy val bHS = timeService.bHS

  val show = Action.async { implicit request =>
    ctAuthorised {
      checkStatus { _ =>
        accountingService.fetchAccountingDetails.map {
          accountingDetails => {
            Ok(AccountingDates(accDForm.form.fill(accountingDetails), timeService.futureWorkingDate(SystemDate.getSystemDate, 60)))
          }
        }
      }
    }
  }

  val submit = Action.async { implicit request =>
    ctAuthorised {
      accDForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(AccountingDates(formWithErrors, timeService.futureWorkingDate(SystemDate.getSystemDate, 60))))
        }, {
          val context = metricsService.saveAccountingDatesToCRTimer.time()
          data => {
            val updatedData = data.crnDate match {
              case "whenRegistered"   => data.copy(crnDate = AccountingDatesModel.WHEN_REGISTERED, day = None, month = None, year = None)
              case "futureDate"       => data.copy(crnDate = AccountingDatesModel.FUTURE_DATE)
              case "notPlanningToYet" => data.copy(crnDate = AccountingDatesModel.NOT_PLANNING_TO_YET, day = None, month = None, year = None)
            }
            accountingService.updateAccountingDetails(updatedData) map {
              case AccountingDetailsSuccessResponse(_) =>
                context.stop()
                Redirect(routes.TradingDetailsController.show())
              case AccountingDetailsNotFoundResponse =>
                context.stop()
                NotFound(defaultErrorPage)
              case _ =>
                context.stop()
                BadRequest(defaultErrorPage)
            }
          }
        }
      )
    }
  }
}