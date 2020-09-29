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

package views

import controllers.PolicyController
import mocks.SCRSMocks
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

class PolicySpec extends UnitSpec with GuiceOneAppPerSuite with SCRSMocks with MockitoSugar {

	val mockMcc = app.injector.instanceOf[MessagesControllerComponents]

	class SetupPage {
		val controller = new PolicyController(mockMcc){
			override val appConfig = mockAppConfig
			override val messagesApi = app.injector.instanceOf[MessagesApi]
		}
    when(mockAppConfig.piwikURL).thenReturn(None)
	}
	"PolicyLinks" should {
		"load the Cookies, privacy and terms page and have the correct URLs" in new SetupPage {
			val result = controller.policyLinks()(FakeRequest())
			val document = Jsoup.parse(contentAsString(result))

			document.title should include("Cookies, privacy and terms")

			document.getElementById("main-heading").text() shouldBe "Cookies, privacy and terms"
			document.getElementById("ch-cookies").attr("href") shouldBe "http://resources.companieshouse.gov.uk/legal/cookies.shtml"
			document.getElementById("hmrc-cookies").attr("href") shouldBe "https://www.tax.service.gov.uk/help/cookies"
			document.getElementById("ch-policy").attr("href") shouldBe "https://www.gov.uk/government/organisations/companies-house/about/personal-information-charter"
			document.getElementById("hmrc-policy").attr("href") shouldBe "https://www.tax.service.gov.uk/help/privacy"
			document.getElementById("ch-terms").attr("href") shouldBe "http://resources.companieshouse.gov.uk/serviceInformation.shtml"
			document.getElementById("hmrc-terms").attr("href") shouldBe "https://www.tax.service.gov.uk/help/terms-and-conditions"
		}
	}
}