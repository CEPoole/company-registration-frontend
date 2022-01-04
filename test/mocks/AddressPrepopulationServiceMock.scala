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

package mocks

import models.NewAddress
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import services.AddressPrepopulationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait AddressPrepopulationServiceMock extends MockitoSugar {
  val mockAddressPrepopulationService: AddressPrepopulationService = mock[AddressPrepopulationService]

  def mockRetrieveAddresses(registrationId: String)(response: Future[Seq[NewAddress]]): Unit =
    when(mockAddressPrepopulationService.retrieveAddresses(ArgumentMatchers.eq(registrationId))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

}
