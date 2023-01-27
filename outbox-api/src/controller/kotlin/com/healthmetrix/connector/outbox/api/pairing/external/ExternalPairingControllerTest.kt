package com.healthmetrix.connector.outbox.api.pairing.external

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.api.pairing.util.OutboxControllerTestApplication
import com.healthmetrix.connector.outbox.api.pairing.util.withQueryParams
import com.healthmetrix.connector.outbox.usecases.OauthErrorUseCase
import com.healthmetrix.connector.outbox.usecases.OauthSuccessUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import com.nimbusds.jose.jwk.RSAKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.Locale

@SpringBootTest(
    classes = [OutboxControllerTestApplication::class, ExternalPairingController::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "portal.host=http://oauth-success.fake",
        "portal.paths.pairing-success=/success",
        "portal.paths.error=/error",
        "default-locale=de-DE",
        "contacts.support-email=test@healthmetrix.com",
    ],
)
@AutoConfigureMockMvc
class ExternalPairingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var mockOauthSuccessUseCase: OauthSuccessUseCase

    @MockBean(name = "addCaseEncryptionKey")
    private lateinit var mockAddCaseEncryptionKey: LazySecret<RSAKey>

    @MockBean
    private lateinit var mockOauthErrorUseCase: OauthErrorUseCase

    @Nested
    @DisplayName("GET /v1/api/pairing/success")
    inner class GetOauthSuccessEndpointTest {
        private val fakeLang = "de-DE"
        private val fakeEmail = "test@healthmetrix.com"

        @Test
        fun `calling the success endpoint with an old or invalid case status results in an error endpoint`() {
            whenever(mockOauthSuccessUseCase.invoke(any(), any()))
                .thenReturn(OauthSuccessUseCase.Result.InvalidCaseStatus)
            val response = mockMvc.get("/v1/api/pairing/success") {
                param("state", "maryland")
                param("code", "123456")
            }.andExpect {
                status { isEqualTo(HttpStatus.MOVED_PERMANENTLY.value()) }
            }.andReturn()

            assertThat(response.response.redirectedUrl).startsWith("http://oauth-success.fake/de/error")
            withQueryParams(response.response.redirectedUrl!!) { query ->
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("oauth_invalid_status")
            }
        }

        @Test
        fun `calling the success endpoint with an error response results in redirection to the error endpoint`() {
            whenever(mockOauthErrorUseCase.invoke(any()))
                .thenReturn(OauthErrorUseCase.Result.Success(Bcp47LanguageTag(Locale.US)))
            val response = mockMvc.get("/v1/api/pairing/success") {
                param("state", "maryland")
                param("error", "some error")
            }.andExpect {
                status { isEqualTo(HttpStatus.MOVED_PERMANENTLY.value()) }
            }.andReturn()

            assertThat(response.response.redirectedUrl).startsWith("http://oauth-success.fake/en/error")
            withQueryParams(response.response.redirectedUrl!!) { query ->
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["lang"]).isEqualTo("en-US")
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("oauth_error")
            }
        }

        @Test
        fun `calling the success endpoint with an error response without a decodable lang redirects to error endpoint with default lang`() {
            whenever(mockOauthErrorUseCase.invoke(any()))
                .thenReturn(OauthErrorUseCase.Result.OauthStateNotFound)
            val response = mockMvc.get("/v1/api/pairing/success") {
                param("state", "maryland")
                param("error", "some error")
            }.andExpect {
                status { isEqualTo(HttpStatus.MOVED_PERMANENTLY.value()) }
            }.andReturn()

            assertThat(response.response.redirectedUrl).startsWith("http://oauth-success.fake/de/error")
            withQueryParams(response.response.redirectedUrl!!) { query ->
                assertThat(query["correlationId"]).isNotNull()
                assertThat(query["lang"]).isEqualTo(fakeLang)
                assertThat(query["emailContact"]).isEqualTo(fakeEmail)
                assertThat(query["kind"]).isEqualTo("oauth_error")
            }
        }

        @Test
        fun `calling the success endpoint with a valid case status results in success`() {
            whenever(mockOauthSuccessUseCase.invoke(any(), any()))
                .thenReturn(OauthSuccessUseCase.Result.Success(Bcp47LanguageTag(Locale.US)))
            mockMvc.get("/v1/api/pairing/success") {
                param("state", "maryland")
                param("code", "123456")
            }.andExpect {
                status { isEqualTo(HttpStatus.MOVED_PERMANENTLY.value()) }
                header { string(HttpHeaders.LOCATION, "http://oauth-success.fake/en/success?lang=en-US") }
            }
        }
    }

    @Nested
    @DisplayName("GET /v1/api/pairing/publickey")
    inner class AddCaseRequestEncryptionEndpointTest {

        @Test
        fun `should return the public key in a JWK format`() {
            val jwk = json {
                "alg" to "RSA-OAEP-256"
                "e" to "AQAB"
                "kid" to "771wu1dtNLbjFFuZqyuU8a21-CrKTCnxNco7dXBLhmg"
                "kty" to "RSA"
                "n" to "5WbfWyW9zj6g_LeCoaluZT7k9Y962xA2JWV8w_Yde30kvzSiaizHEYhTzfxnRhgOsu_SADhtGLUnQSGbAr82b2qkO3ldSCCRIN_UZ2fSiig82ZPF5BZdvPsb5RE4__yQ27lbYezhn_KbntvtAU4IuV38CU_F8Jh5SPghNHcvDGf8LBQQJHqY1HMHIEjL4niL8hk1d0tnTxuWF7qIp7yxMecU1Bpiq8b1MXx1rU5A0t0IVlshE3_u-G7EajYo-t2IVJxLOzvfAqb79FZCA8SNL7iWkRe4qZdAtSg0-zZoXYGwRHnNaFX64jEN2qHUr7YtVYVcdPUz8X6rBSCGoJIoIQ"
                "use" to "enc"
            }

            whenever(mockAddCaseEncryptionKey.value).thenReturn(RSAKey.parse(jwk.toString()))

            mockMvc.get("/v1/api/pairing/publickey")
                .andExpect {
                    status { isOk() }
                    content {
                        json(jwk.toString())
                    }
                }
        }

        @Test
        fun `should return status code 500 if RSAPublicKey could not be loaded`() {
            whenever(mockAddCaseEncryptionKey.value).thenReturn(null)

            mockMvc.get("/v1/api/pairing/publickey")
                .andExpect {
                    status { isInternalServerError() }
                }
        }
    }
}
