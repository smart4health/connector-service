package com.healthmetrix.connector.outbox.api.pairing.frontend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.encodeURL
import com.healthmetrix.connector.outbox.api.pairing.IframeRoutes
import com.healthmetrix.connector.outbox.api.pairing.util.OutboxControllerTestApplication
import com.healthmetrix.connector.outbox.api.pairing.util.withQueryParams
import com.healthmetrix.connector.outbox.api.pairing.validation.CheckPinAttributes
import com.healthmetrix.connector.outbox.usecases.CheckPinUseCase
import com.healthmetrix.connector.outbox.usecases.SendPinUseCase
import com.healthmetrix.connector.outbox.usecases.SendSmsUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [OutboxControllerTestApplication::class, FrontendPairingController::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "contacts.support-email=test@healthmetrix.com",
        "portal.host=http://error-redirect.fake",
        "portal.paths.error=/error",
    ],
)
@AutoConfigureMockMvc
class FrontendPairingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var sendSmsUseCase: SendSmsUseCase

    @MockBean
    private lateinit var mockCheckPinUseCase: CheckPinUseCase

    @MockBean
    private lateinit var mockSendPinUseCase: SendPinUseCase

    private val fakeToken = B64String("fakeToken").string.encodeURL()
    private val fakeEmail = "test@healthmetrix.com"
    private val fakeLang = "de-DE"

    @Nested
    @DisplayName("GET /frontend/send-sms")
    inner class GetSendSmsEndpointTest {
        @Test
        fun `it renders send-sms template with last 4 phone number digits and token as model attributes`() {
            whenever(sendSmsUseCase(any())).thenReturn(SendSmsUseCase.Result.Success("1111"))

            mockMvc.get("/frontend/send-sms") {
                param("token", fakeToken)
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.SEND_SMS_ROUTE) }
                model {
                    attribute("token", fakeToken)
                    attribute("phoneLast4", "1111")
                    attribute("lang", fakeLang)
                }
            }
        }

        @Test
        fun `it renders pin check error route when unable to parse phone number fails`() {
            whenever(sendSmsUseCase(any())).thenReturn(SendSmsUseCase.Result.InvalidToken)

            val response = mockMvc.get("/frontend/send-sms") {
                param("token", fakeToken)
                param("lang", fakeLang)
            }.andExpect {
                view { name("pin-error") }
                model {
                    attribute("lang", fakeLang)
                }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { queryParams ->
                assertThat(queryParams["lang"]).isEqualTo(fakeLang)
                assertThat(queryParams["correlationId"]).isNotNull()
                assertThat(queryParams["emailContact"]).isEqualTo(fakeEmail)
            }
        }
    }

    @Nested
    @DisplayName("GET /frontend/pin-check")
    inner class GetPinCheckEndpointTest {
        @Test
        fun `it renders pin check route on pin-check`() {
            mockMvc.get("/frontend/pin-check") {
                param("token", fakeToken)
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
                model { attribute("checkPin", CheckPinAttributes(pin = "", token = fakeToken, lang = fakeLang)) }
            }
        }
    }

    @Nested
    @DisplayName("POST /frontend/pin-check")
    inner class PostCheckPinEndpointTest {
        @Test
        fun `checking a valid pin returns success and location`() {
            whenever(mockCheckPinUseCase.invoke(any(), any(), any()))
                .thenReturn(Ok("oauth-start-url"))

            mockMvc.post("/frontend/pin-check") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", "fakeToken")
                param("pin", "1234")
                param("lang", fakeLang)
            }.andExpect {
                status { isOk() }
                view { name(IframeRoutes.SUCCESS_ROUTE) }
                model { attribute("redirectUrl", "oauth-start-url") }
            }
        }

        @Test
        fun `invalid token result redirects to pin-check page with error message`() {
            whenever(mockCheckPinUseCase.invoke(any(), any(), any()))
                .thenReturn(Err(CheckPinUseCase.CheckPinError.MALFORMED_INVITATION_TOKEN))

            mockMvc.post("/frontend/pin-check") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", "fakeToken")
                param("pin", "1234")
                param("lang", fakeLang)
            }.andExpect {
                status { isOk() }
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
                model { attributeExists("tokenError") }
            }
        }

        @Test
        fun `invalid case id result redirects to pin-check page with error message`() {
            whenever(mockCheckPinUseCase.invoke(any(), any(), any()))
                .thenReturn(Err(CheckPinUseCase.CheckPinError.INVALID_CASE_ID))

            mockMvc.post("/frontend/pin-check") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", "fakeToken")
                param("pin", "1234")
                param("lang", fakeLang)
            }.andExpect {
                status { isOk() }
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
                model { attributeExists("tokenError") }
            }
        }

        @Test
        fun `pin not sent result redirects to unrecoverable error page`() {
            whenever(mockCheckPinUseCase.invoke(any(), any(), any()))
                .thenReturn(Err(CheckPinUseCase.CheckPinError.PIN_NOT_SENT))

            val response = mockMvc.post("/frontend/pin-check") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", "fakeToken")
                param("pin", "1234")
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.UNRECOVERABLE_ERROR_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("pin_not_sent")
            }
        }

        @Test
        fun `invalid pin result redirects to pin-check with error message`() {
            whenever(mockCheckPinUseCase.invoke(any(), any(), any()))
                .thenReturn(Err(CheckPinUseCase.CheckPinError.INVALID_PIN))

            mockMvc.post("/frontend/pin-check") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", "fakeToken")
                param("pin", "1234")
                param("lang", fakeLang)
            }.andExpect {
                status { isOk() }
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
                model { attributeExists("pinError") }
            }
        }
    }

    @Nested
    @DisplayName("GET /frontend/pairing")
    inner class GetSendPinEndpointTest {
        private val fakeLang = "en-US"

        @Test
        fun `happy path redirects to check-pin-endpoint`() {
            whenever(mockSendPinUseCase.invoke(any(), any())).thenReturn(Ok(Unit))
            mockMvc.get("/frontend/pairing") {
                param("token", fakeToken)
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
                model {
                    attribute(
                        "checkPin",
                        CheckPinAttributes(pin = "", token = fakeToken, lang = fakeLang),
                    )
                    attribute("token", fakeToken)
                }
            }
        }

        @Test
        fun `invalid invitation token results in error-endpoint`() {
            whenever(mockSendPinUseCase.invoke(any(), any()))
                .thenReturn(Err(SendPinUseCase.SendPinError.MALFORMED_INVITATION_TOKEN))

            val response = mockMvc.get("/frontend/pairing") {
                param("token", fakeToken)
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.UNRECOVERABLE_ERROR_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("malformed_token")
            }
        }

        @Test
        fun `invalid case id results in error-endpoint`() {
            whenever(mockSendPinUseCase.invoke(any(), any())).thenReturn(Err(SendPinUseCase.SendPinError.INVALID_CASE_ID))
            val response = mockMvc.get("/frontend/pairing") {
                param("token", "blahblahblah")
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.UNRECOVERABLE_ERROR_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("invalid_case_id")
            }
        }

        @Test
        fun `failure to send sms results in error-endpoint`() {
            whenever(mockSendPinUseCase.invoke(any(), any())).thenReturn(Err(SendPinUseCase.SendPinError.SMS_ERROR))
            val response = mockMvc.get("/frontend/pairing") {
                param("token", "blahblahblah")
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.UNRECOVERABLE_ERROR_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("sms")
            }
        }

        @Test
        fun `sending pin from invalid status results in error-endpoint`() {
            whenever(
                mockSendPinUseCase.invoke(
                    any(),
                    any(),
                ),
            ).thenReturn(Err(SendPinUseCase.SendPinError.INVALID_STATUS))
            val response = mockMvc.get("/frontend/pairing") {
                param("token", "blahblahblah")
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.UNRECOVERABLE_ERROR_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("send_pin_invalid_status")
            }
        }

        @Test
        fun `sending pin from invalid status that is actually already completed`() {
            whenever(
                mockSendPinUseCase.invoke(
                    any(),
                    any(),
                ),
            ).thenReturn(Err(SendPinUseCase.SendPinError.ALREADY_PAIRED))
            val response = mockMvc.get("/frontend/pairing") {
                param("token", "blahblahblah")
                param("lang", fakeLang)
            }.andExpect {
                view { name(IframeRoutes.SUCCESS_ROUTE) }
            }.andReturn()

            withQueryParams(response.redirectUrl()) { query ->
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["kind"]).isEqualTo("already_paired")
            }
        }
    }

    private fun MvcResult.redirectUrl(): String =
        this.modelAndView!!.modelMap["redirectUrl"].toString()
}
