package kafka.authz

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
	all_operations := ["READ", "WRITE", "CREATE", "ALTER", "DESCRIBE", "DELETE"]
	all_operations[_] == action.operation
}

as_consumer {
	re_match(".*-consumer", username)
}

as_producer {
	re_match(".*-producer", username)
}

as_mgmt_user {
	re_match(".*-mgmt", username)
}

on_own_topic(action) {
	owner := trim(username, "-consumer")
	re_match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(username, "-producer")
	re_match(owner, action.resourcePattern.name)
}

on_own_topic(action) {
	owner := trim(username, "-mgmt")
	re_match(owner, action.resourcePattern.name)
}

username = substring(name, 3, count(name)) {
    name := input.requestContext.principal.name
    startswith(name, "CN=")
} else = input.requestContext.principal.name
