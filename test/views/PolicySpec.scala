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

package views

import controllers.PolicyController
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import _root_.helpers.SCRSSpec

import scala.concurrent.ExecutionContext.Implicits.global
import views.html.policies


class PolicySpec extends SCRSSpec with GuiceOneAppPerSuite {

  val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  val mockPolicies = app.injector.instanceOf[policies]

  class SetupPage {
    val controller = new PolicyController(
      mockMcc,
      mockPolicies)
    (
      mockAppConfig,
      global)

  }

  "PolicyLinks" should {
    "load the Cookies, privacy and terms page and have the correct URLs" in new SetupPage {
      val result = controller.policyLinks()(FakeRequest())
      val document = Jsoup.parse(contentAsString(result))

      document.title should include("Cookies, privacy and terms")

      document.getElementById("main-heading").text() shouldBe "Cookies, privacy and terms"
      document.getElementById("ch-cookies").attr("href") shouldBe "http://ewf.companieshouse.gov.uk/cookies"
      document.getElementById("hmrc-cookies").attr("href") shouldBe "https://www.tax.service.gov.uk/help/cookies"
      document.getElementById("ch-policy").attr("href") shouldBe "https://www.gov.uk/government/organisations/companies-house/about/personal-information-charter"
      document.getElementById("hmrc-policy").attr("href") shouldBe "https://www.tax.service.gov.uk/help/privacy"
      document.getElementById("ch-terms").attr("href") shouldBe "http://resources.companieshouse.gov.uk/serviceInformation.shtml"
      document.getElementById("hmrc-terms").attr("href") shouldBe "https://www.tax.service.gov.uk/help/terms-and-conditions"
    }
  }
}