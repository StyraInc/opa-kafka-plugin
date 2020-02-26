# Policy sample

This sample is meant for more dynamic Kafka environments where topics come and go.
It has got a standard policy that can be used by following a domain based naming convention on topics and users, but also supports custom data to extend this policy with whitelisting and blacklisting.

## Default policy

Using the naming convention for usernames and topic names the default policy will automatically apply.

Each user should have a domain prefix followed by a dot and then a username.
For example if we have a user named `abc.alice`. `abc` could be a shortname of the product or service that the user `alice` relates to. A user can only be in one domain.

Each topic has a domain prefix and a domain suffix like `abc.mytopic.xyz`. The prefix explains which domain of users can produce on the topic while the suffix explains who can consume from the topic. In this case `abc.alice` would be able to produce to the topic and `xyz.bob` will be authorized to consume from the topic.

The same prefix and suffix on topic names can be used if the topic should be read/write within the same domain.

Describe operations are always permitted.

## Custom data

In some cases where the default policy isn't enough it can be extended using custom data. Known use cases where the standard policy is lacking is if we want to have consumers from two or more different domains on the same topic. If we want producers from different domains to the same topic, or if we want to disallow a group of users within a domain that would normally be able to consume/produce a topic.

### Custom data structure example:
```
{
    "user_permissions": {
        "abc.user1": {
            "allowed_consume": [
                "xyz.topic1.xyz"
            ],
            "allowed_produce": [
                "xyz.topic2.xyz"
            ],
            "denied_consume": [
                "xyz.topic3.abc"
            ],
            "denied_produce": [
                "abc.topic4.xyz"
            ]
        },
        "abc.user2": {
            "allowed_consume": [
                "xyz.topic.*.xyz"
            ],
            "allowed_produce": [
                "xyz.topic.*.xyz"
            ],
            "denied_consume": [
                "xyz.topic.*.abc"
            ],
            "denied_produce": [
                "abc.topic.*.xyz"
            ]
        }
    },
    "topic_permissions": {
        "xyz.topic5.xyz": {
            "allowed_consume": [
                "abc"
            ],
            "allowed_produce": [
                "abc"
            ],
            "denied_consume": [
                "xyz"
            ],
            "denied_produce": [
                "xyz"
            ]
        }
    }
}
```


# Tests

`opa test ./ test/`