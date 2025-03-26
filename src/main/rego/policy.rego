package kafka.authz

# ----------------------------------------------------
#  Policies
# ----------------------------------------------------

default allow = false

allow if {
	inter_broker_communication
}

allow if {
	consume(input.action)
	on_own_topic(input.action)
	as_consumer
}

allow if {
	produce(input.action)
	on_own_topic(input.action)
	as_producer
}

allow if {
	create(input.action)
	on_own_topic(input.action)
}

allow if {
	any_operation(input.action)
	on_own_topic(input.action)
	as_mgmt_user
}

allow if {
	input.action.operation == "READ"
	input.action.resourcePattern.resourceType == "GROUP"
}

allow if {
	describe(input.action)
}

allow if {
	idempotent_produce(input.action)
}

# ----------------------------------------------------
#  Functions
# ----------------------------------------------------

inter_broker_communication if {
	input.requestContext.principal.name == "ANONYMOUS"
}

inter_broker_communication if {
	input.requestContext.securityProtocol == "SSL"
	input.requestContext.principal.principalType == "User"
	username == "localhost"
}

consume(action) if {
	action.operation == "READ"
}

produce(action) if {
	action.operation == "WRITE"
}

idempotent_produce(action) if {
	action.operation == "IDEMPOTENT_WRITE"
}

create(action) if {
	action.operation == "CREATE"
}

describe(action) if {
	action.operation == "DESCRIBE"
}

any_operation(action) if {
	action.operation in ["READ", "WRITE", "CREATE", "ALTER", "DESCRIBE", "DELETE"]
}

as_consumer if {
	regex.match(".*-consumer", username)
}

as_producer if {
	regex.match(".*-producer", username)
}

as_mgmt_user if {
	regex.match(".*-mgmt", username)
}

on_own_topic(action) if {
	owner := trim(username, "-consumer")
	regex.match(owner, action.resourcePattern.name)
}

on_own_topic(action) if {
	owner := trim(username, "-producer")
	regex.match(owner, action.resourcePattern.name)
}

on_own_topic(action) if {
	owner := trim(username, "-mgmt")
	regex.match(owner, action.resourcePattern.name)
}

username := cn_parts[0] if {
	name := input.requestContext.principal.name
	startswith(name, "CN=")
	parsed := parse_user(name)
	cn_parts := split(parsed.CN, ".")
}
# If client certificates aren't used for authentication
else := input.requestContext.principal.name if {
	true
}

parse_user(user) := {key: value |
	parts := split(user, ",")
	[key, value] := split(parts[_], "=")
}
