# opa-kafka-plugin
![](https://github.com/Bisnode/opa-kafka-plugin/workflows/build/badge.svg)
[![codecov](https://codecov.io/gh/Bisnode/opa-kafka-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/Bisnode/opa-kafka-plugin)

Open Policy Agent (OPA) plugin for Kafka authorization.

### Prerequisites

* Kafka 2.3.x (compile from source for other versions)
* Java 11 or above
* OPA installed and running on the brokers

## Installation

Download the latest [OPA authorizer plugin](https://github.com/Bisnode/opa-kafka-plugin/releases/)  
and put the jar file (`opa-authorizer-0.1.0-all.jar`) somewhere Kafka recognizes it - this could be directly 
in Kafkas `libs` directory or in a separate plugin directory pointed out to Kafka at startup, e.g: 

`CLASSPATH=/usr/local/share/kafka/plugins/*`

To activate the opa-kafka-plugin add the `authorizer.class.name` to server.properties\
`authorizer.class.name=com.bisnode.kafka.authorization.OpaAuthorizer`

<br />
The plugin supports the following properties:

| Property Key | Example | Description |
| --- | --- | --- |
| `opa.authorizer.url` | `http://opa:8181/v1/data/kafka/authz/allow` | Name of the OPA policy to query. |
| `opa.authorizer.allow.on.error` | `false` | Fail-closed or fail-open if OPA call fails. |
| `opa.authorizer.cache.initial.capacity` | `100` | Initial decision cache size. |
| `opa.authorizer.cache.maximum.size` | `100` | Max decision cache size. |
| `opa.authorizer.cache.expire.after.ms` | `600000` | Decision cache expiry in milliseconds. |

## Usage

Example structure of input data provided from opa-kafka-plugin to Open Policy Agent.
```
{
  "operation": {
    "name": "Write"
  },
  "resource": {
    "resourceType": {
      "name": "Topic"
    },
    "name": "alice-topic1"
  },
  "session": {
    "principal": {
      "principalType": "alice-producer"
    },
    "clientAddress": "172.21.0.5",
    "sanitizedUser": "alice-producer"
  }
}
```

The following table summarizes the supported resource types and operation names.

| `input.resourceType.name` | `input.operation.name` |
| --- | --- |
| `Cluster` | `ClusterAction` |
| `Cluster` | `Create` |
| `Cluster` | `Describe` |
| `Group` | `Read` |
| `Group` | `Describe` |
| `Topic` | `Alter` |
| `Topic` | `Delete` |
| `Topic` | `Describe` |
| `Topic` | `Read` |
| `Topic` | `Write` |

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

## Performance
Performance results of opa-kafka-plugin compared with ACL's and even unauthorized
access to Kafka shows that there is a very little trade off when it comes to
performance when using either this opa-kafka-plugin or ACL's.

The tests were made with the following setup:

|Resources|#|
|---|---|
| Brokers | 3 |
| Partitions per topic | 10 |
| Replicas per topic | 3 |
| Zookeeper nodes | 3 |

Background noise on one topic with a producer producing 5000 msgs/s with message
size of 512b were used in all tests.

### opa-kafka-plugin test results

|Test #|Records sent|Payload(b)|records/s|MB/sec|avg latency (ms)|latency avg 50th perc (ms)|latency avg 95th perc (ms)|latency avg 99th perc (ms)|latency avg 99,99 perc (ms)|
|---|---|---|---|---|---|---|---|---|---|
|1|102000|666|170|0.11|4.07|1|2|18|748|
|2|102000|330|170|0.05|2.03|1|2|18|754|
|3|102000|1200|170|0.19|2.09|1|2|19|946|

### ACL test results

|Test #|Records sent|Payload(b)|records/s|MB/sec|avg latency (ms)|latency avg 50th perc (ms)|latency avg 95th perc (ms)|latency avg 99th perc (ms)|latency avg 99,99 perc (ms)|
|---|---|---|---|---|---|---|---|---|---|
|1|102000|666|170|0.11|3.45|1|4|21|604|
|2|102000|330|170|0.05|3.21|1|3|22|566|
|3|102000|1200|170|0.19|3.01|1|3|19|504|

### No authorization test results

|Test #|Records sent|Payload(b)|records/s|MB/sec|avg latency (ms)|latency avg 50th perc (ms)|latency avg 95th perc (ms)|latency avg 99th perc (ms)|latency avg 99,99 perc (ms)|
|---|---|---|---|---|---|---|---|---|---|
|1|102000|666|170| 1.11|1.70|1|2|19|128|
|2|102000|330|170| 0.05|2.03|1|2|18|298|
|3|102000|1200|170| 0.19|2.09|1|2|18|332|

## Build from source

Using gradle wrapper: `./gradlew clean test shadowJar`

The resulting jar (with dependencies embedded) will be named `opa-authorizer-{$VERSION}-all.jar` and stored in
`build/libs`.
