# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [4.2.0] - 14 Nov 2023

### Changed

- Update Kotlin version to 1.7.10
- Update Data4Life SDK libraries to newest versions
- Update other dependencies

### Fixed

- Pin potentially vulnerable transitive dependencies to non-vulnerable versions

## [4.1.1] - 10 Nov 2021

### Fixed

- Allow ingesting Provenance resources while Provenance auto-generation is switched on.

## [4.1.0] - 22 Sep 2021

### Added

- Expose method for creating and linking Provenance instances for other resources

### Changed

- Make `LatchedCallback` class private
- README updated to document new features
- Update Data4Life SDK version to 1.15.1
- Update Data4Life Java FHIR classes to 1.6.3

### Fixed

- Fix inability to generate Provenance for resources of the types AdverseEvent, ActivityDefinition, and List.

## [4.0.0] - 31 Aug 2021

### Added

- Ability to add and upload Provenance resources for ingested resources to document the ingestion (on by default)
- Configuration options for Provenance creation:
  1. Whether a Provenance instance should be created and uploaded for each ingested resource
  2. Whether it is considered an overall failure of the ingested resource is uploaded OK, but the Provenance resource
     fails to upload

### Changed

- **Breaking:** the private key and access token parameters in `uploadResource` now have the type `ByteArray`
- **Breaking:** OAuth client ID must now be passed to the ingestion engine factory functions
- Default upload call timeout (value assigned for newly created ingestion engine instances) can now be set in the
  configuration
- README updated to document new features
- Upgrade HAPI dependencies to version 5.5.0
- Upgrade core FHIR utilities to version 5.4.11
- Upgrade SLF4J to version 1.7.32
- Update Data4Life SDK version to 1.14.0
- Update Data4Life Java FHIR classes to 1.6.2
- Upgrade ThreeTen to version 1.5.1 (version used by new SDK version)
- Add Data4Life SDK error and util JARs as dependencies (split out from other SDK modules)
- Update Kotest to version 4.6.2
- Update mockk to version 1.12.0

## [3.0.3] - 09 June 2021

### Changed

- Update D4L SDK to version 1.13.2
- Update D4L FHIR classes to version 1.5.0

### Fixed

- Certificate issue should now be fixed (fixed by SDK updates)

## [3.0.2] - 21 May 2021

### Removed

- com.github.johnrengelman.shadow Gradle plugin (to ensure publishing of slim JAR)

## [3.0.1] - 07 May 2021

### Changed

- Update D4L SDK to version 1.11.0
- Update D4L FHIR classes to version 1.4.0
- Update bouncycastle to version 1.64 (version used in current SDK)
- Update mockk to version 1.11.0
- Update kotlin Gradle plugin to version 1.4.32
- Update nu.studer.credentials Gradle plugin to version 2.1
- Update com.github.johnrengelman.shadow Gradle plugin to version 6.1.0
- Update Gradle to version 6.8.3

### Fixed

- MedicationStatement could not be ingested for certain values in the status field (fixed by SDK updates)

## [3.0.0] - 29 Apr 2021

### Added

- Validation rule for banning the use of the `implicitRules` element in FHIR resources (on by default)
- Validation resources can now be loaded from FHIR packages when initializing ingestion engine
- Added a generic configuration class
- Added the possibility of reading configuration from a config file (JSON format, must be called *
  ingestion-config.json*) - is now required when loading validation resources from a folder or from FHIR packages.

### Changed

- Only the public API is now visible to library consumers
- Update HAPI FHIR to version 5.3
- Update D4L SDK to version 1.9.2
- Update D4L FHIR classes to version 1.3.1
- Initializing an ingestion engine from resources in a directory no longer possible without a configuration file
- New configuration allows specifying custom "trigger codes" for Observation resources (for running validation against
  specific profiles for Observation with a particular code in the `code` element)
- Update Jenkins pipeline library to version 2.12.0

### Fixed

- Do not generate dokka JavaDoc pages for empty packages

## [2.0.1] - 24 Jan 2021

### Changed

* Update Jenkins library

## [2.0.0] - 22 Jan 2021

### Added

- Improved validation configuration (R4 engine only)
- Resources containing extensions are no longer rejected by default (R4 engine only)
- Validation against custom value sets (R4 engine only)
- Validation against custom code systems (R4 engine only)
- Validation of QuestionnaireResponses against Questionnaires (R4 engine only)
- Factory functions for ingestion engine creation  (R4 engine only)
- Ability to load FHIR artefact directly from file system when initiation ingestion engine (R4 engine only)
- For Observation resources, set of FHIR profiles to check against can depend on Observation.code & can include profiles
  from the core FHIR specification (R4 engine only)

### Changed

- Switched to using R4 SDK (SDK v1.9.1 and SDK FHIR library v1.2.1)
- Pull SDK dependencies from package repositories rather than from local JARs
- Text of OperationOutcome resources generated for errors is now always put in the "diagnostics" field
- Significant README update
- Use kotest v4.3.1 instead of KotlinTest (library was renamed)
- Update mockk dependency to v1.10.4

### Fixed

- Fixed: incorrect handling of Attachment encoding

## [1.0.2] - 16 September 2020

### Added

- Attachment hash validation

## [1.0.1] - 15 September 2020

### Changed

- clean up

## [1.0.0] - 09 March 2020

### Added
- Make call timeout customizable
- Extend logging
- Add more specific exception classes added
- Throw exception in `uploadDocument()` method if (1) upload call fails, (2) upload call hits internal timeout, or (3) resource to be uploaded is invalid

### Changed
- *Breaking change*: Reject invalid resources in `uploadDocument()`
- *Breaking change*: `environment` parameter in `FhirStu3IngestionEngine` constructor is now of class `care.data4life.ingestion.Environment` (possible values same as before)
- *Breaking change*: Limit public API to
    1. `FhirStu3IngestionEngine` with methods `uploadDocument()` and `validate()`, plus public property `callTimeout`
    2. The `containsSeriousIssue()` helper function
    3. Types taken, returned, or throws by the above

### Removed
- Remove stub of `getOAuthAuthorizeBaseURL()` method

### Fixed
- Fixed: `uploadDocument()` would block until internal timeout even if successful

## [0.1.0] - 21 Feb 2020

### Added
- Improved and extended logging

### Changed
- update HAPI FHIR to 4.2.0

### Removed
- Removed extended HTTP request logging to fix related security concerns

## [0.0.1] - 11 Feb 2020

### Added
- Uploading FHIR resources to the D4L PHDP
