# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [TBD] - 2022-06-23

- Add configuration properties (opa.authorizer.truststore.*) for truststore for HTTPS connections to OPA ([@iamatwork](https://github.com/@iamatwork))

## [1.4.0] - 2022-01-11

- Collect and expose JMX metrics from OPA authorizer ([@quangminhtran94](https://github.com/quangminhtran94))

### Changes

## [1.3.0] - 2021-11-24

### Changes

- Fix issue where unimplemented `acls` method of authorizer would be called under certain conditions ([@iamatwork](https://github.com/@iamatwork))
- Change package group from com.bisnode.kafka.authorization to org.openpolicyagent.kafka

## [1.2.0] - 2021-10-12

### Changes

- Ensure compatibility with Kafka 3.0.0 (@scholzj)

## [1.1.0] - 2021-06-11

### Changes

- Update to Kafka library 2.8.0
  - Tested on Kafka 2.7.0 & 2.8.0

## [1.0.0] - 2021-03-29

### Changes

#### Breaking changes:

- Update to use Scala 2.13
  - Requires a Kafka cluster running 2.13
- Update to Kafka library 2.7.0
  - Requires Kafka 2.7.X
- New input structure to OPA
  - You will need to adjust policies to work with the new input structure. See an example of the new structure down below. We suggest to update your policies before upgrading, to work with both the old and the new structure. Then upgrade the plugin and then remove the old policies.`

New input structure:
```json
{
    "action": {
        "logIfAllowed": true,
        "logIfDenied": true,
        "operation": "DESCRIBE",
        "resourcePattern": {
            "name": "alice-topic",
            "patternType": "LITERAL",
            "resourceType": "TOPIC",
            "unknown": false
        },
        "resourceReferenceCount": 1
    },
    "requestContext": {
        "clientAddress": "192.168.64.1",
        "clientInformation": {
            "softwareName": "unknown",
            "softwareVersion": "unknown"
        },
        "connectionId": "192.168.64.4:9092-192.168.64.1:58864-0",
        "header": {
            "data": {
                "clientId": "rdkafka",
                "correlationId": 5,
                "requestApiKey": 3,
                "requestApiVersion": 2
            },
            "headerVersion": 1
        },
        "listenerName": "SASL_PLAINTEXT",
        "principal": {
            "name": "alice-consumer",
            "principalType": "User"
        },
        "securityProtocol": "SASL_PLAINTEXT"
    }
}
```

#### Other changes

- Include `guava` and `paranamer` in the shadowJar since it's been excluded from the Kafka installation
- Update to use the new Kafka libraries to use the new API
- Update OPA policy and tests to work with the new input structure
- Update version on various dependencies
- Add Maven information to README
- Update changelog

## [0.4.2] - 2020-10-20
- Update Guava to 30.0-jre
- Update OPA Gradle plugin to 0.3.0
- Update github release script to properly use username and password

## [0.4.1] - 2020-04-29
- Release to Maven Central under com.bisnode.kafka.authorization group. No code changes.

## [0.4.0] - 2020-04-23
- Allow `super.users` to bypass OPA authorizer checks - [@scholzj](https://github.com/scholzj)
- Fix wrong unit provided in docs on cache expiry - [@kelvk](https://github.com/kelvk)

## [0.3.0] - 2019-11-28
- Default cache size increase from 500 to 50000 based on real world usage metrics.
- Don't cache decision on errors as to avoid locking a client out if actually authorized.

## [0.2.0] - 2019-11-21
- Fix connection leak in authorization call.

## [0.1.0] - 2019-11-14
### Added
- First release!
