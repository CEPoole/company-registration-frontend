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

package forms

import models.RegistrationEmailModel
import play.api.data.Form
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class RegistrationEmailFormSpec extends UnitSpec with WithFakeApplication {

  def fillForm(radioButtonValue: String, deValue : Option[String]) : Form[RegistrationEmailModel] = {
    RegistrationEmailForm.form.bind(Map("registrationEmail" -> radioButtonValue, "DifferentEmail" -> deValue.getOrElse("")))
  }

  " form" should {
    "have currentEmail as registrationEmail and transform DifferentEmail to NONE" in {
      val result = fillForm("currentEmail", Option("addw,awdwa@e,c"))

      result.get.currentEmail shouldBe "currentEmail"
      result.get.differentEmail shouldBe None
    }
    "bind successfully with differentEmail as registrationEmail and check the value" in {
      val result = fillForm("differentEmail", Option("abc@def.co.uk"))

      result.get.currentEmail shouldBe "differentEmail"
      result.get.differentEmail shouldBe Option("abc@def.co.uk")
    }
    "return form error with differentEmail with no value entered " in {
      val result = fillForm("differentEmail", Option(""))

      result.errors.head.message shouldBe "error.DifferentEmail.required"
    }
    "return form error with differentEmail invalid special chars " in {
      val result = fillForm("differentEmail", Option("abc^@def.co.uk"))

      result.data("registrationEmail") shouldBe "differentEmail"
      result.data("DifferentEmail") shouldBe "abc^@def.co.uk"
      result.errors.head.key shouldBe "DifferentEmail"
      result.errors.head.message shouldBe "Enter an email address using only letters, numbers and - . @ _ +"
    }
    "return form error with DifferentEmail length over 70 characters " in {
      val result = fillForm("differentEmail", Option("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.co.uk"))

      result.data("registrationEmail") shouldBe "differentEmail"
      result.data("DifferentEmail") shouldBe "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.co.uk"
      result.errors.head.key shouldBe "DifferentEmail"
      result.errors.head.message shouldBe "Enter an email address using 70 characters or less"
    }
    "return form error with no radio button selected " in {
      val result = fillForm("", Option(""))

      result.errors.head.message shouldBe "error.registrationEmail.required"
    }
    "return form error with no radio button selected with text in DifferentEmail text box " in {
      val result = fillForm("", Option("a@b.com"))

      result.errors.head.message shouldBe "error.registrationEmail.required"
    }
    "return form error with invalid name for radio button " in {
      val result = fillForm("RadioButtonOne", Option("a@b.com"))

      result.errors.head.message shouldBe "error.registrationEmail.required"
    }
  }
}
