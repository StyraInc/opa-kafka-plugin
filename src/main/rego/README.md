# Policy sample

This sample assumes that users are prefixed with the owner name and suffixed
with the type of user (`-consumer`, `-producer`, `-mgmt`).
It assumes that inter broker communication is unauthenticated.

For example topic `alice-topic1`, only users prefixed with `alice` will be able to
read or consume based on their suffix.
The mgmt user got more permissions.

# Tests

See [policy tests](../../test/rego/README.md)
