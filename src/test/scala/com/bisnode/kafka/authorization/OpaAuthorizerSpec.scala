package com.bisnode.kafka.authorization

import java.net.InetAddress

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Operation, Read, Resource, Topic, Write}
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

/**
 * Integration level tests of Kafka policy as described in src/main/rego/README.md
 * NOTE: Requires a running OPA instance with the provided policy loaded.
 */
@RunWith(classOf[JUnitRunner])
class OpaAuthorizerSpec extends FlatSpec with Matchers {

  "Request object" should "serialize to JSON" in {
    val request = createRequest("bob", "bob-topic", Read)
    val objectMapper = new ObjectMapper() with ScalaObjectMapper
    objectMapper.registerModule(DefaultScalaModule)

    objectMapper.writeValueAsString(request) should not be "{}"
  }

  "OpaAuthorizer" should "authorize when username matches name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "alice-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (true)
  }

  "OpaAuthorizer" should "not authorize when username does not match name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "bob-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
  }

  "OpaAuthorizer" should "not authorize read request for producer" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "alice-topic", Read)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
  }

  "OpaAuthorizer" should "not authorize write request for consumer" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-consumer", "alice-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
  }

  def setupAuthorizer(): OpaAuthorizer = {
    val opaAuthorizer = new OpaAuthorizer()
    val config = new java.util.HashMap[String, String]
    config.put("opa.authorizer.url", "http://localhost:8181/v1/data/kafka/authz/allow")
    config.put("opa.authorizer.allow.on.error", "false")
    opaAuthorizer.configure(config)
    opaAuthorizer
  }

  def createRequest(username: String, topic: String, operation: Operation): Request = {
    val session = Session(new KafkaPrincipal("User", username), InetAddress.getLoopbackAddress)
    val resource = Resource(Topic, topic, PatternType.LITERAL)

    new Request(new Input(session, operation, resource))
  }
}
