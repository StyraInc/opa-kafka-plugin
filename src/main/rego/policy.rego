package kafka.authz

import future.keywords.in

# ----------------------------------------------------
#  Policies
# ----------------------------------------------------

default allow = false

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
	input.action.operation == "READ"
	input.action.resourcePattern.resourceType == "GROUP"
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

inter_broker_communication {
	input.requestContext.securityProtocol == "SSL"
	input.requestContext.principal.principalType == "User"
	username == "localhost"
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
	action.operation in ["READ", "WRITE", "CREATE", "ALTER", "DESCRIBE", "DELETE"]
}

as_consumer {
	regex.match(".*-consumer", username)
}

as_producer {
	regex.match(".*-producer", username)
}

as_mgmt_user {
	regex.match(".*-mgmt", username)
}

on_own_topic(action) {
	owner := trim(username, "-consumer")
	regex.match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(username, "-producer")
	regex.match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(username, "-mgmt")
	regex.match(owner, action.resourcePattern.name)
}

username = substring(name, 3, count(name)) {
	name := input.requestContext.principal.name
	startswith(name, "CN=")
} else = input.requestContext.principal.name {
	true
}
