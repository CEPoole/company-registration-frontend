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

package utils

trait DateUtil {

  /**
    * splits an optional string date in a non-specified format by the symbol
    * and returns each segment as an option in the same format as the provided date
    */
  def splitOptDateBy(symbol: String, date: Option[String]): (Option[String], Option[String], Option[String]) = {
    date.fold[(Option[String], Option[String], Option[String])](
      (None, None, None)
    )(_.split(symbol).toList match {
      case List(a, b, c) => (Some(a), Some(b), Some(c))
      case _ => (None, None, None)
    })
  }
}
