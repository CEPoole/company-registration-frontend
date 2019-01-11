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

package services

import connectors.AddressLookupConnector
import fixtures.AddressFixture
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AddressLookupFrontendServiceSpec extends UnitSpec with MockitoSugar with AddressFixture {

  val mockAddressLookupConnector = mock[AddressLookupConnector]
  val mockMetricsService = mock[MetricsService]

  class Setup {
    val service = new AddressLookupFrontendService {
      override val addressLookupFrontendConnector: AddressLookupConnector = mockAddressLookupConnector
      override val metricsService: MetricsService = mockMetricsService
    }
  }

  implicit val hc = HeaderCarrier()

  "getAddress" should {

    val id = "testID"
    val address = validNewAddress

    "return an address" in new Setup {

      when(mockAddressLookupConnector.getAddress(eqTo(id))(any()))
        .thenReturn(Future.successful(address))

      val req = FakeRequest("GET", s"/test-uri?id=$id")
      val result = await(service.getAddress(hc, req))

      result shouldBe address
    }

    "throw a QueryStringMissingException" in new Setup {
      val req = FakeRequest("GET", "/test-uri")

      intercept[QueryStringMissingException](await(service.getAddress(hc, req)))
    }
  }

  "buildAddressLookupUrl" should {

    val id = "testID"
    val timeoutUrl = "/test/timeout/url"
    val req = FakeRequest("GET", "/test-uri")
    val call = Call("testUrl", "")

    "return an address" in new Setup {
      when(mockAddressLookupConnector.getOnRampURL(any(), any[Call]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(id))

      await(service.buildAddressLookupUrl(timeoutUrl, call)) shouldBe id
    }
  }
}
