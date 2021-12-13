# OPA Tutorial Companion Scripts

This directory contains companion scripts for the
[OPA Kafka tutorial](https://www.openpolicyagent.org/docs/latest/kafka-authorization/) in the OPA docs.

The `create_cert.sh` script will create a server certificate for TLS, along with four client certificates representing
the four different users used in the tutorial, namely:

* `anon_producer`
* `anon_consumer`
* `pii_consumer`
* `fanout_producer`

These certificates will be stored in the `cert` directory, which is automatically mounted into the Kafka container using
the Docker compose file.
