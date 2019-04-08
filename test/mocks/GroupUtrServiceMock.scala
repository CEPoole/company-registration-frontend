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

package mocks

import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import services._

import scala.concurrent.Future

trait GroupUtrServiceMock {
  this: MockitoSugar =>

  lazy val mockGroupUtrService = mock[GroupUtrService]

  object GroupUtrServiceMocks {
    def retrieveUtrRelief(response: GroupUTR): OngoingStubbing[Future[GroupUTR]] = {
      when(mockGroupUtrService.retrieveOwningCompanyDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Some(response(None)), "testGroupCompanyname1"))
    }

    def updateUtrRelief(response: GroupUtrResponse): OngoingStubbing[Future[GroupUtrResponse]] = {
      when(mockGroupUtrService.updateGroupUtr(Matchers.any[GroupUTR]())(Matchers.any()))
        .thenReturn(Future.successful(response))
    }
  }
}