package kafka.authz

import data.test_data as test_bundle_data

test_default_allow {
    not allow
}

# ---------- Operations that should never fail ---------- #
test_inter_broker {
    allow with input.session.sanitizedUser as "ANONYMOUS"
}

# test_legacy_user {
#     allow with input.session.sanitizedUser as "LEGACY_USER"
# }

test_describe_operation {
    allow with input.operation.name as "Describe"
}

# ------------ Test base policy ------------ #
test_produce_on_source_create {
    allow
        with input.operation.name as "Create"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "abc.user1"
}
test_produce_on_source_write {
    allow
        with input.operation.name as "Write"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_consume_on_target_create {
    allow
        with input.operation.name as "Create"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "xyz.user1"
}

test_consume_on_target_read {
    allow
        with input.operation.name as "Read"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "xyz.user1"
}

test_produce_on_target_write {
    not allow
        with input.operation.name as "Write"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "xyz.user1"
}

test_consume_on_source_read {
    not allow
        with input.operation.name as "Read"
        with input.resource.name as "abc.topic1.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

# ------------ Test with bundle data ------------ #
test_whitelisted_consumer {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic1.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_whitelisted_producer {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "xyz.topic2.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_blacklisted_consumer {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic3.abc"
        with input.session.sanitizedUser as "abc.user1"
}

test_blacklisted_producer {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "abc.topic4.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_whitelisted_consumer_wildcarded {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic99.xyz"
        with input.session.sanitizedUser as "abc.user2"
}

test_whitelisted_producer_wildcarded {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "xyz.topic99.xyz"
        with input.session.sanitizedUser as "abc.user2"
}

test_blacklisted_consumer_wildcarded {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic99.abc"
        with input.session.sanitizedUser as "abc.user2"
}

test_blacklisted_producer_wildcarded {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "abc.topic99.xyz"
        with input.session.sanitizedUser as "abc.user2"
}

test_whitelisted_topic_consumer {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic5.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_whitelisted_topic_producer {
    allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "xyz.topic5.xyz"
        with input.session.sanitizedUser as "abc.user1"
}

test_blacklisted_topic_consumer {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Read"
        with input.resource.name as "xyz.topic5.xyz"
        with input.session.sanitizedUser as "xyz.user1"
}

test_blacklisted_topic_producer {
    not allow
        with data.kafka.permissions as test_bundle_data
        with input.operation.name as "Write"
        with input.resource.name as "xyz.topic5.xyz"
        with input.session.sanitizedUser as "xyz.user1"
}
