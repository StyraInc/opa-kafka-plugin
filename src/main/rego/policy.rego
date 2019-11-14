package kafka.authz

# ----------------------------------------------------
#  Policies
# ----------------------------------------------------

default allow = false

allow {
	inter_broker_communication
}

allow {
    consume
    on_own_topic
    as_consumer
}

allow {
	produce
	on_own_topic
    as_producer
}

allow {
    create
    on_own_topic
}

allow {
    any_operation
    on_own_topic
    as_mgmt_user
}

allow {
    describe
}


# ----------------------------------------------------
#  Functions
# ----------------------------------------------------

inter_broker_communication {
    input.session.principal.name == "ANONYMOUS"
}

consume {
    input.operation.name == "Read"
}

produce {
    input.operation.name == "Write"
}

create {
    input.operation.name == "Create"
}

describe {
    input.operation.name == "Describe"
}

any_operation {
    all_operations := ["Read", "Write", "Create", "Alter", "Describe", "Delete"]
    all_operations[_] == input.operation.name
}

as_consumer {
    re_match(".*-consumer", input.session.principal.name)
}

as_producer {
    re_match(".*-producer", input.session.principal.name)
}

as_mgmt_user {
    re_match(".*-mgmt", input.session.principal.name)
}

on_own_topic = true {
    owner := trim(input.session.principal.name, "-consumer")
    re_match(owner, input.resource.name)
}

on_own_topic = true {
    owner := trim(input.session.principal.name, "-producer")
    re_match(owner, input.resource.name)
}

on_own_topic = true {
    owner := trim(input.session.principal.name, "-mgmt")
    re_match(owner, input.resource.name)
}
