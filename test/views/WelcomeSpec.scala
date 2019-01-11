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

package views

import config.AppConfig
import controllers.reg.WelcomeController
import mocks.SCRSMocks
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class WelcomeSpec extends UnitSpec with WithFakeApplication with MockitoSugar with SCRSMocks {
	class SetupPage {
		val controller = new WelcomeController{
			val ggUrl = ""
			val frontendUrl = ""
			override val appConfig: AppConfig = mockAppConfig
		}
		when(mockAppConfig.piwikURL).thenReturn(None)
	}
	"WelcomeController.show" should {
		"make sure that welcome page has the correct elements for when PAYE feature switch DISABLED" in new SetupPage {
			System.clearProperty("feature.paye")
			System.setProperty("feature.paye", "false")
			System.setProperty("feature.signPosting", "false")
			val result = controller.show()(FakeRequest())
			val document = Jsoup.parse(contentAsString(result))

			document.title() shouldBe "Register a limited company"

			Map(
				"main-heading" -> "Register a limited company",
				"description-one" -> "Use this service to:",
				"inc" -> "register a limited company with Companies House",
				"ct" -> "register for Corporation Tax with HM Revenue and Customs (HMRC)",
				"subheading" -> "Register a limited company and set up Corporation Tax",
				"app" -> "Your application:",
				"30" -> "shouldn't take more than 30 minutes to complete",
				"one" -> "doesn't have to be done in one go - you can save your details and finish it later",
				"90" -> "must be completed within 90 days of starting - any information you've entered won't be saved after that time",
				"cost" -> "It costs £12 to set up a company. Your information will be shared between Companies House and HMRC during the application process."
			) foreach { case (element, message) =>
				document.getElementById(element).text() shouldBe message
			}

		}

		"make sure that welcome page has the correct elements for when PAYE feature switch ENABLED" in new SetupPage {
			System.clearProperty("feature.paye")
			System.setProperty("feature.paye", "true")
			System.setProperty("feature.signPosting", "false")
			val result = controller.show()(FakeRequest())
			val document = Jsoup.parse(contentAsString(result))

			document.title() shouldBe "Register a limited company"

			Map(
				"main-heading" -> "Register a limited company",
				"description-one" -> "Use this service to:",
				"inc_ct" -> "set up a limited company and register it for Corporation Tax",
				"paye" -> "register as an employer for PAYE",
				"subheading" -> "Register a limited company and set up Corporation Tax",
				"app" -> "Your application:",
				"30" -> "shouldn't take more than 30 minutes to complete",
				"one" -> "doesn't have to be done in one go - you can save your details and finish it later",
				"90" -> "must be completed within 90 days of starting - any information you've entered won't be saved after that time",
				"cost" -> "It costs £12 to set up a company. Your information will be shared between Companies House and HMRC during the application process."
			) foreach { case (element, message) =>
				document.getElementById(element).text() shouldBe message
			}

		}
	}}
