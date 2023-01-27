package com.healthmetrix.connector.inbox.api.upload.v3

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import com.healthmetrix.connector.inbox.api.upload.FhirResourceNotReadableException
import com.healthmetrix.connector.inbox.api.upload.FhirResourceTypeMismatchException
import org.hl7.fhir.r4.model.DomainResource
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.stereotype.Component

@Component
class JsonDomainResourceHttpMessageConverter(
    private val fhirContext: FhirContext,
) : AbstractHttpMessageConverter<DomainResource>(
    MediaType("application", "fhir+json"),
) {
    override fun supports(clazz: Class<*>): Boolean {
        return DomainResource::class.java.isAssignableFrom(clazz)
    }

    override fun readInternal(clazz: Class<out DomainResource>, inputMessage: HttpInputMessage): DomainResource {
        return try {
            fhirContext.newJsonParser().parseResource(inputMessage.body) as DomainResource
        } catch (ex: DataFormatException) {
            throw FhirResourceNotReadableException(ex, inputMessage)
        } catch (ex: ClassCastException) {
            throw FhirResourceTypeMismatchException(ex, inputMessage)
        }
    }

    override fun writeInternal(domainResource: DomainResource, outputMessage: HttpOutputMessage) {
        throw NotImplementedError("DomainResource serialization not supported")
    }
}

@Component
class XmlDomainResourceHttpMessageConverter(
    private val fhirContext: FhirContext,
) : AbstractHttpMessageConverter<DomainResource>(
    MediaType("application", "fhir+xml"),
) {
    override fun supports(clazz: Class<*>): Boolean {
        return DomainResource::class.java.isAssignableFrom(clazz)
    }

    override fun readInternal(clazz: Class<out DomainResource>, inputMessage: HttpInputMessage): DomainResource {
        return try {
            fhirContext.newXmlParser().parseResource(inputMessage.body) as DomainResource
        } catch (ex: DataFormatException) {
            throw FhirResourceNotReadableException(ex, inputMessage)
        } catch (ex: ClassCastException) {
            throw FhirResourceTypeMismatchException(ex, inputMessage)
        }
    }

    override fun writeInternal(domainResource: DomainResource, outputMessage: HttpOutputMessage) {
        throw NotImplementedError("DomainResource serialization not supported")
    }
}
