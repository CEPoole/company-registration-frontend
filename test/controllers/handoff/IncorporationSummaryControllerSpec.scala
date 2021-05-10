/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.handoff

import builders.AuthBuilder
import config.FrontendAppConfig
import fixtures.PayloadFixture
import helpers.SCRSSpec
import models.SummaryHandOff
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{DecryptionError, JweCommon, PayloadError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class IncorporationSummaryControllerSpec extends SCRSSpec with PayloadFixture with GuiceOneAppPerSuite with AuthBuilder {

  class Setup {

    val TestController = new IncorporationSummaryController {
      override val controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
      val authConnector = mockAuthConnector
      val keystoreConnector = mockKeystoreConnector
      val handOffService = mockHandOffService
      val handBackService = mockHandBackService
      override val compRegConnector = mockCompanyRegistrationConnector
      implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
      override val messagesApi = app.injector.instanceOf[MessagesApi]
      implicit val ec = global
    }
    val jweInstance = () => app.injector.instanceOf[JweCommon]
  }

  val extID = Some("extID")

  "HMRC CT Summary hand off " should {
    "send the correct Json" in new Setup {
      Json.toJson(summaryHandOffModelPayload).toString() shouldBe summaryHandOffJson
    }

    "return a 303 if user details are retrieved" in new Setup {
      val payload = summaryEncryptedPayload(jweInstance())
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      when(mockHandOffService.summaryHandOff(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(("testLink", payload))))

      when(mockHandOffService.buildHandOffUrl(ArgumentMatchers.eq("testLink"), ArgumentMatchers.eq(payload)))
        .thenReturn(s"testLink?request=$payload")

      showWithAuthorisedUserRetrieval(TestController.incorporationSummary, extID) {
        result =>
          status(result) shouldBe SEE_OTHER

          val redirect = redirectLocation(result).get

          jweInstance().decrypt[SummaryHandOff](redirect.split("request=").apply(1)).get.journey_id shouldBe "testJourneyID"
      }
    }

    "return a 400 if no link or payload are returned" in new Setup {
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      when(mockHandOffService.summaryHandOff(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      showWithAuthorisedUserRetrieval(TestController.incorporationSummary, extID) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "returnToCorporationTaxSummary" should {
    val payload = Json.obj(
      "user_id" -> Json.toJson("testUserID"),
      "journey_id" -> Json.toJson("testJourneyID"),
      "hmrc" -> Json.obj(),
      "ch" -> Json.obj(),
      "links" -> Json.obj()
    )

    "return a 303 when hand back service decrypts the reverse hand off payload successfully" in new Setup {
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      val encryptedPayload = jweInstance().encrypt[JsValue](payload).get

      when(mockHandBackService.processCompanyNameReverseHandBack(ArgumentMatchers.eq(encryptedPayload))(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(Success(payload)))

      showWithAuthorisedUser(TestController.returnToCorporationTaxSummary(encryptedPayload)) {
        result =>
          status(result) shouldBe SEE_OTHER
      }
    }

    "return a 400 when hand back service errors while decrypting the payload" in new Setup {
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      val encryptedPayload = jweInstance().encrypt[JsValue](payload).get

      when(mockHandBackService.processCompanyNameReverseHandBack(ArgumentMatchers.eq(encryptedPayload))(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(Failure(DecryptionError)))

      showWithAuthorisedUser(TestController.returnToCorporationTaxSummary(encryptedPayload)) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return a 400 when hand back service decrypts the payload successfully but the Json is malformed" in new Setup {
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      val encryptedPayload = jweInstance().encrypt[JsValue](payload).get

      when(mockHandBackService.processCompanyNameReverseHandBack(ArgumentMatchers.eq(encryptedPayload))(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(Failure(PayloadError)))

      showWithAuthorisedUser(TestController.returnToCorporationTaxSummary(encryptedPayload)) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
    "return a 303 when request is made with auth but keystore does not exist" in new Setup {
      mockKeystoreFetchAndGet("registrationID", None)
      val encryptedPayload = jweInstance().encrypt[JsValue](payload).get

      when(mockHandBackService.processCompanyNameReverseHandBack(ArgumentMatchers.eq(encryptedPayload))(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(Success(payload)))

      showWithAuthorisedUser(TestController.returnToCorporationTaxSummary(encryptedPayload)) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"/register-your-company/post-sign-in?handOffID=HO5b&payload=$encryptedPayload")
      }
    }
  }
}
