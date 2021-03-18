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

package www

import itutil.servicestubs.TakeoverStub
import itutil.{IntegrationSpecBase, LoginStub}
import models._
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.Json

import java.util.UUID

class SummaryControllerISpec extends IntegrationSpecBase with LoginStub with TakeoverStub {
  val userId = "/bar/foo"
  val csrfToken = UUID.randomUUID().toString
  val regId = "5"
  val sessionCookie = () => getSessionCookie(Map("csrfToken" -> csrfToken), userId)
  val testAddress: NewAddress = NewAddress("testLine1", "testLine2", None, None, None, None, None)
  val takeoverDetails = Some(TakeoverDetails(replacingAnotherBusiness = true, Some("test name"), Some(testAddress), Some("test previous name"), Some(testAddress)))
  val incompleteTakeoverDetails = Some(TakeoverDetails(replacingAnotherBusiness = true))
  lazy val defaultCookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  "Display summary correctly with full model" in {
    stubAuthorisation()
    stubGetTakeoverDetails(regId, 200, takeoverDetails)
    stubSuccessfulLogin(userId = userId)
    stubKeystore(SessionId, regId)
    stubBusinessRegRetrieveMetaDataWithRegId(regId, 200, Json.toJson(BusinessRegistration(regId, "123", "en", Some("director"), Links(Some("foo"), Some("bar")))).toString())
    stubGet(s"/company-registration/corporation-tax-registration/$regId/accounting-details", 200, """{"accountingDateStatus":"FUTURE_DATE", "startDateOfBusiness":"2019-01-02", "links": {}}""")

    stubGet(s"/company-registration/corporation-tax-registration/$regId/contact-details", 200, Json.parse(
      """|{
         | "contactDaytimeTelephoneNumber": "12345678",
         | "contactMobileNumber": "45678",
         | "contactEmail": "foo@foo.com",
         | "links": {
         | }
         |}
      """.stripMargin).toString())

    stubGet(s"/company-registration/corporation-tax-registration/$regId/company-details", 200, Json.toJson(CompanyDetails(
      "testCompanyName",
      CHROAddress(
        "Premises", "Line1", Some("Line2"), "Locality", "Country", Some("PO Box"), Some("FX1 1ZZ"), Some("Region")
      ),
      PPOB(
        "RO", None
      ),
      "testJurisdiction"
    )).toString())

    stubGet(s"/company-registration/corporation-tax-registration/$regId/trading-details", 200, Json.toJson(TradingDetails("true")).toString)
    stubGet(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration", 200,
      s"""{
         |    "registrationID" : "${regId}",
         |    "status" : "draft",
         |        "verifiedEmail" : {
         |        "address" : "user@test.com",
         |        "type" : "GG",
         |        "link-sent" : true,
         |        "verified" : true,
         |        "return-link-email-sent" : false
         |    }
         |}""".stripMargin)

    val fResponse = await(buildClient(controllers.reg.routes.SummaryController.show().url)
      .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie(), "Csrf-Token" -> "nocheck")
      .get())
    val doc = Jsoup.parse(fResponse.body)
    fResponse.status shouldBe 200
    doc.getElementById("companyContactDetails").id shouldBe "companyContactDetails"
    intercept[Exception](doc.getElementById("companyContactName").html)
    doc.getElementById("change-contact-details").attr("href").contains(controllers.reg.routes.CompanyContactDetailsController.show().url) shouldBe true
  }

  "Redirect to the Takeover Information Needed page if Takeover information is missing but replacingAnotherBusiness is true" in {
    stubAuthorisation()
    stubGetTakeoverDetails(regId, 200, incompleteTakeoverDetails)
    stubSuccessfulLogin(userId = userId)
    stubKeystore(SessionId, regId)
    stubBusinessRegRetrieveMetaDataWithRegId(regId, 200, Json.toJson(BusinessRegistration(regId, "123", "en", Some("director"), Links(Some("foo"), Some("bar")))).toString())
    stubGet(s"/company-registration/corporation-tax-registration/$regId/accounting-details", 200, """{"accountingDateStatus":"FUTURE_DATE", "startDateOfBusiness":"2019-01-02", "links": {}}""")

    stubGet(s"/company-registration/corporation-tax-registration/$regId/contact-details", 200, Json.parse(
      """|{
         | "contactDaytimeTelephoneNumber": "12345678",
         | "contactMobileNumber": "45678",
         | "contactEmail": "foo@foo.com",
         | "links": {
         | }
         |}
      """.stripMargin).toString())

    stubGet(s"/company-registration/corporation-tax-registration/$regId/company-details", 200, Json.toJson(CompanyDetails(
      "testCompanyName",
      CHROAddress(
        "Premises", "Line1", Some("Line2"), "Locality", "Country", Some("PO Box"), Some("FX1 1ZZ"), Some("Region")
      ),
      PPOB(
        "RO", None
      ),
      "testJurisdiction"
    )).toString())

    stubGet(s"/company-registration/corporation-tax-registration/$regId/trading-details", 200, Json.toJson(TradingDetails("true")).toString)
    stubGet(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration", 200,
      s"""{
         |    "registrationID" : "${regId}",
         |    "status" : "draft",
         |        "verifiedEmail" : {
         |        "address" : "user@test.com",
         |        "type" : "GG",
         |        "link-sent" : true,
         |        "verified" : true,
         |        "return-link-email-sent" : false
         |    }
         |}""".stripMargin)

    val fResponse = await(buildClient(controllers.reg.routes.SummaryController.show().url)
      .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie(), "Csrf-Token" -> "nocheck")
      .get())
    fResponse.status shouldBe 303
    fResponse.header(HeaderNames.LOCATION).get shouldBe controllers.takeovers.routes.TakeoverInformationNeededController.show().url
  }


}
