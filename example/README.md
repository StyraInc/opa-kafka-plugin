# Open Policy Agent and Kafka with Docker Compose

Example code for running Kafka with client certificates for authentication, and using OPA for authorization decisions.

## Setup

1. Build the OPA Kafka authorizer plugin. From the project root directory, run: `./gradlew shadowJar`.
2. Build an OPA bundle. From this directory, run: `opa build --bundle --output policy/bundle.tar.gz ../src/main/rego/`
3. Run the `create_cert.sh` script to create server and client certificates. These will be found in the `cert` directory.
4. `docker compose up`

## Updating policy

Simply rebuild the policy bundle: `opa build --bundle --output policy/bundle.tar.gz ../src/main/rego/`

## Querying Kafka

Three different users (represented by client certificates) are created by the `create_cert.sh` script:

* `alice-mgmt` - can produce and consume to any topic named `alice-*`
* `alice-producer` - can produce to any topic named `alice-*`
* `alice-consumer` - can consume from any topic named `alice-*`

The following example commands uses the CLI client tools provided with Kafka,
and assume the Kafka root directory as the current working directory.

### Producing to a topic

Using `alice-mgmt` or `alice-producer`:

```shell
bin/kafka-console-producer.sh --bootstrap-server localhost:9093 --topic alice-topic1 --producer.config path/to/cert/client/alice-mgmt.properties
> My first message
> My second message
...
Ctrl+c
```

`alice-consumer` should however not be authorized:

```shell

bin/kafka-console-producer.sh --bootstrap-server localhost:9093 --topic alice-topic1 --producer.config path/to/cert/client/alice-consumer.properties
> My first message
>[2021-12-01 09:43:45,437] ERROR Error when sending message to topic alice-topic1 with key: null, value: 8 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [alice-topic1
```

### Consuming from a topic

Using `alice-mgmt` or `alice-consumer`:

```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --alice-topic1 test --consumer.config path/to/cert/client/alice-consumer.properties --from-beginning
My first message
My second message
...
Ctrl+c

Processed a total of 4 messages
```

`alice-producer` should however not be authorized:

```shell

bin/kafka-console-producer.sh --broker-list localhost:9093 --topic alice-topic1 --producer.config path/to/cert/client/alice-producer.properties
> My first message
>[2021-12-01 09:43:45,437] ERROR Error when sending message to topic alice-topic1 with key: null, value: 8 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [alice-topic1
```