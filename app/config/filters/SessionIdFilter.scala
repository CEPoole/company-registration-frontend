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

package config.filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Logger
import play.api.http.DefaultHttpFilters
import play.api.mvc.Results.Redirect
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters
import utils.SCRSValidators.desSessionRegex

import scala.concurrent.Future

class SCRSFilters @Inject()(defaultFilters: FrontendFilters,
                            sessionIdFilter: SessionIdFilter) extends DefaultHttpFilters(defaultFilters.filters :+ sessionIdFilter: _*)

class SessionIdFilterImpl @Inject()(val mat: Materializer) extends SessionIdFilter

trait SessionIdFilter extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val hc = HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))
    hc.sessionId match {
      case Some(sessionId) if !sessionId.value.matches(desSessionRegex) =>
        Logger.warn(s"[IncorrectSessionId] The session Id ${sessionId.value} doesn't match the DES schema. Redirecting the user to sign in")
        Future.successful(Redirect(controllers.reg.routes.SignInOutController.postSignIn(None)).withNewSession)
      case _ => f(rh)
    }
  }
}