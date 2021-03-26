package kafka.authz

# ----------------------------------------------------
#  Policies
# ----------------------------------------------------

default allow = false

# Anything from brokers (ANONYMOUS) is allowed (Other way to identify cluster actions?)
allow {
	inter_broker_communication
}

allow {
	consume(input.action)
	on_own_topic(input.action)
	as_consumer
}

allow {
	produce(input.action)
	on_own_topic(input.action)
	as_producer
}

allow {
	create(input.action)
	on_own_topic(input.action)
}

allow {
	any_operation(input.action)
	on_own_topic(input.action)
	as_mgmt_user
}

allow {
	describe(input.action)
}

# ----------------------------------------------------
#  Functions
# ----------------------------------------------------

inter_broker_communication {
	input.requestContext.principal.name == "ANONYMOUS"
}

consume(action) {
	action.operation == "READ"
}

produce(action) {
	action.operation == "WRITE"
}

create(action) {
	action.operation == "CREATE"
}

describe(action) {
	action.operation == "DESCRIBE"
}

any_operation(action) {
	all_operations := ["READ", "WRITE", "CREATE", "ALTER", "DESCRIBE", "DELETE"]
	all_operations[_] == action.operation
}

as_consumer {
	re_match(".*-consumer", input.requestContext.principal.name)
}

as_producer {
	re_match(".*-producer", input.requestContext.principal.name)
}

as_mgmt_user {
	re_match(".*-mgmt", input.requestContext.principal.name)
}

on_own_topic(action) {
	owner := trim(input.requestContext.principal.name, "-consumer")
	re_match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(input.requestContext.principal.name, "-producer")
	re_match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(input.requestContext.principal.name, "-mgmt")
	re_match(owner, action.resourcePattern.name)
}
