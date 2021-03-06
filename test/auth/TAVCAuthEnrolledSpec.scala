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

package auth

import java.net.URLEncoder

import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.http.Status
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.test.Helpers._
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.Future


class TAVCAuthEnrolledSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  val internalId = "Int-312e5e92-762e-423b-ac3d-8686af27fdb5"

  "Government Gateway Provider" should {
    "have an account type additional parameter set to organisation" in {
      val ggw = new GovernmentGatewayProvider(MockConfig.introductionUrl, MockConfig.ggSignInUrl)
      ggw.additionalLoginParameters("accountType") shouldEqual List("organisation")
    }
  }

  "Government Gateway Provider" should {
    "have a login url set from its second constructor parameter" in {
      val ggw = new GovernmentGatewayProvider(MockConfig.introductionUrl, MockConfig.ggSignInUrl)
      ggw.loginURL shouldEqual MockConfig.ggSignInUrl
    }
  }

  "Government Gateway Provider" should {
    "have a continueURL constructed from its first constructor parameter" in {
      val ggw = new GovernmentGatewayProvider(MockConfig.introductionUrl, MockConfig.ggSignInUrl)
      ggw.continueURL shouldEqual MockConfig.introductionUrl
    }
  }

  "Government Gateway Provider" should {
    "handle a session timeout with a redirect" in {
      implicit val fakeRequest = FakeRequest()
      val ggw = new GovernmentGatewayProvider(MockConfig.introductionUrl, MockConfig.ggSignInUrl)
      val timeoutHandler = ggw.handleSessionTimeout(fakeRequest)
      status(timeoutHandler) shouldBe SEE_OTHER
      redirectLocation(timeoutHandler) shouldBe Some("/investment-tax-relief-attachments/session-timeout")
    }
  }

  "Extract previously logged in time of logged in user" should {
    s"return ${ggUser.previouslyLoggedInAt.get}" in {
      val user = TAVCUser(ggUser.allowedAuthContext, internalId)
      user.previouslyLoggedInAt shouldBe ggUser.previouslyLoggedInAt
    }
  }

  "Calling authenticated async action with no login session" should {
    "result in a redirect to login" in {

      val result = AuthEnrolledTestController.authorisedAsyncAction(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/gg/sign-in?continue=${URLEncoder.encode(MockConfig.introductionUrl)}&origin=investment-tax-relief-attachments-frontend&accountType=organisation")
    }
  }

  "Calling authenticated async action with a default GG login session with no TAVC enrolment" should {
    "result in a redirect to subscription" in {
      implicit val hc = HeaderCarrier()
      when(AuthEnrolledTestController.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(None))
      val result = AuthEnrolledTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId))
      redirectLocation(result) shouldBe Some("/investment-tax-relief-subscription/")
    }
  }

  "Calling authenticated async action with a GG login session with a HMRC-TAVC-ORG enrolment" should {
    "result in a status OK" in {
      implicit val hc = HeaderCarrier()
      val enrolledUser = Enrolment("HMRC-TAVC-ORG", Seq(Identifier("TavcReference", "1234")), "Activated")
      when(AuthEnrolledTestController.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Option(enrolledUser)))
      val result = AuthEnrolledTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId))
      status(result) shouldBe Status.OK
    }
  }


  "Calling authenticated getTavCReferenceNumber when it is not found on the enrolement" should {
    "return an empty TavcReference" in {
      implicit val hc = HeaderCarrier()
      when(AuthEnrolledTestController.enrolmentConnector.getTavcReferenceNumber(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(""))
      val result = AuthEnrolledTestController.getTavCReferenceNumber()(hc)
      await(result) shouldBe ""
    }
  }

  "Calling authenticated getTavCReferenceNumber when it is exists on the enrolement" should {
    "return the TavcReference" in {
      implicit val hc = HeaderCarrier()
      when(AuthEnrolledTestController.enrolmentConnector.getTavcReferenceNumber(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful("XATAVC000123456"))
      val result = AuthEnrolledTestController.getTavCReferenceNumber()(hc)
      await(result) shouldBe "XATAVC000123456"
    }
  }
}
