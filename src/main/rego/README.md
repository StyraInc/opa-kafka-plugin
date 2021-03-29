# Policy sample

This sample assumes that users are prefixed with the owner name and suffixed
with the type of user (`-consumer`, `-producer`, `-mgmt`).
It assumes that inter broker communication is unauthenticated.

For example, only users prefixed with `alice` will be able to read or consume
topic `alice-topic1`, depending on the user type.

See [policy tests](../../test/rego/README.md)
