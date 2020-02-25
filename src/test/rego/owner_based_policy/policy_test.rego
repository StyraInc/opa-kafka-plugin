package kafka.authz

# --------------------------------------------------
#   Positive test
# --------------------------------------------------

# Brokers
test_inter_broker_communication {
    allow
        with input.session.principal.name as "ANONYMOUS"
}

# Consumers
test_consume_own_topic_as_consumer {
    allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-consumer"
        with input.resource.name as "alice-mytopic"
}
test_create_own_topic_as_consumer {
    allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-consumer"
        with input.resource.name as "alice-topic1"
}

# Producers
test_produce_own_topic_as_producer {
    allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "alice-mytopic"
}

test_create_own_topic_as_producer {
    allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "alice-topic1"
}

# Global access
test_anyone_describe_some_topic {
    allow
        with input.operation.name as "Describe"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "some-mytopic"
}
test_anyone_describe_own_topic {
    allow
        with input.operation.name as "Describe"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "alice-mytopic"
}

# MGMT User tests
test_mgmt_user_own_topic_read {
    allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}
test_mgmt_user_own_topic_write {
    allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}
test_mgmt_user_own_topic_create {
    allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}
test_mgmt_user_own_topic_delete {
    allow
        with input.operation.name as "Delete"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}
test_mgmt_user_own_topic_describe {
    allow
        with input.operation.name as "Describe"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}
test_mgmt_user_own_topic_alter {
    allow
        with input.operation.name as "Alter"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "alice-topic1"
}


# --------------------------------------------------
#   Negative test
# --------------------------------------------------

test_consume_own_topic_as_producer {
    not allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "alice-mytopic"
}

test_consume_someone_elses_topic_as_producer {
    not allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "someone-mytopic"
}

test_consume_someone_elses_topic_as_consumer {
    not allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-consumer"
        with input.resource.name as "someone-mytopic"
}

test_produce_own_topic_as_consumer {
    not allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-consumer"
        with input.resource.name as "alice-mytopic"
}

test_produce_someone_elses_topic_as_consumer {
    not allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-consumer"
        with input.resource.name as "someone-mytopic"
}

test_produce_someone_elses_topic_as_producer {
    not allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "someone-mytopic"
}

test_create_someone_elses_topic_as_producer {
    not allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "someone-topic1"
}

test_create_someone_elses_topic_as_consumer {
    not allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-producer"
        with input.resource.name as "someone-topic1"
}


# MGMT User tests
test_mgmt_user_other_topic_read {
    not allow
        with input.operation.name as "Read"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "some-topic1"
}
test_mgmt_user_other_topic_write {
    not allow
        with input.operation.name as "Write"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "some-topic1"
}
test_mgmt_user_other_topic_create {
    not allow
        with input.operation.name as "Create"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "some-topic1"
}
test_mgmt_user_other_topic_delete {
    not allow
        with input.operation.name as "Delete"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "some-topic1"
}
test_mgmt_user_other_topic_alter {
    not allow
        with input.operation.name as "Alter"
        with input.session.principal.name as "alice-mgmt"
        with input.resource.name as "some-topic1"
}
