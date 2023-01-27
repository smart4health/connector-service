package com.healthmetrix.connector.inbox.api.query

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.onFailure
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.web.ApiResponse
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.inbox.api.DecommissionedApiResponse
import com.healthmetrix.connector.inbox.pairing.QueryStatusUseCase
import com.healthmetrix.connector.inbox.query.FindPatientByInternalCaseIdUseCase
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryController(
    private val queryStatusUseCase: QueryStatusUseCase,
    private val findPatientByInternalCaseIdUseCase: FindPatientByInternalCaseIdUseCase,
    @Value("\${query.bulk-limit}")
    private val bulkQueryLimit: Int,
) {

    @GetMapping(
        path = [
            "/v1/his/patients/{_patientId}/cases/{_externalCaseId}/status",
            "/v2/his/patients/{_patientId}/cases/{_externalCaseId}/status",
        ],
    )
    fun oldQueryCaseStatus(
        @PathVariable _patientId: String,
        @PathVariable _externalCaseId: ExternalCaseId,
    ): ResponseEntity<DecommissionedApiResponse> = DecommissionedApiResponse.asEntity()

    @GetMapping("/v3/his/patients/{patientId}/cases/{externalCaseId}/status")
    fun queryCaseStatus(
        @PathVariable patientId: String,
        @PathVariable externalCaseId: ExternalCaseId,
    ): ResponseEntity<QueryStatusResponse> {
        logger.debug("queryStatus for {}", StructuredArguments.kv("externalCaseId", externalCaseId))
        return when (queryStatusUseCase(externalCaseId)) {
            QueryStatusUseCase.Result.NO_CASE_ID -> QueryStatusResponse.CaseNotFound
            QueryStatusUseCase.Result.NOT_PAIRED -> QueryStatusResponse.CaseFound(false)
            QueryStatusUseCase.Result.PAIRED -> QueryStatusResponse.CaseFound(true)
        }.asEntity()
    }

    @PostMapping("/v3/his/patients/status")
    fun queryBatchCaseStatus(
        @RequestBody externalCaseIds: List<ExternalCaseId>,
    ): ResponseEntity<BulkStatusQueryResponse> {
        if (externalCaseIds.size > bulkQueryLimit) {
            return BulkStatusQueryResponse.TooManyExternalCaseIds(bulkQueryLimit).asEntity()
        }

        val (statuses, sums) = queryStatusUseCase(externalCaseIds).let { (statuses, sums) ->
            statuses.mapValues {
                BulkStatusQueryStatus.from(it.value)
            } to sums.mapKeys {
                BulkStatusQueryStatus.from(it.key)
            }
        }

        return BulkStatusQueryResponse.Ok(
            statuses,
            BulkStatusQueryResponse.Ok.Statistics(
                sums,
                statuses.count(),
            ),
        ).asEntity()
    }

    @GetMapping("/v3/his/patient")
    fun findPatientByInternalCaseId(
        @RequestParam
        internalCaseId: String,
    ): ResponseEntity<FindPatientByInternalCaseIdResponse> = findPatientByInternalCaseIdUseCase(internalCaseId)
        .onFailure {
            logger.info(
                "Failed to find case {} {}",
                StructuredArguments.kv("internalCaseId", internalCaseId),
                StructuredArguments.kv("error", it.javaClass.simpleName),
            )
        }
        .map { FindPatientByInternalCaseIdResponse.Found(it) }
        .mapError { FindPatientByInternalCaseIdResponse.NotFound }
        .merge()
        .asEntity()

    sealed class FindPatientByInternalCaseIdResponse(
        status: HttpStatus,
        hasBody: Boolean,
    ) : ApiResponse(status, hasBody) {

        data class Found(
            @JsonProperty("external_case_id")
            val externalCaseId: ExternalCaseId,
        ) : FindPatientByInternalCaseIdResponse(HttpStatus.OK, true)

        object NotFound : FindPatientByInternalCaseIdResponse(HttpStatus.NOT_FOUND, false)
    }

    enum class BulkStatusQueryStatus {
        NOT_FOUND,
        NOT_PAIRED,
        PAIRED,
        ;

        companion object {
            fun from(result: QueryStatusUseCase.Result) = when (result) {
                QueryStatusUseCase.Result.NO_CASE_ID -> NOT_FOUND
                QueryStatusUseCase.Result.NOT_PAIRED -> NOT_PAIRED
                QueryStatusUseCase.Result.PAIRED -> PAIRED
            }
        }
    }

    sealed class BulkStatusQueryResponse(
        status: HttpStatus,
    ) : ApiResponse(status, true) {
        data class Ok(
            val statuses: Map<ExternalCaseId, BulkStatusQueryStatus>,
            val statistics: Statistics,
        ) : BulkStatusQueryResponse(HttpStatus.OK) {
            data class Statistics(
                @get:JsonAnyGetter
                val sums: Map<BulkStatusQueryStatus, Int>,
                @JsonProperty("TOTAL")
                val total: Int,
            )
        }

        class TooManyExternalCaseIds(
            @JsonIgnore
            val limit: Int,
        ) : BulkStatusQueryResponse(HttpStatus.BAD_REQUEST) {
            val message = "Too many IDs requested, limit is $limit"
        }
    }
}
