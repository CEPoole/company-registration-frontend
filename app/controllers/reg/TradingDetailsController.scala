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

package controllers.reg

import config.FrontendAuthConnector
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.SCRSRegime
import forms.TradingDetailsForm
import models.{TradingDetailsErrorResponse, TradingDetailsForbiddenResponse, TradingDetailsNotFoundResponse, TradingDetailsSuccessResponse}
import services.{MetricsService, TradingDetailsService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.reg.TradingDetailsView
import utils.{MessagesSupport, SessionRegistration}

import scala.concurrent.Future

object TradingDetailsController extends TradingDetailsController {
  val authConnector = FrontendAuthConnector
  val tradingDetailsService = TradingDetailsService
  override val metricsService: MetricsService = MetricsService
  val companyRegistrationConnector = CompanyRegistrationConnector
  val keystoreConnector = KeystoreConnector
}

trait TradingDetailsController extends FrontendController with Actions with ControllerErrorHandler with SessionRegistration with MessagesSupport {

  val tradingDetailsService : TradingDetailsService
  val metricsService: MetricsService

  val show = AuthorisedFor(taxRegime = SCRSRegime("trading-details"), pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        checkStatus { regID =>
          for {
            tradingDetails <- tradingDetailsService.retrieveTradingDetails(regID)
          } yield {
            Ok(TradingDetailsView(TradingDetailsForm.form.fill(tradingDetails)))
          }
        }
  }

  val submit = AuthorisedFor(taxRegime = SCRSRegime("trading-details"), pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        registered{a =>
        TradingDetailsForm.form.bindFromRequest.fold(
          errors => Future.successful(BadRequest(TradingDetailsView(errors))),
          payments => {
            val context = metricsService.saveTradingDetailsToCRTimer.time()
            tradingDetailsService.updateCompanyInformation(payments).map {
              case TradingDetailsSuccessResponse(_) =>
                context.stop()
                Redirect(controllers.handoff.routes.BusinessActivitiesController.businessActivities())
              case TradingDetailsErrorResponse(_) =>
                context.stop()
                BadRequest(defaultErrorPage)
              case TradingDetailsNotFoundResponse =>
                context.stop()
                BadRequest(defaultErrorPage)
              case TradingDetailsForbiddenResponse =>
                context.stop()
                BadRequest(defaultErrorPage)
            }
          }
        )
  }
}
}
