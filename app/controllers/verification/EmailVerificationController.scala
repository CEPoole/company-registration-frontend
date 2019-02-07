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

package controllers.verification

import config.{AppConfig, FrontendAppConfig, FrontendAuthConnector, FrontendConfig}
import connectors.{CompanyRegistrationConnector, KeystoreConnector}
import controllers.auth.AuthFunction
import play.api.mvc.{Action, AnyContent, Result}
import services.EmailVerificationService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{MessagesSupport, SessionRegistration}
import views.html.verification.{CreateGGWAccount, CreateNewGGWAccount, createNewAccount, verifyYourEmail}

import scala.concurrent.Future

object EmailVerificationController extends EmailVerificationController with ServicesConfig {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = KeystoreConnector
  val createGGWAccountUrl = getConfString("gg-reg-fe.url", throw new Exception("Could not find config for gg-reg-fe url"))
  val callbackUrl = getConfString("auth.login-callback.url", throw new Exception("Could not find config for callback url"))
  val frontEndUrl=FrontendConfig.self
  val emailVerificationService = EmailVerificationService
  val companyRegistrationConnector = CompanyRegistrationConnector
  override val appConfig: AppConfig  = FrontendAppConfig
}

trait EmailVerificationController extends FrontendController with AuthFunction with SessionRegistration with MessagesSupport {

  val keystoreConnector : KeystoreConnector
  val createGGWAccountUrl: String
  val callbackUrl: String
  val frontEndUrl : String
  val emailVerificationService: EmailVerificationService

  implicit val appConfig: AppConfig  = FrontendAppConfig

  val verifyShow: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        keystoreConnector.fetchAndGet[String]("registrationID").flatMap{
          _.fold(
            Future.successful(Redirect(controllers.reg.routes.SignInOutController.postSignIn(None)))){rId =>
            val emailBlock = emailVerificationService.fetchEmailBlock(rId)

            emailBlock.map(_.fold(
              Redirect(controllers.reg.routes.SignInOutController.postSignIn(None)))
            (email => Ok(verifyYourEmail(email.address)))

            )

          }
        }
      }

  }

  val verifySubmit: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok)
  }

  val resendVerificationLink: Action[AnyContent] = Action.async {
    implicit request =>
      ctAuthorised {
        keystoreConnector.fetchAndGet[String]("registrationID").flatMap {
          _.fold(
            Future.successful(Redirect(controllers.reg.routes.SignInOutController.postSignIn(None)))) { rId =>
            val emailBlock = emailVerificationService.fetchEmailBlock(rId)

            emailBlock.flatMap(
              emailBlockv => emailBlockv.fold[Future[Result]](
              Future.successful(Redirect(controllers.reg.routes.SignInOutController.postSignIn(None))))
            (email =>
              emailVerificationService.sendVerificationLink(email.address, rId).map {_  => Redirect(controllers.verification.routes.EmailVerificationController.verifyShow())}))
          }
        }
      }
  }


  val createShow: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(createNewAccount()))
  }

  val createSubmit: Action[AnyContent] = Action.async { implicit request =>
    //TODO change this to create account - need to remove auth context from session / logout
    Future.successful(Redirect(controllers.reg.routes.ReturningUserController.show()))
  }

  val createGGWAccountAffinityShow: Action[AnyContent] = Action.async {
    implicit request =>
      val redirect = controllers.reg.routes.ReturningUserController.show().url
      val url = controllers.reg.routes.SignInOutController.signOut(Some(ContinueUrl(s"$frontEndUrl$redirect"))).url
      Future.successful(Ok(CreateGGWAccount(url)))

  }

  val createGGWAccountSubmit: Action[AnyContent] = Action.async { implicit request =>
    //TODO change this to create account - need to remove auth context from session / logout
    Future.successful(Redirect(controllers.reg.routes.SignInOutController.signOut(None)).withNewSession)
  }

  val createNewGGWAccountShow: Action[AnyContent] = Action.async {
    implicit request =>
      val redirect = controllers.reg.routes.ReturningUserController.show().url
      val url = controllers.reg.routes.SignInOutController.signOut(Some(ContinueUrl(s"$frontEndUrl$redirect"))).url
      Future.successful(Ok(CreateNewGGWAccount(url)))
  }

  val startAgain: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Redirect(controllers.reg.routes.ReturningUserController.show()).withNewSession)
  }
}
