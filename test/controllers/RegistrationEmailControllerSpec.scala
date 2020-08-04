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

package controllers

import builders.AuthBuilder
import config.FrontendAppConfig
import controllers.reg.RegistrationEmailController
import helpers.SCRSSpec
import models.{Email, RegistrationEmailModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.EmailVerificationService
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.BooleanFeatureSwitch

import scala.concurrent.Future


class RegistrationEmailControllerSpec extends SCRSSpec with WithFakeApplication with MockitoSugar with AuthBuilder {

  class Setup {
    val controller = new RegistrationEmailController {
      val authConnector = mockAuthConnector
      val keystoreConnector = mockKeystoreConnector
      implicit val appConfig: FrontendAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]
      override val compRegConnector = mockCompanyRegistrationConnector
      override val emailVerification: EmailVerificationService = mockEmailService
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      def showLogicFun(email: String = "fakeEmail") = showLogic(email)(HeaderCarrier(), FakeRequest())
      def submitLogicFun(
                          regID: String = "regid",
                          email: String = "fakeEmail",
                          authProvId:String ="provId",
                          extId: String = "extID",
                          r: Request[AnyContent]) = submitLogic(email, regID,authProvId,extId)(HeaderCarrier(), r)
    }
    case class funcMatcher(func: () => Future[Result]) extends ArgumentMatcher[() => Future[Result]] {
      override def matches(oarg: scala.Any): Boolean = oarg match {
        case a:(() => Future[Result]) => true
        case _ => false
      }
    }
    val mockOfFunction  = () => Future.successful(Results.Ok(""))
  }

  "show" should {

    val authResult = new ~(
      Name(None, None),
      Some("fakeEmail")
    )

    "return 200, with data in keystore" in new Setup {

      val email = "foo@bar.wibble"
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(),Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      val awaitedFun = await(controller.showLogicFun())
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(),Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))
      showWithAuthorisedUserRetrieval(controller.show, authResult)(
        result => {
          status(result) shouldBe 200
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe "Which email address do you want to use for this application?"
          document.getElementById("registrationEmail-currentemail").attr("checked") shouldBe "checked"
        }
      )
    }
    "return 200, with no data in keystore" in new Setup {
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(),Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", None)
      val awaitedFun = await(controller.showLogicFun())
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(),Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))
      showWithAuthorisedUserRetrieval(controller.show, authResult)(
        result => {
          status(result) shouldBe 200
          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementById("registrationEmail-currentemail").attr("checked") shouldBe ""
        }
      )
    }
    "return an exception when keystore returns an exception" in new Setup {
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      mockKeystoreFetchAndGetFailed[RegistrationEmailModel]("RegEmail", new Exception(""))
      val awaitedFun = intercept[Exception](await(controller.showLogicFun()))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(),Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.failed(awaitedFun))
      intercept[Exception](showWithAuthorisedUserRetrieval(controller.show, authResult)(
        result => {
          await(result)
        }
      ))
    }

  }

  "submit" should {
    val validEmail = Email("foo@bar.com","SCP",false,true,false)

    "return 400 when invalid data used " in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )
      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "a@b.com")

      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))


      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("registrationEmail-currentemail").attr("checked") shouldBe ""
      }

    }

    "return 303 and redirect to CompletionCapacity route when success on currentEmail and sendLink returns true (meaning email verified) and SCP verified is false" in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      mockAuthorisedUser(Future.successful(Some(false)))

      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "currentEmail")

      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.sendVerificationLink(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(true)))

      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.reg.routes.CompletionCapacityController.show().url
      }
    }

    "return 303 and redirect to CompletionCapacity route when success on currentEmail and sendLink returns true (meaning email verified) and SCP verified is true" in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      mockAuthorisedUser(Future.successful(Some(true)))
      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "currentEmail")
      when(mockEmailService.saveEmailBlock(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(validEmail)))
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.sendVerificationLink(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(true)))

      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.reg.routes.CompletionCapacityController.show().url
      }
    }

    "return 303 and redirect to Email Verification show route when success on currentEmail and sendLink returns false meaning email NOT verified and email not verified in SCP " in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      mockAuthorisedUser(Future.successful(Some(false)))
      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "currentEmail")

      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.sendVerificationLink(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(false)))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.verification.routes.EmailVerificationController.verifyShow().url
      }
    }

    "return 303 and redirect to Email Verification show route when success on currentEmail and sendLink returns false meaning email NOT verified and email is verified in SCP " in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      mockAuthorisedUser(Future.successful(Some(true)))
      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "currentEmail")
      when(mockEmailService.saveEmailBlock(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(validEmail)))
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.sendVerificationLink(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(false)))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.reg.routes.CompletionCapacityController.show().url
      }
    }


    "return 303 and redirect to Completion Capacity show route when success on currentEmail and sendLink returns None meaning email NOT verified  but email is verified in SCP " in new Setup {

      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      mockAuthorisedUser(Future.successful(Some(true)))
      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "currentEmail")
      when(mockEmailService.saveEmailBlock(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(validEmail)))
      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      when(mockEmailService.sendVerificationLink(Matchers.any(), Matchers.any(),Matchers.any(),Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.reg.routes.CompletionCapacityController.show().url
      }
    }

    "return 303 and redirect to RegistrationEmailConfirmation route when success on differentEmail" in new Setup {
      val authResult = new ~(
        new ~(
          new ~(
            Name(None,None),
            Some("fakeEmail")
          ), Credentials("provId", "provType")
        ), Some("extID")
      )

      val cm = CacheMap("", Map())

      val req = FakeRequest().withFormUrlEncodedBody("registrationEmail" -> "differentEmail", "DifferentEmail" -> "my@email.com")

      mockKeystoreFetchAndGet[String]("registrationID", Some("regid"))
      mockKeystoreFetchAndGet[RegistrationEmailModel]("RegEmail", Some(RegistrationEmailModel("currentEmail", Some("differentEmail"))))
      mockKeystoreCache[RegistrationEmailModel]("RegEmail", RegistrationEmailModel("currentEmail", Some("differentEmail")), cm)
      val awaitedFun = await(controller.submitLogicFun("regid", r = req))
      when(mockEmailService.emailVerifiedStatusInSCRS(Matchers.any(), Matchers.argThat(funcMatcher(mockOfFunction)))(Matchers.any())).thenReturn(Future.successful(awaitedFun))

      submitWithAuthorisedUserRetrieval(controller.submit, req, authResult) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe controllers.reg.routes.RegistrationEmailConfirmationController.show().url

      }
    }
  }
}