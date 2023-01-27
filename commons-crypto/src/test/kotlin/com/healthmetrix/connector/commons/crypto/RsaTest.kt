package com.healthmetrix.connector.commons.crypto

import com.healthmetrix.connector.commons.decodeBase64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.interfaces.RSAPublicKey

private const val ADD_CASE_ENCRYPTION_KEY_B64 =
    "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUF3MHNOMHlSeFMyZ2JidlR5czBIegpjaTJoYnBYdjIrSUF6bG90UWZEc0VTUzJzVzU1aEtwbDhnUmJHaTZXbXYzbGN1M2lyZ3h5bDkxem1BV1V4NXNxCm94U3dCZHg5dlNBWFpBdjBHQjJxeHdDSFh6Yk5RdVdBQlZmM2pIby9wSXhlMkNlSk9ZVVZHUUJIa1QrMDVFT2kKRi9mUXpUeTR5ZjlsWVRsSEVMU2xiTHhVNjlSK0QzUzM1V2dBV2FRWVBCY1ZmWkpJY3o5SVUydVJNaGFYbFNFbApjRW5yZFB5QkNsVk9oTitmaWpwZmtpL3NhbjdLMUU0MG9oWDVVRTNmbzVLSzhqMXUyYnI0bndNcUUyUWFEakc4ClQ5T3J5eXFvZWtrU2tHMy9ZYW1yM1BOM2h3dlkxQWMrWjloNlNYbUFHanNOMGprcERaamRrRUYwR1hjMzdGa20KTndJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg=="
private const val ADD_CASE_DECRYPTION_KEY_B64 =
    "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcEFJQkFBS0NBUUVBdzBzTjB5UnhTMmdiYnZUeXMwSHpjaTJoYnBYdjIrSUF6bG90UWZEc0VTUzJzVzU1CmhLcGw4Z1JiR2k2V212M2xjdTNpcmd4eWw5MXptQVdVeDVzcW94U3dCZHg5dlNBWFpBdjBHQjJxeHdDSFh6Yk4KUXVXQUJWZjNqSG8vcEl4ZTJDZUpPWVVWR1FCSGtUKzA1RU9pRi9mUXpUeTR5ZjlsWVRsSEVMU2xiTHhVNjlSKwpEM1MzNVdnQVdhUVlQQmNWZlpKSWN6OUlVMnVSTWhhWGxTRWxjRW5yZFB5QkNsVk9oTitmaWpwZmtpL3NhbjdLCjFFNDBvaFg1VUUzZm81S0s4ajF1MmJyNG53TXFFMlFhRGpHOFQ5T3J5eXFvZWtrU2tHMy9ZYW1yM1BOM2h3dlkKMUFjK1o5aDZTWG1BR2pzTjBqa3BEWmpka0VGMEdYYzM3RmttTndJREFRQUJBb0lCQVFDd1RXVmhvNWlUMXB4TgpndHhIYjlaeTBUYlhPb3liR0dCWjZaYkozTkdBZUlCbGxiSW1UaDVTYWhSRWdxSjdzWkllT3h0VXZQQUdvV2psCmFNUnpubVRUYksycjNPMjJldTRpNTVlbzNiOTZmOE8xOVNkQXFTYjFyQUJTMVZuM0ZySFl1WGhzY1BRbTZkV3kKRHEwakZOdVVmNmdFWURrQ1Fvb25SeW9jcnJoWWUrakJ6YU4vdVRrdDAxblJPSTN2bTF2NmNtZlhIdXBmMVc4YQpNZCtzY2lpbTI3UDhWZFJsRjFFbG1tNFZLZDM1bWRzUTBwMVhDbTI1cFhWNjNNSWRFWW1McmJCaDhzdHRMVldtClRndkNCK2U3b0NUdXFYazg4bnh4ald1MEtZSWVuVDFTOUliaFZhVHFFZC80Q2xVTUtPVENiQ1l2SWRHdVVBNi8KV2o2eEhhNEJBb0dCQU9mK0ErMGdGb2U2TTMvS0ZZL3VOTDVqU1U0MEpkeVpOVWZLV3R3OVZ3bDNhZkRQRzlBUApQUDF0YmtNMUV2ejlwQk1ocU9zMFBjUXVtSEU1bTl6M0NJZzRkOTNoSUZGbG4zUEcreGFQdy85UTVRdGhNc3ZFCnEzZEpaVW5VdFNSaTZUMU9rK2pEMEZXbUs0Wmxld0hvdUZKN3JGUytYaDRnZ1RIZUxwaXZoRXozQW9HQkFOZUEKekxmYXU5OGFZU3piai8xczhqSXRZZkZjYUNYS3VYd1JHbnZxd25SWGJROW1YaERuekVxSEpqcVJHNzJhdWRVdwpNdnJ5ZDc4UUhNUndqRUlEUUxmUDZXa1liODkzQ1dNOVkyb1BSMzltT2NyRmR0bWVRRlFrWkp1NGNjc1pETGhUCkZIWTFUWW5CVVQ4OWxqVm5PY3JyQjYxNVRnWnpNQkFLbkJzcUZlREJBb0dBRDBqL1BUcG1BWjlWRVZCaFIxQnMKalRiQmQ3T2I0d0w5TGJPNWROVmR6TFBmZVF4TVN0TVdNNlJvSldsenpOTnhZZ2xQdGQrRlNrMi9vWVlvTE5EaAo4UWUzYmhrTkpnL0tCN2pPaGxnR2srWGlrWE5nQTJqNzJ5b3MwRWFCZ05vN2Y5eVRoanlRbDNRUlhoT0ZuVVNXCkVHa3htNHZIYTdpOGltcVhLMXcxSFZVQ2dZRUFrRW9FeC9VRVpWVGNTNVNXekMvdDJmclk0U25sOGFmU21XYzYKUEUzcTlNcTBrdU1QaUhJckxwdGUxVWZqTXdndDlMZlk2bnorQUVkaGU0Vi80NU1aK2ZpVFozS2RLbU9oUFhrVQozeVpyMExrNWFMTGQ2TWMwZXlJQzJ1Q2NFRWd0WkJ6OFRrbFNLVEh1bkZFNENYbWNFR2xkTXFGTnhMUExNbkpvCkl5Z0NqZ0VDZ1lCTlNkU3A0UXlIYVJhMVJpdndUZllOT2o0VkJoeTQ2VTZxaGVKR3EzU2dYZTJtNEVJaVB6ai8KU2cxZ1pmSktUQjk1RDJDTFJYNU9yUmQvdnVBeUxNWG9lc0k4b3BKV1pLcXR5dGdSZGVmbWNEOTh6NjBTMm9YVQpwZ1lXTERXM0FkYk9ib1ZJVnNqS0p5QTFhZDdCckMxTS9DZzAxR2xoOHlIV094WGswUFlrNVE9PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo="

internal class RsaTest {

    private val invalidB64 = "aW52YWxpZAo="

    @Nested
    @DisplayName("Public Key deserialization")
    inner class PublicKey {
        @Test
        fun `buildFromB64String should return instance of RsaPublicKey for valid input`() {
            assertThat(RsaPublicKey.buildFromB64String(ADD_CASE_ENCRYPTION_KEY_B64)).isInstanceOf(RSAPublicKey::class.java)
        }

        @Test
        fun `buildFromB64String should throw Exception for non b64 input`() {
            assertThrows<java.lang.NullPointerException> { RsaPublicKey.buildFromB64String("invalid") }
        }

        @Test
        fun `buildFromB64String should throw Exception for invalid b64String as input`() {
            assertThrows<java.lang.NullPointerException> { RsaPublicKey.buildFromB64String(invalidB64) }
        }
    }

    @Nested
    @DisplayName("Private Key deserialization")
    inner class PrivateKey {
        @Test
        fun `buildFromB64String should return instance of RsaPrivateKey for valid input`() {
            assertThat(RsaPrivateKey.buildFromB64String(ADD_CASE_DECRYPTION_KEY_B64)).isInstanceOf(RsaPrivateKey::class.java)
        }

        @Test
        fun `buildFromB64String should throw Exception for non b64 input`() {
            assertThrows<NullPointerException> { RsaPrivateKey.buildFromB64String("invalid") }
        }

        @Test
        fun `buildFromB64String should throw Exception for invalid b64String as input`() {
            assertThrows<NullPointerException> { RsaPrivateKey.buildFromB64String(invalidB64) }
        }

        @Test
        fun `toPemKeyString should make a string that looks approximately correct`() {
            val key = ADD_CASE_DECRYPTION_KEY_B64.decodeBase64()!!

            val result = RsaPrivateKey.toPemKeyString(key)

            assertThat(result).startsWith("-----BEGIN RSA PRIVATE KEY-----")
            assertThat(result).endsWith("-----END RSA PRIVATE KEY-----\n")
        }
    }

    @Nested
    @DisplayName("Encryption roundtrip")
    inner class EncryptionRoundtrip {
        @Test
        fun `encryption roundtrip should work`() {
            val publicKey = RsaPublicKey(RsaPublicKey.buildFromB64String(ADD_CASE_ENCRYPTION_KEY_B64)!!)
            val privateKey = RsaPrivateKey.buildFromB64String(ADD_CASE_DECRYPTION_KEY_B64)!!

            val expected = "test"
            val encrypted = publicKey.encrypt(expected.toByteArray(Charsets.UTF_8))!!

            val result = privateKey.decrypt(encrypted)?.toString(Charsets.UTF_8)

            assertThat(result).isEqualTo(expected)
        }
    }
}
