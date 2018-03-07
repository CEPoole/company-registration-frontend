/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.data.format.Formatter
import play.api.data.validation.ValidationError
import play.api.data.{FormError, Forms}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.{maxLength, minLength, pattern}
import play.api.libs.json._


object Validation {

  def length(maxLen: Int, minLen: Int = 1): Reads[String] = maxLength[String](maxLen) keepAnd minLength[String](minLen)

  def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)

  def lengthFmt(maxLen: Int, minLen: Int = 1): Format[String] = readToFmt(length(maxLen, minLen))

  def yyyymmddValidator = pattern("^[0-9]{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$".r)

  def yyyymmddValidatorFmt = readToFmt(yyyymmddValidator)

  def withFilter[A](fmt: Format[A], error: ValidationError)(f: (A) => Boolean): Format[A] = {
    Format(fmt.filter(error)(f), fmt)
  }
}

trait HMRCAddressValidator {
  val maxLineLength = 27
}

trait CHAddressValidator {

  import Validation.lengthFmt

  val premisesValidator = lengthFmt(120)
  val lineValidator = lengthFmt(50)
  val countryValidator = lineValidator
  val postcodeValidator = lengthFmt(20)
  val regionValidator = lineValidator
}

trait EmptyStringValidator {
  def customErrorTextValidation = Forms.of[String](stringFormat)
  def stringFormat: Formatter[String] = new Formatter[String] {
    def message(key:String) = s"error.$key.required"
    private def getNonEmpty(key: String, data: Map[String, String]) = data.getOrElse(key, "").trim match {
      case "" => None
      case entry => Some(entry)
    }
    def bind(key: String, data: Map[String, String]) = getNonEmpty(key, data).toRight(Seq(FormError(key, message(key), Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }

}
