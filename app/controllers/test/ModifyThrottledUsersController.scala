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

package controllers.test

import config.{FrontendAppConfig, WSHttp}
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class ModifyThrottledUsersControllerImpl @Inject()(val appConfig: FrontendAppConfig,
                                                   val wSHttp: WSHttp,
                                                   mcc: MessagesControllerComponents) extends ModifyThrottledUsersController(mcc) {
  lazy val crUrl = appConfig.servicesConfig.baseUrl("company-registration")
}

abstract class ModifyThrottledUsersController(mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext
  val wSHttp: CoreGet
  val crUrl: String

  def modifyThrottledUsers(usersIn: Int) = Action.async {
    implicit request =>
      wSHttp.GET[JsValue](s"$crUrl/company-registration/test-only/modify-throttled-users/$usersIn").map { res =>
        val usersIn = (res \ "users_in").as[Int]
        Ok(s"users_in set to $usersIn")
      }
  }
}