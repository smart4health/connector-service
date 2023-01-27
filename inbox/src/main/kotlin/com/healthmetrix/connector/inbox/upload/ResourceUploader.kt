package com.healthmetrix.connector.inbox.upload

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.d4l.DomainResourceUploader
import net.logstash.logback.argument.StructuredArguments.kv
import org.hl7.fhir.r4.model.DomainResource
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResourceUploader(
    private val fhirContext: FhirContext,
    private val resourceUploader: DomainResourceUploader,
) {

    operator fun invoke(
        json: String,
        accessToken: String,
        privateKeyPemString: String,
        internalResourceId: UUID,
    ): com.github.michaelbull.result.Result<UUID, Pair<UUID, Throwable>> {
        val reference = try {
            fhirContext.newJsonParser().parseResource(json) as DomainResource
        } catch (ex: DataFormatException) {
            logger.warn(
                "Error reading resource from cache {}",
                kv("internalResourceId", internalResourceId),
                ex,
            )
            return Err(internalResourceId to ex)
        } catch (ex: ClassCastException) {
            logger.warn(
                "Resource is not a DomainResource {}",
                kv("internalResourceId", internalResourceId),
                ex,
            )
            return Err(internalResourceId to ex)
        }

        return try {
            resourceUploader.uploadDocument(reference, accessToken, privateKeyPemString)
            Ok(internalResourceId)
        } catch (ex: Throwable) {
            logger.warn("Error uploading document", ex)
            Err(internalResourceId to ex)
        }
    }
}
