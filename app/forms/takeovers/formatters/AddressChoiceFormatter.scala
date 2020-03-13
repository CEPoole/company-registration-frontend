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

package forms.takeovers.formatters

import models.takeovers.{AddressChoice, OtherAddress, PreselectedAddress}
import play.api.data.FormError
import play.api.data.format.Formatter
import utils.StringUtil._

object AddressChoiceFormatter {

  val OtherKey: String = "Other"

  def addressChoiceFormatter(businessName: String, addressCount: Int): Formatter[AddressChoice] = new Formatter[AddressChoice] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AddressChoice] = {
      val error = Left(Seq(FormError(key, "error.otherBusinessAddress.required", Seq(businessName))))
      data.get(key) match {
        case Some(choice) if choice == OtherKey => Right(OtherAddress)
        case Some(choice) => choice.toIntOption match {
          case Some(index) if index < addressCount && index >= 0 => Right(PreselectedAddress(index))
          case _ => error
        }
        case None => error
      }
    }

    override def unbind(key: String, value: AddressChoice): Map[String, String] = value match {
      case PreselectedAddress(index) => Map(key -> index.toString)
      case OtherAddress => Map(key -> OtherKey)
    }
  }

}
