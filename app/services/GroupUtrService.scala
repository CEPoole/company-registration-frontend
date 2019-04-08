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

import javax.inject.Inject

import connectors.CompanyRegistrationConnector
import connectors.KeystoreConnector
import models._
import uk.gov.hmrc.http.HeaderCarrier
import utils.SCRSExceptions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GroupUtrServiceImpl @Inject()(val keystoreConnector: KeystoreConnector,
                                          val compRegConnector: CompanyRegistrationConnector) extends GroupUtrService

trait GroupUtrService extends CommonService with SCRSExceptions {

  val compRegConnector: CompanyRegistrationConnector

  def updateGroupUtr(groupUtr : GroupUTR)(implicit hc: HeaderCarrier) : Future[GroupUtrResponse] = {
    for {
      regID <- fetchRegistrationID
//      tD <- compRegConnector.updateGroupRelief(regID, GroupReliefDetails)
    } yield {
      GroupUtrSuccessResponse(groupUtr)
    }
  }

  def retrieveOwningCompanyDetails(registrationID : String)(implicit hc: HeaderCarrier) : Future[(Some[GroupUTR],String)] = {
//    compRegConnector.retrieveGroupRelief(registrationID).map(_.fold(GroupRelief())(t => t))
    Future.successful(Some(GroupUTR(None)),"Group Parent Name")
  }
}