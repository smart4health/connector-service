package com.healthmetrix.connector.outbox.api.pairing

import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.array
import com.healthmetrix.connector.commons.decodeURL
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.outbox.api.OutboxApplication
import com.healthmetrix.connector.outbox.email.EmailService
import com.healthmetrix.connector.outbox.oauth.OauthClient
import com.healthmetrix.connector.outbox.sms.SmsService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.net.URL
import java.util.Locale

@SpringBootTest(
    classes = [OutboxApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "portal.host=http://portal.fake/",
        "portal.paths.pairing-success=/success",
        "default-locale=en-US",
        "invitation-mail.templates.en=123",
        "invitation-mail.templates.de=321",
        "invitation-mail.registration-url=http://register.fake",
    ],
)
@AutoConfigureMockMvc
@DirtiesContext
class PairingTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var mockEmailService: EmailService

    @MockBean
    private lateinit var mockSmsService: SmsService

    @MockBean
    private lateinit var mockOauthClient: OauthClient

    @Nested
    inner class PairingFlow {

        @Test
        fun `a pin can be successfully verified`() {
            val internalCaseId = InternalCaseId.randomUUID()
            val emailAddress = "email"
            val mobile = "mobile"
            val pubKey = "pubKey"

            // 1. addCase
            whenever(mockEmailService.sendEmail(any())).thenReturn(true)

            val requestContent = json {
                "email" to emailAddress
                "mobileNumber" to mobile
            }

            val requestWrapper = json {
                "content" to requestContent.toString().toByteArray()
                "contentType" to MediaType.APPLICATION_JSON_VALUE
                "publicKey" to pubKey
            }

            mockMvc.put("/v1/internal/cases/$internalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.toString()
            }.andExpect {
                status { isCreated() }
            }

            var redirectUrl: URL? = null
            verify(mockEmailService).sendEmail(
                check { email ->
                    assertThat(email.destAddress).isEqualTo(emailAddress)
                    redirectUrl = URL(email.template.asJson().get("redirect_url") as String)
                    assertThat(email.template.asJson().get("registration_url") as String).isEqualTo("http://register.fake")
                },
            )

            val queryParams = redirectUrl!!.query.split("&").toList().map {
                val params = it.split("=")
                params[0] to params[1]
            }.toMap()

            val encryptedToken =
                queryParams["token"] as String // defaults to de-DE when no lang is provided in inbox AddCase request
            val lang = queryParams["lang"] as String

            // 2. Get the PIN
            whenever(mockSmsService.sendSms(any())).thenReturn(Ok(Unit))

            mockMvc.get("/frontend/send-sms") {
                param("token", encryptedToken.decodeURL())
                param("lang", lang)
            }.andExpect {
                view { name(IframeRoutes.SEND_SMS_ROUTE) }
            }

            mockMvc.get("/frontend/pairing") {
                param("token", encryptedToken.decodeURL())
                param("lang", lang)
            }.andExpect {
                view { name(IframeRoutes.PIN_CHECK_ROUTE) }
            }

            var pin = ""
            verify(mockSmsService).sendSms(
                check { sms ->
                    assertThat(sms.destNumber).isEqualTo(mobile)
                    pin = Regex("[0-9]+").findAll(sms.text).first().value
                },
            )

            var parsedState = ""
            var parsedPubKey = ""
            // 3. submit the pin

            whenever(mockOauthClient.buildAuthorizationUrl(any(), any())).thenAnswer { invocationOnMock ->
                parsedState = invocationOnMock.arguments[0].toString()
                parsedPubKey = invocationOnMock.arguments[1].toString()
                "$parsedState:$parsedPubKey"
            }

            mockMvc
                .post("/frontend/pin-check") {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                    param("token", encryptedToken.decodeURL())
                    param("pin", pin)
                    param("lang", lang)
                }.andExpect {
                    status { isOk() }
                    view().name("pin-success")
                    model().attribute("redirectUrl", "$parsedState:$parsedPubKey")
                }

            // 4. pretend oauth succeeded
            whenever(mockOauthClient.getRefreshToken(any()))
                .thenReturn("fake refresh token")

            mockMvc.get("/v1/api/pairing/success") {
                param("state", parsedState)
                param("code", "hello")
            }.andExpect {
                status { isEqualTo(HttpStatus.MOVED_PERMANENTLY.value()) }
                header {
                    string(
                        HttpHeaders.LOCATION,
                        "http://portal.fake/en/success?lang=${Locale.US.toLanguageTag()}",
                    )
                }
            }

            // 5. retrieving the refresh token results in correct values
            mockMvc.get("/v1/internal/pairing/refreshtokens").andExpect {
                status { isOk() }
                content {
                    json(
                        array {
                            json {
                                "internalCaseId" to internalCaseId
                                "refreshToken" to "fake refresh token"
                            }
                        }.toString(),
                    )
                }
            }

            // 6. deleting auth codes results a single array element
            mockMvc.delete("/v1/internal/pairing/refreshtokens/$internalCaseId").andExpect {
                status { isOk() }
            }

            // 7. retrieving auth codes again results in an empty array
            mockMvc.get("/v1/internal/pairing/refreshtokens").andExpect {
                status { isOk() }
                content {
                    json(array {}.toString())
                }
            }
        }
    }

    @Nested
    inner class AddCaseEncryptionKeyIntegrationTest {

        @Test
        fun `addCase encryption key can be fetched`() {
            // matches the pub key specified in mock secrets
            val expectedJwk = json {
                "alg" to "RSA-OAEP-256"
                "e" to "AQAB"
                "kid" to "WiPqKBUtTMf7FjZHproVJlJ8Mn4TsKoqwh19mPjmYIM"
                "kty" to "RSA"
                "n" to "w0sN0yRxS2gbbvTys0Hzci2hbpXv2-IAzlotQfDsESS2sW55hKpl8gRbGi6Wmv3lcu3irgxyl91zmAWUx5sqoxSwBdx9vSAXZAv0GB2qxwCHXzbNQuWABVf3jHo_pIxe2CeJOYUVGQBHkT-05EOiF_fQzTy4yf9lYTlHELSlbLxU69R-D3S35WgAWaQYPBcVfZJIcz9IU2uRMhaXlSElcEnrdPyBClVOhN-fijpfki_san7K1E40ohX5UE3fo5KK8j1u2br4nwMqE2QaDjG8T9OryyqoekkSkG3_Yamr3PN3hwvY1Ac-Z9h6SXmAGjsN0jkpDZjdkEF0GXc37FkmNw"
                "use" to "enc"
            }

            mockMvc.get("/v1/api/pairing/publickey")
                .andExpect {
                    status { is2xxSuccessful() }
                    content {
                        json(expectedJwk.toString())
                    }
                }
        }
    }
}
