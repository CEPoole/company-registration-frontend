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

import com.github.tomakehurst.wiremock.client.WireMock._
import itutil.{FakeAppConfig, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.test.FakeApplication

import scala.concurrent.duration.FiniteDuration

class VerifyYourEmailISpec extends IntegrationSpecBase with LoginStub with BeforeAndAfterEach with FakeAppConfig {


  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  val userId = "/wibble"

  def stubKeystore(session: String, regId: String, email: String) = {
    val keystoreUrl = s"/keystore/company-registration-frontend/${session}"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
                |"id": "${session}",
                |"data": {
                |  "registrationID": "${regId}",
                |  "email": "${email}"
                |}
                |}""".stripMargin
          )
        )
      )
  }

  "GET /ryc/verify-your-email" should {
    "Show a page with the email when logged in" in {
      stubAuthorisation()

      val email = "foo@bar.wibble"
      stubKeystore(SessionId, "5",  email)

      val fResponse = buildClient("/sent-an-email").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie(userId=userId)).
        get()

      val response = await(fResponse)(FiniteDuration(10, "seconds"))

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Confirm your email address"
      document.getElementById("description").text should include(email)
    }

    "redirect to sign-in when not logged in" in {
      stubAuthorisation(401, None)

      val response = await(buildClient("/sent-an-email").get())

      response.status shouldBe 303

      val redirectTo = response.header(HeaderNames.LOCATION)

      redirectTo shouldBe defined
      redirectTo map { r =>
        r should include("/gg/sign-in")
        r should include("register-your-company%2Fpost-sign-in")
      }
    }
  }
}