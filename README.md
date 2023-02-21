# Smart4Health Connector

## Acknowledgements

<img src="./img/eu.jpg" align="left" alt="European Flag" width="60">

This project has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 826117.

## About

The Smart4Health Connector is an application that will connect a patient's hospital
data to an eHR of his or her choice. In order to fulfill this while guaranteeing maximum data privacy and security
concerns, the application consists of two components split up on the hospital's internal network and the DMZ.

See the full documentation document here: https://zenodo.org/record/6782461

## Setup requirements

### Ingestion library JAR

The ingestion library jar file should be obtained from project partners and placed here:
`dit-ingestion/libs/dil-v4.2.0.jar`.

Also, it will expect FHIR validation definitions in the fhir-package directory. See the README.md file located there to
obtain those.

### Ingestion library dependencies

D4L ingestion libraries now exist on GitHub Packages. To use this,
you will need a GitHub account and a personal access token with
permission to download packages.

Then, create a (`.gitignore`d) `secrets.properties` file in the root
project, and fill it with

```properties
gpr.user=<USERNAME>
gpr.token=<TOKEN>
```

Building should then work as expected.

> Note: Authentication errors may be represented by `400 Bad Request`

### FHIR validation packages

The following FHIR definitions should be obtained from simplifier.net and placed into the directory `fhir-package`

- smart4health.eu.core-0.5.2.tgz: downlod from https://simplifier.net/packages/smart4health.eu.core/0.5.2
- hl7.fhir.us.core-4.0.0.tgz: downlod from https://simplifier.net/packages/hl7.fhir.us.core/4.0.0: extract,
  remove `us.nlm.vsac` from package.json and compress back to .tgz
- hl7.fhir.uv.bulkdata-1.0.1.tgz: downlod from https://simplifier.net/packages/hl7.fhir.uv.bulkdata/1.0.1
- hl7.fhir.uv.ips-1.0.0.tgz: downlod from https://simplifier.net/packages/hl7.fhir.uv.ips/1.0.0

## Inbox

Exists inside the hospital intranet

To start the Inbox API locally, run the following command:

```shell script
./gradlew inbox-api:bootRun
```

This will start the local inbox API with an in-memory H2 database. If you want to persist data to a local postgres
instance or docker container, make sure your postgres instance for inbox is listening on port `2345`:

```shell
docker run --rm -it --name inbox -e POSTGRES_PASSWORD=password -e POSTGRES_USER=inbox -e POSTGRES_DB=inbox -p 2345:5432 postgres
```

Then run the following command:

```shell script
SPRING_PROFILES_ACTIVE=postgres ./gradlew inbox-api:bootRun
```

### Features

Features are implemented as spring profiles as well, but should only control one aspect of the application

| Feature    | Description |
|------------|---|
| postgres   | Switches the default H2 DB to an external postgres server |
| lesshealth | restricts the health information provided by spring actuator
| outbox     | Enables the inbox to send requests to the actual outbox, not a mock
| secrets    | Enables usage of the AWS secrets manager instead of a mock
| jsonlog    | Enables json logging. Nice for production, less nice in a console
| swagger    | Enables the swagger docs at /swagger-ui/docs.html

Currently used profiles for production:
outbox,secrets,oauth,upload,postgres

## Outbox

Exists in the "DMZ" network between the intra- and internet

To start the Outbox API locally, run the following command:

```shell script
./gradlew outbox-api:bootRun
```

This will start the local outbox API with an in-memory H2 database. If you want to persist data to a local postgres
instance or docker container, make sure your postgres instance for outbox is listening on port `5432`:

```shell
docker run --rm -it --name outbox -e POSTGRES_PASSWORD=password -e POSTGRES_USER=outbox -e POSTGRES_DB=outbox -p 5432:5432 postgres
```

Then run the following command:

```shell script
SPRING_PROFILES_ACTIVE=postgres ./gradlew outbox-api:bootRun
```

### Swagger Documentation

If you want to see the swagger documentation for the Inbox API locally, make sure your Inbox API is running
with `swagger` profile and then visit `localhost:8081/swagger-ui/docs.html`. You should then be able to see the
different endpoints under each controller, and the query parameters or payloads each accepts (and returns).

### Features

Features are implemented as spring profiles as well, but should only control one aspect of the application

| Feature | Description |
|---|---|
| postgres | Switches the default H2 DB to an external postgres server |
| lesshealth | restricts the health information provided by spring actuator
| secrets | Enables usage of the AWS secrets manager instead of a mock
| jsonlog | Enables json logging.  Nice for production, less nice in a console
| email | Enables Mailjet email sending
| sms | Enables Mailjet SMS sending
| phonevalidator | Enables validating phone numbers with the google library

Currently used profiles for production:
email,phonevalidator,sms,oauth,secrets,postgres

## Connector Portal
The Frontend communicating with the outbox is implemented here: https://github.com/smart4health/connector-portal

## Code Stats
Generated with [tokei](https://github.com/XAMPPRocky/tokei)

```
===============================================================================
 Language            Files        Lines         Code     Comments       Blanks
===============================================================================
 Batch                   1           92           71            0           21
 CSS                     3          104           88            1           15
 Dockerfile              1           15           10            0            5
 HTML                    4           93           83            0           10
 JavaScript              3           55           36           13            6
 JSON                   39        23530        23530            0            0
 Kotlin                205        12781        10541          264         1976
 Shell                   1          244          103          116           25
 SQL                    16           69           69            0            0
 Plain Text              3           18            0           18            0
 TOML                    1          102           80            1           21
 XML                     6          160          135            0           25
 YAML                    4          330          279            7           44
-------------------------------------------------------------------------------
 Markdown                2          349            0          232          117
 |- Shell                1           10           10            0            0
 (Total)                            359           10          232          117
===============================================================================
 Total                 289        37942        35025          652         2265
===============================================================================
```
