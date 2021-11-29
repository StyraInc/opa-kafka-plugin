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

Using the `alice-mgmt` user with a client certificate. The following commands assume the Kafka root directory
as the current working directory.

### Producing to a topic

```shell
bin/kafka-console-producer.sh --broker-list localhost:9093 --topic alice-topic1 --producer.config path/to/cert/client/alice.properties
> My first message
> My second message
...
Ctrl+c
```

### Consuming from a topic

```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --alice-topic1 test --consumer.config path/to/cert/client/alice.properties --from-beginning
My first message
My second message
...
Ctrl+c

Processed a total of 4 messages
```