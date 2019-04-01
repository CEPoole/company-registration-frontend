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

package models

import play.api.libs.json.Json

case class Groups(
                   groupRelief: Boolean = true,
                   nameOfCompany: Option[GroupCompanyName],
                   address: Option[CHROAddress],
                   groupUTR: Option[GroupUTR]
                 )

case class GroupCompanyName(name: String, nameType: GroupCompanyNameEnum.Value)

case class GroupUTR(UTR: String, utr : Option[String])

object GroupUTR{
  implicit val format = Json.format[GroupUTR]
}

object GroupCompanyNameEnum extends Enumeration {
  val Other = Value
  val CohoEntered = Value
}

case class GroupRelief(
                      groupRelief: String = ""
                      )

object GroupRelief {
  implicit val format = Json.format[GroupRelief]
}


case class Shareholders(shareHolderName: List[String],
                        other: Option[String])




sealed trait GroupReliefResponse
case class GroupReliefSuccessResponse(response: GroupRelief) extends GroupReliefResponse
case object GroupReliefNotFoundResponse extends GroupReliefResponse
case object GroupReliefForbiddenResponse extends GroupReliefResponse
case class GroupReliefErrorResponse(err: Exception) extends GroupReliefResponse

sealed trait GroupUtrResponse
case class GroupUtrSuccessResponse(response: GroupUTR) extends GroupUtrResponse
case object GroupUtrNotFoundResponse extends GroupUtrResponse
case object GroupUtrForbiddenResponse extends GroupUtrResponse
case class GroupUtrErrorResponse(err: Exception) extends GroupUtrResponse
