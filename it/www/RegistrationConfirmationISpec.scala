/*
 * Copyright 2017 HM Revenue & Customs
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

package www

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import itutil.{IntegrationSpecBase, LoginStub}
import models.RegistrationConfirmationPayload
import models.handoff.{HandOffNavModel, NavLinks, Receiver, Sender}
import play.api.http.HeaderNames
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.NavModelRepo
import utils.JweCommon

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationConfirmationISpec extends IntegrationSpecBase with LoginStub {

  private def client(path: String) = ws.url(s"http://localhost:$port/register-your-company$path").withFollowRedirects(false)

  val userId = "/bar/foo"
  val regId = "regId5"
  lazy val defaultCookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  class Setup {
    val rc = app.injector.instanceOf[ReactiveMongoComponent]
    val repo = new NavModelRepo {
      override val mongo: ReactiveMongoComponent = rc
      override val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
    }
    await(repo.repository.ensureIndexes)
  }

  def confirmationEncryptedRequest(encrypted: String) = s"/registration-confirmation?request=$encrypted"

  val forwardPayloadString =
    s"""
       |{
       |  "user_id" : "Ext-xxx",
       |  "journey_id" : "$regId",
       |  "ct_reference" : "TEST-ACKREF",
       |  "hmrc" : {},
       |  "ch" : {},
       |  "links" : {
       |    "forward" : "link-to-confirmation-on-ct"
       |  }
       |}
      """.stripMargin

  val forwardPayloadWithChString =
    s"""
       |{
       |  "user_id" : "Ext-xxx",
       |  "journey_id" : "$regId",
       |  "ct_reference" : "TEST-ACKREF",
       |  "hmrc" : {},
       |  "ch" : {
       |    "data" : "test-data"
       |  },
       |  "links" : {
       |    "forward" : "link-to-confirmation-on-ct"
       |  }
       |}
      """.stripMargin

  val forwardPayloadJson = Json.parse(forwardPayloadString).as[JsObject]
  val forwardPayloadWithChJson = Json.parse(forwardPayloadWithChString).as[JsObject]

  val handOffNavModel = HandOffNavModel(
    Sender(
      Map(
        "1" -> NavLinks(
          "testForwardLinkFromSender17373737373",
          "testReverseLinkFromSender1"
        ),
        "3" -> NavLinks(
          "testForwardLinkFromSender3",
          "testReverseLinkFromSender3"
        ),
        "5-2" -> NavLinks(
          "link-to-confirmation-on-ct",
          ""
        )
      )
    ),
    Receiver(
      Map(
        "0" -> NavLinks(
          "testForwardLinkFromReceiver0",
          "testReverseLinkFromReceiver0"
        ),
        "2" -> NavLinks(
          "testForwardLinkFromReceiver2",
          "testReverseLinkFromReceiver2"
        )
      ),
      Map("testJumpKey" -> "testJumpLink"),
      Some(Json.parse("""{"testCHBagKey": "testValue"}""").as[JsObject])
    )
  )

  def stubKeystore(session: String, regId: String): StubMapping = {
    val keystoreUrl = s"/keystore/company-registration-frontend/${session}"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |"id": "${session}",
               |"data": {
               |    "registrationID": "${regId}" }
               |}""".stripMargin
          )
      )
    )
  }

  "HO5-1" should {

    val transID = "1551552"
    lazy val encryptedForwardPayload = app.injector.instanceOf[JweCommon].encrypt(RegistrationConfirmationPayload(
      userId,
      "journeyid",
      transID,
      None,
      None,
      Json.obj(),
      Json.obj(),
      Json.obj("forward" -> "/link-to-before-you-pay-coho")
    ))

    "redirect to the forward url if there is a 200 on submission" in new Setup {
      stubSuccessfulLogin(userId = userId)
      stubAuthorisation(200, Some(
        """
          |{
          | "externalId" : "Ext-xxx"
          |}
        """.stripMargin))

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))

      val crResponse =
        s"""
           |{
           |"acknowledgement-reference" : "TEST-ACKREF",
           |"transaction-id" : "$transID"
           |}""".stripMargin
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)
      stubGet(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)

      val fResponse = client(confirmationEncryptedRequest(encryptedForwardPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      val encryptedHandOffString = response.header(HeaderNames.LOCATION).get.split("request=").takeRight(1)(0)
      val decryptedHandoffJson = app.injector.instanceOf[JweCommon].decrypt[JsObject](encryptedHandOffString).get


      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/link-to-before-you-pay-coho")
      decryptedHandoffJson shouldBe forwardPayloadJson
    }

    "redirect with the same ch data that was recieved" in new Setup {
      stubSuccessfulLogin(userId = userId)
      stubAuthorisation(200, Some(
        """
          |{
          | "externalId" : "Ext-xxx"
          |}
        """.stripMargin))

      val encryptedForwardWithChPayload = app.injector.instanceOf[JweCommon].encrypt(RegistrationConfirmationPayload(
        userId,
        "journeyid",
        transID,
        None,
        None,
        Json.obj("data" -> "test-data"),
        Json.obj(),
        Json.obj("forward" -> "/link-to-before-you-pay-coho")
      ))


      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))

      val crResponse =
        s"""
           |{
           |"acknowledgement-reference" : "TEST-ACKREF",
           |"transaction-id" : "$transID"
           |}""".stripMargin
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)
      stubGet(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)

      val fResponse = client(confirmationEncryptedRequest(encryptedForwardWithChPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      val encryptedHandOffString = response.header(HeaderNames.LOCATION).get.split("request=").takeRight(1)(0)
      val decryptedHandoffJson = app.injector.instanceOf[JweCommon].decrypt[JsObject](encryptedHandOffString).get

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/link-to-before-you-pay-coho")
      decryptedHandoffJson shouldBe forwardPayloadWithChJson
    }

    "redirect to the forward url if there is a 502 on submission" in new Setup {
      stubSuccessfulLogin(userId = userId)
      stubAuthorisation(200, Some(
        """
          |{
          | "externalId" : "Ext-xxx"
          |}
        """.stripMargin))

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))

      val crResponse =
        s"""
           |{
           |"acknowledgement-reference" : "TEST-ACKREF",
           |"transaction-id" : "$transID"
           |}""".stripMargin
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 502, crResponse)
      stubGet(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)

      val fResponse = client(confirmationEncryptedRequest(encryptedForwardPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      val encryptedHandOffString = response.header(HeaderNames.LOCATION).get.split("request=").takeRight(1)(0)
      val decryptedHandoffJson = app.injector.instanceOf[JweCommon].decrypt[JsObject](encryptedHandOffString).get

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/link-to-before-you-pay-coho")
      decryptedHandoffJson shouldBe forwardPayloadJson
    }

    "redirect to the forward url if there is 403 on submission" in new Setup {
      stubSuccessfulLogin(userId = userId)
      stubAuthorisation(200, Some(
        """
          |{
          | "externalId" : "Ext-xxx"
          |}
        """.stripMargin))

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))

      val crResponse =
        s"""
           |{
           |"acknowledgement-reference" : "TEST-ACKREF",
           |"transaction-id" : "$transID"
           |}""".stripMargin
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 403, crResponse)
      stubGet(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)

      val fResponse = client(confirmationEncryptedRequest(encryptedForwardPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      val encryptedHandOffString = response.header(HeaderNames.LOCATION).get.split("request=").takeRight(1)(0)
      val decryptedHandoffJson = app.injector.instanceOf[JweCommon].decrypt[JsObject](encryptedHandOffString).get

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/link-to-before-you-pay-coho")
      decryptedHandoffJson shouldBe forwardPayloadJson
    }
  }

  "HO6" should {

    val transID = "1551551"
    val paymentRef = "TEST-PAYMENTREF"
    val paymentAmount = "12"

    lazy val encryptedPayload = app.injector.instanceOf[JweCommon].encrypt(RegistrationConfirmationPayload(
      userId,
      "journeyid",
      transID,
      Some(paymentRef),
      Some(paymentAmount),
      Json.obj(),
      Json.obj(),
      Json.obj()
    ))

    "Return a redirect to a new page when not authenticated" in new Setup {
      stubAuthorisation(401, None)

      val response = await(client("/registration-confirmation?request=xxx").get())

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/sign-in-complete-application")
    }

    "updating confirmation references successfully should return the application submitted page" in new Setup {
      stubAuthorisation()
      stubSuccessfulLogin(userId = userId)

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))

      val crResponse =
        s"""
           |{
           |"acknowledgement-reference" : "TEST-ACKREF",
           |"payment-reference" : "$paymentRef",
           |"payment-amount": "$paymentAmount",
           |"transaction-id" : "$transID"
           |}""".stripMargin
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 200, crResponse)

      val fResponse = client(confirmationEncryptedRequest(encryptedPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/register-your-company/application-submitted")
    }

    "updating confirmation references with 502 should return a retry page" in new Setup {

      stubAuthorisation()
      stubSuccessfulLogin(userId = userId)

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 502, "")

      val fResponse = client(confirmationEncryptedRequest(encryptedPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/register-your-company/application-submitted")
    }

    "updating confirmation references with 403 should return a deskpro page" in new Setup {

      stubAuthorisation()
      stubSuccessfulLogin(userId = userId)

      val csrfToken = UUID.randomUUID().toString
      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken), userId)

      stubKeystore(SessionId, regId)
      await(repo.repository.insertNavModel(regId, handOffNavModel))
      stubPut(s"/company-registration/corporation-tax-registration/$regId/confirmation-references", 403, "")

      val fResponse = client(confirmationEncryptedRequest(encryptedPayload.get)).
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        get()

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION).get should include("/register-your-company/something-went-wrong")
    }
  }
}