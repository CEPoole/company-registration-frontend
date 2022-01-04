/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import helpers.UnitSpec
import mocks.AppConfigMock
import models._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi, MessagesProvider}
import play.api.mvc.Call

class AddressLookupConfigBuilderServiceSpec()(implicit messagesProvider: MessagesProvider) extends UnitSpec with GuiceOneAppPerSuite with AppConfigMock {

  implicit val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  object TestService extends AddressLookupConfigBuilderService(mockAppConfig) {
    override lazy val companyRegistrationFrontendURL = "testCompanyRegUrl"
    override lazy val timeoutLength = 22666
    override lazy val accessibilityFooterUrl = "testCompanyRegUrl/register-your-company/accessibility-statement?pageUri=%2F?service=address-lookup&userAction=lookup"
  }

  "buildConfig" should {
    "return a filled AlfJourneyConfig model" in {

      val result: AlfJourneyConfig = TestService.buildConfig(
        handbackLocation = Call("GET", "/foo"),
        specificJourneyKey = "PPOB",
        lookupPageHeading = Messages("page.addressLookup.PPOB.lookup.heading"),
        confirmPageHeading = Messages("page.addressLookup.PPOB.confirm.description")
      )

      val expectedConfig: AlfJourneyConfig = AlfJourneyConfig(

        version = AlfJourneyConfig.defaultConfigVersion,
        options = JourneyOptions(
          continueUrl = "testCompanyRegUrl/foo",
          homeNavHref = "http://www.hmrc.gov.uk/",
          accessibilityFooterUrl = "testCompanyRegUrl/register-your-company/accessibility-statement?pageUri=%2F?service=address-lookup&userAction=lookup",
          deskProServiceName = "SCRS",
          showPhaseBanner = true,
          alphaPhase = false,
          showBackButtons = true,
          includeHMRCBranding = false,
          disableTranslations = true,

          selectPageConfig = SelectPageConfig(
            proposalListLimit = 30,
            showSearchAgainLink = true
          ),

          confirmPageConfig = ConfirmPageConfig(
            showSearchAgainLink = false,
            showSubHeadingAndInfo = false,
            showChangeLink = true
          ),

          timeoutConfig = TimeoutConfig(
            timeoutAmount = 22666,
            timeoutUrl = "testCompanyRegUrl/register-your-company/error/timeout"
          )
        ),
        labels = JourneyLabels(en = LanguageLabels(
          appLevelLabels = AppLevelLabels(
            navTitle = "Set up a limited company and register for Corporation Tax",
            phaseBannerHtml = "This is a new service. Help us improve it - send your <a href='https://www.tax.service.gov.uk/register-for-paye/feedback'>feedback</a>."
          ),
          SelectPageLabels(
            title = "Choose an address",
            heading = "Choose an address",
            searchAgainLinkText = "Search again",
            editAddressLinkText = "The address is not on the list"
          ),
          LookupPageLabels(
            title = "Find the address where the company will carry out most of its business activities",
            heading = "Find the address where the company will carry out most of its business activities",
            filterLabel = "Property name or number",
            submitLabel = "Find address",
            manualAddressLinkText = "Enter address manually"
          ),
          EditPageLabels(
            title = "Enter an address",
            heading = "Enter an address",
            line1Label = "Address line 1",
            line2Label = "Address line 2",
            line3Label = "Address line 3"
          ),
          ConfirmPageLabels(
            title = "Confirm where the company will carry out most of its business activities",
            heading = "Confirm where the company will carry out most of its business activities",
            submitLabel = "Confirm and continue",
            changeLinkText = "Change"
          )
        )
        )
      )

      result shouldBe expectedConfig

    }
  }

}
