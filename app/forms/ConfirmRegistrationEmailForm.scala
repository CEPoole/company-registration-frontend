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

import models.{ConfirmRegistrationEmailModel, RequiredBooleanForm}
import play.api.data.Forms._
import play.api.data.Form


object ConfirmRegistrationEmailForm extends RequiredBooleanForm {

  override val errorMsg = "page.reg.confirmRegistrationEmail.error"
  def form = Form(
    mapping(
      "confirmRegistrationEmail" -> requiredBoolean
    )(ConfirmRegistrationEmailModel.apply)(ConfirmRegistrationEmailModel.unapply)
  )
}