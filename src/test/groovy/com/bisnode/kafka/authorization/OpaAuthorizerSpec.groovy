package com.bisnode.kafka.authorization

import kafka.network.RequestChannel
import kafka.security.auth.Operation
import kafka.security.auth.Resource
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

@Subject(OpaAuthorizer)
class OpaAuthorizerSpec extends Specification {

    OpaAuthorizer _opaAuthorizer

    def setup() {
        _opaAuthorizer = new OpaAuthorizer()
        _opaAuthorizer.configure([
                'opa.authorizer.url': 'http://localhost:8181/v1/data/kafka/authz/allow',
                'opa.authorizer.allow.on.error': 'false'
        ])
    }

    @Ignore
    def 'Given '() {
        given:
        _opaAuthorizer
        def session = Mock(RequestChannel.Session)
        def operation = Mock(Operation)
        def resource = Mock(Resource)

        when:
        boolean result = _opaAuthorizer.authorize(session, operation, resource)

        then:
        result
    }



}
