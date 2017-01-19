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

package controllers

import auth.{MockAuthConnector, MockConfig}
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.EnrolmentConnector
import controllers.helpers.ControllerSpec
import play.api.test.Helpers._


class SignOutControllerSpec extends ControllerSpec {

  object TestController extends SignOutController {
    override lazy val enrolmentConnector = mockEnrolmentConnector
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
  }

  "SignOutController" should {
    "Use the correct enrolment connector" in {
      SignOutController.enrolmentConnector shouldBe EnrolmentConnector
    }
    "Use the correct application config" in {
      SignOutController.applicationConfig shouldBe FrontendAppConfig
    }
    "Use the correct auth connector" in {
      SignOutController.authConnector shouldBe FrontendAuthConnector
    }
  }

  "SignOutController.signout" should {

    "Redirect to sign-out" in {
      mockEnrolledRequest()
      showWithSessionAndAuth(TestController.signout())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe
            Some(s"${TestController.applicationConfig.ggSignOutUrl}?continue=${TestController.applicationConfig.signOutPageUrl}")
        }
      )
    }

  }

  "SignOutController.show" should {

    "Show the signed out page" in {
      mockNotEnrolledRequest()
      showWithoutSession(TestController.show())(
        result => {
          status(result) shouldBe OK
        }
      )
    }

  }

}