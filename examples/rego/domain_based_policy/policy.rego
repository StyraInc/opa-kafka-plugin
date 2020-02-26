package kafka.authz

# Import path to data. As long as using "as bundle_data" it can a preferred path.
import data.kafka.permissions as bundle_data

default allow = false

# ------------- Standard policy ------------- #
allow {
	not blacklisted_produce
	produce_on_source
}

allow {
	not blacklisted_consume
	consume_on_target
}

allow {
	input.operation.name == "Describe"
}

allow {
    inter_broker_communication
}

# If migriting from a non-authorized kafka to an auhtorized
#  with a small set of users it can be good to allow
#  them the same rights as before during a migration period.
#  Make sure to remove this rule after migration is done.
#
# allow {
#     legacy_user
# }

# ------------- Based on bulk data ------------- #
allow {
	not blacklisted_consume
	whitelisted_consumer
}
allow {
	not blacklisted_produce
	whitelisted_producer
}

# --------------------- FUNCTIONS ---------------------- #

inter_broker_communication {
    # If inter-broker communication is unauthenticated
    # they will come with username ANONYMOUS. Make sure to protect
    # this using network rules, or consider having authenticated
    # brokers.
    input.session.sanitizedUser == "ANONYMOUS"
}

# legacy_user {
#     input.session.sanitizedUser == "LEGACY_USER"
# }

produce_on_source {
    is_produce_operation
    topic := topic_segments(input.resource.name)
    user := user_segments(input.session.sanitizedUser)
    user[0] == topic[0]
}

consume_on_target {
    is_consume_operation
    topic := topic_segments(input.resource.name)
    user := user_segments(input.session.sanitizedUser)
    user[0] == topic[2]
}


is_consume_operation {
	consume_operations := ["Read", "Create"]
    input.operation.name == consume_operations[_]
}

is_produce_operation {
	produce_operations := ["Write", "Create"]
    input.operation.name == produce_operations[_]
}


# Topic name standard: source.topicname.target
topic_segments(topic) = output {
	output := split(topic, ".")
}

# User name standard: owner.username
user_segments(user) = output {
	output := split(user, ".")
}


# ----------------------- FUNCTIONS BASED ON BULK DATA ----------------------- #

whitelisted_consumer {
    is_consume_operation
    regex.globs_match(input.resource.name, bundle_data.user_permissions[input.session.sanitizedUser].allowed_consume[_])
}
whitelisted_consumer {
    is_consume_operation
    user := user_segments(input.session.sanitizedUser)
    user[0] == bundle_data.topic_permissions[input.resource.name].allowed_consume[_]
}

whitelisted_producer {
    is_produce_operation
    regex.globs_match(input.resource.name, bundle_data.user_permissions[input.session.sanitizedUser].allowed_produce[_])
}
whitelisted_producer {
    is_produce_operation
    user := user_segments(input.session.sanitizedUser)
    user[0] == bundle_data.topic_permissions[input.resource.name].allowed_produce[_]
}

# Blacklist validation
blacklisted_consume {
    regex.globs_match(input.resource.name, bundle_data.user_permissions[input.session.sanitizedUser].denied_consume[_])
}
blacklisted_consume {
	user := user_segments(input.session.sanitizedUser)
    user[0] == bundle_data.topic_permissions[input.resource.name].denied_consume[_]
}
blacklisted_produce {
    regex.globs_match(input.resource.name, bundle_data.user_permissions[input.session.sanitizedUser].denied_produce[_])
}
blacklisted_produce {
	user := user_segments(input.session.sanitizedUser)
    user[0] == bundle_data.topic_permissions[input.resource.name].denied_produce[_]
}
# -------------------------------------------- #
