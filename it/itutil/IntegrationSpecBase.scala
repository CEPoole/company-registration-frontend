/*
 * Copyright 2017 HM Revenue & Customs
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
package itutil

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.{FeatureSwitch, FeatureSwitchManager, SCRSFeatureSwitches}

trait IntegrationSpecBase extends UnitSpec
  with GivenWhenThen
  with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val testkey = "Fak3-t0K3n-f0r-pUBLic-r3p0SiT0rY"

  def setupFeatures(cohoFirstHandOff: Boolean = false,
                    businessActivitiesHandOff: Boolean = false,
                    paye: Boolean = false,
                    vat: Boolean = false,
                    signPosting: Boolean = false) = {
    def enableFeature(fs: FeatureSwitch, enabled: Boolean) = {
      enabled match {
        case true => app.injector.instanceOf[FeatureSwitchManager].enable(fs)
        case _ => app.injector.instanceOf[FeatureSwitchManager].disable(fs)
      }
    }
    enableFeature(app.injector.instanceOf[SCRSFeatureSwitches].cohoFirstHandOff, cohoFirstHandOff)
    enableFeature(app.injector.instanceOf[SCRSFeatureSwitches].businessActivitiesHandOff, businessActivitiesHandOff)
    enableFeature(app.injector.instanceOf[SCRSFeatureSwitches].paye, paye)
    enableFeature(app.injector.instanceOf[SCRSFeatureSwitches].vat, vat)

  }

  override def beforeEach() = {
    resetWiremock()
  }

  override def beforeAll() = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll() = {
    stopWiremock()
    super.afterAll()
  }
}
