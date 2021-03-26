package kafka.authz

# --------------------------------------------------
#   Positive test
# --------------------------------------------------

# Brokers
test_inter_broker_communication {
	allow with input.requestContext.principal.name as "ANONYMOUS"
}

# Consumers
test_consume_own_topic_as_consumer {
	allow with input.requestContext.principal.name as "alice-consumer"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "alice-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_create_own_topic_as_consumer {
	allow with input.requestContext.principal.name as "alice-consumer"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

# Producers
test_produce_own_topic_as_producer {
	allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "alice-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_create_own_topic_as_producer {
	allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

# Global access
test_anyone_describe_some_topic {
	allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "DESCRIBE",
			"resourcePattern": {
				"name": "some-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_anyone_describe_own_topic {
	allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "DESCRIBE",
			"resourcePattern": {
				"name": "alice-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

# MGMT User tests
test_mgmt_user_own_topic_read {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_own_topic_write {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_own_topic_create {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_own_topic_delete {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "DELETE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_own_topic_describe {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "DESCRIBE",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_own_topic_alter {
	allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "ALTER",
			"resourcePattern": {
				"name": "alice-topic1",
				"resourceType": "TOPIC",
			},
		}
}

# --------------------------------------------------
#   Negative test
# --------------------------------------------------

test_consume_own_topic_as_producer {
	not allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "alice-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_consume_someone_elses_topic_as_producer {
	not allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "someone-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_consume_someone_elses_topic_as_consumer {
	not allow with input.requestContext.principal.name as "alice-consumer"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "someone-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_produce_own_topic_as_consumer {
	not allow with input.requestContext.principal.name as "alice-consumer"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "alice-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_produce_someone_elses_topic_as_consumer {
	not allow with input.requestContext.principal.name as "alice-consumer"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "someone-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_produce_someone_elses_topic_as_producer {
	not allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "someone-mytopic",
				"resourceType": "TOPIC",
			},
		}
}

test_create_someone_elses_topic_as_producer {
	not allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "someone-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_create_someone_elses_topic_as_consumer {
	not allow with input.requestContext.principal.name as "alice-producer"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "someone-topic1",
				"resourceType": "TOPIC",
			},
		}
}

# MGMT User tests
test_mgmt_user_other_topic_read {
	not allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "READ",
			"resourcePattern": {
				"name": "some-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_other_topic_write {
	not allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "WRITE",
			"resourcePattern": {
				"name": "some-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_other_topic_create {
	not allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "CREATE",
			"resourcePattern": {
				"name": "some-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_other_topic_delete {
	not allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "DELETE",
			"resourcePattern": {
				"name": "some-topic1",
				"resourceType": "TOPIC",
			},
		}
}

test_mgmt_user_other_topic_alter {
	not allow with input.requestContext.principal.name as "alice-mgmt"
		 with input.action as {
			"operation": "ALTER",
			"resourcePattern": {
				"name": "some-topic1",
				"resourceType": "TOPIC",
			},
		}
}
