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

package mocks

import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import services._

import scala.concurrent.Future

trait CompanyContactDetailsServiceMock {
  this: MockitoSugar =>

  lazy val mockCompanyContactDetailsService = mock[CompanyContactDetailsService]

  object CompanyContactDetailsServiceMocks {
    def fetchContactDetails(response: CompanyContactDetailsApi): OngoingStubbing[Future[CompanyContactDetailsApi]] = {
      when(mockCompanyContactDetailsService.fetchContactDetails(Matchers.any()))
        .thenReturn(Future.successful(response))
    }

    def updateContactDetails(response: CompanyContactDetailsResponse): OngoingStubbing[Future[CompanyContactDetailsResponse]] = {
      when(mockCompanyContactDetailsService.updateContactDetails(Matchers.any[CompanyContactDetailsApi]())(Matchers.any()))
        .thenReturn(Future.successful(response))
    }
  }
}