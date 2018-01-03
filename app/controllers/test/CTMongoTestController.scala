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

package controllers.test

import javax.inject.{Inject, Singleton}

import config.WSHttp
import play.api.Application
import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }


object CTMongoTestController extends CTMongoTestController with ServicesConfig {
  val http = WSHttp
  val ctUrl = baseUrl("company-registration")
}

trait CTMongoTestController extends FrontendController {

  val http: HttpGet
  val ctUrl: String

  def dropCollection = Action.async {
    implicit request =>
      for {
        ct <- dropCTCollection
        ctMessage = (ct \ "message").as[String]
      } yield {
        Ok(s"$ctMessage")
      }
  }

  def dropCTCollection(implicit hc: HeaderCarrier): Future[JsValue] = {
    http.GET[JsValue](s"$ctUrl/company-registration/test-only/drop-ct")
  }
}
