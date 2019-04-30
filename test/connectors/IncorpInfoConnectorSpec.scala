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

package connectors

import helpers.SCRSSpec
import mocks.MetricServiceMock
import models.{CHROAddress, Shareholder}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, Upstream5xxResponse}

import scala.concurrent.Future

class IncorpInfoConnectorSpec extends SCRSSpec {

    val iiUrl = "testIIUrl"

    class Setup {
        val connector = new IncorpInfoConnector {
          override val incorpInfoUrl = iiUrl
          override val wSHttp = mockWSHttp
          override val metricsService = MetricServiceMock
         }
     }

    val transId = "txID-12345"
    val companyProfileUrl = s"$iiUrl/$transId/company-profile"

  "getCompanyProfile" should {

    val json = Json.parse("""{"test":"json"}""")

    "return a Json object" in new Setup {
      mockHttpGet(companyProfileUrl, Future.successful(json))
      val res = await(connector.getCompanyProfile(transId))
      res shouldBe json
    }
  }

  "getCompanyName" should {

    val json = Json.parse("""{"company_name":"testCompanyName"}""")

    "return a company name from the fetched json" in new Setup {
      mockHttpGet(companyProfileUrl, Future.successful(json))
      val res = await(connector.getCompanyName(transId))
      res shouldBe "testCompanyName"
    }
  }

  "injectTestIncorporationUpdate" should {
    "set up a successful incorporation update" in new Setup {
      mockHttpGet(s"$iiUrl/test-only/add-incorp-update/?txId=$transId&date=2018-1-1&crn=12345678&success=true", Future.successful(HttpResponse(200)))
      val res = await(connector.injectTestIncorporationUpdate(transId, isSuccess = true))
      res shouldBe true
    }
    "set up a rejected incorporation update" in new Setup {
      mockHttpGet(s"$iiUrl/test-only/add-incorp-update/?txId=$transId&date=2018-1-1&success=false", Future.successful(HttpResponse(200)))
      val res = await(connector.injectTestIncorporationUpdate(transId, isSuccess = false))
      res shouldBe true
    }
    "recover any exceptions returned by II" in new Setup {
      mockHttpGet(s"$iiUrl/test-only/add-incorp-update/?txId=$transId&date=2018-1-1&success=false", Future.failed(new NotFoundException("404")))
      val res = await(connector.injectTestIncorporationUpdate(transId, isSuccess = false))
      res shouldBe false
    }
  }

  "manuallyTriggerIncorporationUpdate" should {
    "trigger subscriptions to be fired" in new Setup {
      mockHttpGet(s"$iiUrl/test-only/manual-trigger/fireSubs", Future.successful(HttpResponse(200)))
      val res = await(connector.manuallyTriggerIncorporationUpdate)
      res shouldBe true
    }
    "persist any exceptions returned by II" in new Setup {
      mockHttpGet(s"$iiUrl/test-only/manual-trigger/fireSubs", Future.failed(new Upstream5xxResponse("502", 502, 502)))
      val res = await(connector.manuallyTriggerIncorporationUpdate)
      res shouldBe false
    }
  }
  "returnListOfShareholdersFromTxApi" should {
    val listOfShareHoldersFromII = Json.parse("""[{
                                                |  "percentage_dividend_rights": 75,
                                                |  "percentage_voting_rights": 75.0,
                                                |  "percentage_capital_rights": 75,
                                                |  "corporate_name": "big company",
                                                |    "address": {
                                                |    "premises": "11",
                                                |    "address_line_1": "Add L1",
                                                |    "address_line_2": "Add L2",
                                                |    "locality": "London",
                                                |    "country": "United Kingdom",
                                                |    "postal_code": "ZZ1 1ZZ"
                                                |      }
                                                |    },{
                                                |  "percentage_dividend_rights": 75,
                                                |  "percentage_voting_rights": 74.3,
                                                |  "percentage_capital_rights": 75,
                                                |  "corporate_name": "big company 1",
                                                |    "address": {
                                                |    "premises": "11 FOO",
                                                |    "address_line_1": "Add L1 1",
                                                |    "address_line_2": "Add L2 2",
                                                |    "locality": "London 1",
                                                |    "country": "United Kingdom 1",
                                                |    "postal_code": "ZZ1 1ZZ 1"
                                                |      }
                                                |    },{
                                                |    "surname": "foo will never show",
                                                |    "forename" : "bar will never show",
                                                |    "percentage_dividend_rights": 75,
                                                |    "percentage_voting_rights": 75,
                                                |    "percentage_capital_rights": 75,
                                                |    "address": {
                                                |    "premises": "11",
                                                |    "address_line_1": "Add L1",
                                                |    "address_line_2": "Add L2",
                                                |    "locality": "London",
                                                |    "country": "United Kingdom",
                                                |    "postal_code": "ZZ1 1ZZ"
                                                |      }
                                                |    }
                                                |]""".stripMargin)

    val listOfShareholders = List(
      Shareholder("big company",Some(75.0),Some(75.0),Some(75.0),CHROAddress("11","Add L1",Some("Add L2"),"London","United Kingdom",None,Some("ZZ1 1ZZ"),None)),
      Shareholder("big company 1",Some(74.3),Some(75.0),Some(75.0),CHROAddress("11 FOO","Add L1 1",Some("Add L2 2"),"London 1","United Kingdom 1",None,Some("ZZ1 1ZZ 1"),None))
    )
    "return a list of shareholders in right of either when 200 returned" in new Setup {

      mockHttpGet(s"$iiUrl/shareholders/$transId",
        Future.successful(
          HttpResponse(200,Some(listOfShareHoldersFromII))))

      val res = await(connector.returnListOfShareholdersFromTxApi(transId))
      res.right.get shouldBe listOfShareholders

    }
    "return an empty list for a 204" in new Setup {
      mockHttpGet(s"$iiUrl/shareholders/$transId",
        Future.successful(
          HttpResponse(204,Some(listOfShareHoldersFromII))))

      val res = await(connector.returnListOfShareholdersFromTxApi(transId))
      res.right.get shouldBe List.empty[Shareholder]
    }
    "return empty list if json is unparsable as a list of shareholders" in new Setup {
      mockHttpGet(s"$iiUrl/shareholders/$transId",
        Future.successful(
          HttpResponse(200,Some(Json.obj("foo" -> "bar")))))

      val res = await(connector.returnListOfShareholdersFromTxApi(transId))
      res.right.get shouldBe List.empty[Shareholder]
    }
    "return a left if txapi returns a exception" in new Setup {
      val ex = new Exception("foo")
      mockHttpGet(s"$iiUrl/shareholders/$transId",
        Future.failed(ex))

      val res = await(connector.returnListOfShareholdersFromTxApi(transId))
      res.left.get shouldBe ex
    }
  }
}


