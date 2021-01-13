/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.test

import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import services.internal.TestIncorporationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class TestIncorporateControllerImpl @Inject()(val checkIncorpService: TestIncorporationService,
                                              mcc: MessagesControllerComponents) extends TestIncorporateController(mcc)

abstract class TestIncorporateController(mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  val checkIncorpService: TestIncorporationService

  implicit val ec: ExecutionContext = mcc.executionContext

  def incorporate(txId: String, accepted: Boolean) = Action.async {
    implicit request =>
      checkIncorpService.incorporateTransactionId(txId, accepted) map { success =>
        Ok(if (success) s"[SUCCESS] incorporating $txId" else s"[FAILED] to incorporate $txId")
      }
  }
}
