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

import _root_.helpers.SCRSSpec
import builders.AuthBuilder
import controllers.groups.GroupUtrController
import fixtures.UserDetailsFixture
import mocks.GroupUtrServiceMock
import models.{Email, GroupUTR, Groups}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class GroupUtrSpec extends SCRSSpec with UserDetailsFixture
  with WithFakeApplication with AuthBuilder with GroupUtrServiceMock {

  class Setup {
    val controller = new GroupUtrController {
      override val authConnector = mockAuthConnector
      override val groupUtrService = mockGroupUtrService
      override val compRegConnector = mockCompanyRegistrationConnector
      override val keystoreConnector= mockKeystoreConnector
      override val appConfig = mockAppConfig
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }

    val ctDocFirstTimeThrough =
    Json.parse(
      s"""
         |{
         |    "OID" : "123456789",
         |    "registrationID" : "1",
         |    "status" : "draft",
         |    "formCreationTimestamp" : "2016-10-25T12:20:45Z",
         |    "language" : "en",
         |    "verifiedEmail" : {
         |        "address" : "user@test.com",
         |        "type" : "GG",
         |        "link-sent" : true,
         |        "verified" : true,
         |        "return-link-email-sent" : false
         |    }
         |}""".stripMargin)
  }

  "show" should{
    "display the CompanyContactDetails page with the correct elements passed into it" in new Setup {

      mockKeystoreFetchAndGet("registrationID", Some("1"))
      CTRegistrationConnectorMocks.retrieveCTRegistration(ctDocFirstTimeThrough)
      when(mockCompanyRegistrationConnector.fetchCompanyName(any())(any())).thenReturn(Future.successful("testCompanyname1"))
      when(mockCompanyRegistrationConnector.retrieveEmail(any())(any())).thenReturn(Future.successful(Some(Email("verified@email","GG",true,true,true))))
      when(mockGroupUtrService.retrieveGroupUtr(any())(any())).thenReturn(Future.successful(GroupUTR("", None)))

      showWithAuthorisedUser(controller.show) {
        result =>
          val document = Jsoup.parse(contentAsString(result))

          document.title() shouldBe "Do you know XXXX's Unique Taxpayer Reference (UTR)?"
          document.getElementById("main-heading").text() shouldBe "Do you know [XXX]'s Unique Taxpayer Reference (UTR)?"
      }
    }
  }
}