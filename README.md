#  Open Policy Agent plugin for Kafka authorization
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.bisnode.kafka.authorization/opa-authorizer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.bisnode.kafka.authorization/opa-authorizer)
![](https://github.com/anderseknert/opa-kafka-plugin/workflows/build/badge.svg)
[![codecov](https://codecov.io/gh/Bisnode/opa-kafka-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/Bisnode/opa-kafka-plugin)

Open Policy Agent (OPA) plugin for Kafka authorization.

### Prerequisites

* Kafka 2.7.0+
* Java 11 or above
* OPA installed and running on the brokers

## Installation

###

Download the latest OPA authorizer plugin jar from [Releases](https://github.com/anderseknert/opa-kafka-plugin/releases/) (or [Maven Central](https://search.maven.org/artifact/com.bisnode.kafka.authorization/opa-authorizer)) and put the
file (`opa-authorizer-{$VERSION}.jar`) somewhere Kafka recognizes it - this could be directly in Kafka's `libs` directory
or in a separate plugin directory pointed out to Kafka at startup, e.g:

`CLASSPATH=/usr/local/share/kafka/plugins/*`

To activate the opa-kafka-plugin add the `authorizer.class.name` to server.properties\
`authorizer.class.name=org.openpolicyagent.kafka.OpaAuthorizer`

<br />
The plugin supports the following properties:

| Property Key | Example | Default | Description |
| --- | --- | --- | --- |
| `opa.authorizer.url` | `http://opa:8181/v1/data/kafka/authz/allow` |  | Name of the OPA policy to query. [required] |
| `opa.authorizer.allow.on.error` | `false` | `false` | Fail-closed or fail-open if OPA call fails. |
| `opa.authorizer.cache.initial.capacity` | `5000` | `5000` | Initial decision cache size. |
| `opa.authorizer.cache.maximum.size` | `50000` | `50000` | Max decision cache size. |
| `opa.authorizer.cache.expire.after.seconds` | `3600` | `3600` | Decision cache expiry in seconds. |
| `super.users` | `User:alice;User:bob` |  | Super users which are always allowed. |

## Usage

Example structure of input data provided from opa-kafka-plugin to Open Policy Agent.
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

The following table summarizes the supported resource types and operation names.

| `input.action.resourcePattern.resourceType` | `input.action.operation` |
| --- | --- |
| `CLUSTER` | `CLUSTER_ACTION` |
| `CLUSTER` | `CREATE` |
| `CLUSTER` | `DESCRIBE` |
| `GROUP` | `READ` |
| `GROUP` | `DESCRIPTION` |
| `TOPIC` | `CREATE` |
| `TOPIC` | `ALTER` |
| `TOPIC` | `DELETE` |
| `TOPIC` | `DESCRIBE` |
| `TOPIC` | `READ` |
| `TOPIC` | `WRITE` |
| `TRANSACTIONAL_ID` | `DESCRIBE` |
| `TRANSACTIONAL_ID` | `WRITE` |

These are handled by the method _authorizeAction_, and passed to OPA with an _action_, that identifies 
the accessed resource and the performed operation. _patternType_ is always _LITERAL_.

Creation of a topic checks for CLUSTER + CREATE. If this is denied, it will check for TOPIC with its name + CREATE.

When doing idepotent write to a topic, and the first request for operation=IDEMPOTENT_WRITE on the resourceType=CLUSTER is denied,
the method _authorizeByResourceType_ to check, if the user has the right to write to any topic.
If yes, the idempotent write is granted by Kafka's ACL-implementation. To allow for a similar check,
it is mapped to OPA with _patternType=PREFIXED_, _resourceType=TOPIC_, and _name=""_.
```json
{
  "action": {
    "logIfAllowed": true,
    "logIfDenied": true,
    "operation": "DESCRIBE",
    "resourcePattern": {
      "name": "",
      "patternType": "PREFIXED",
      "resourceType": "TOPIC",
      "unknown": false
    },
    "resourceReferenceCount": 1
  },
  ...
}
```

It's likely possible to use all different resource types and operations described in the Kafka API docs:
https://kafka.apache.org/24/javadoc/org/apache/kafka/common/acl/AclOperation.html
https://kafka.apache.org/24/javadoc/org/apache/kafka/common/resource/ResourceType.html

### Security protocols:

| Protocol | Description |
|---|---|
| `PLAINTEXT` | Un-authenticated, non-encrypted channel |
| `SASL_PLAINTEXT` | authenticated, non-encrypted channel |
| `SASL` | authenticated, SSL channel |
| `SSL` | SSL channel |

More info:

https://kafka.apache.org/24/javadoc/org/apache/kafka/common/security/auth/SecurityProtocol.html

### Policy sample

With the [sample policy rego](src/main/rego/README.md) you will out of the box get
a structure where an "owner" can one user per type (`consumer`, `producer`, `mgmt`). The owner and user type is separated by `-`.
* Username structure: `<owner>-<type>`
* Topic name structure: `<owner->.*`

\
<b>Example:</b> \
User `alice-consumer` will be...
* allowed to consume on topic `alice-topic1`
* allowed to consume on topic `alice-topic-test`
* denied to produce on any topic
* denied to consume on topic `bob-topic`

[See sample rego](src/main/rego/README.md)

## Build from source

Using gradle wrapper: `./gradlew clean test shadowJar`

The resulting jar (with dependencies embedded) will be named `opa-authorizer-{$VERSION}-all.jar` and stored in
`build/libs`.

## Logging

Set log level `log4j.logger.org.openpolicyagent=INFO` in `config/log4j.properties`
Use DEBUG or TRACE for debugging.

In a busy Kafka cluster it might be good to tweak the cache since it may produce a lot of log entries in Open Policy Agent, especially if decision logs are turned on. If the policy isn't dynamically updated very often it's recommended to cache a lot to improve performance and reduce the amount of log entries.
