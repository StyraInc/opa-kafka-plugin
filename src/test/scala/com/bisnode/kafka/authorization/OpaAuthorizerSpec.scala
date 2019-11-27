package com.bisnode.kafka.authorization

import java.net.{InetAddress, URI}
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Operation, Read, Resource, Topic, Write}
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

/**
 * Integration level tests of Kafka policy as described in src/main/rego/README.md
 * NOTE: Requires a running OPA instance with the provided policy loaded
 */
@RunWith(classOf[JUnitRunner])
class OpaAuthorizerSpec extends FlatSpec with Matchers with PrivateMethodTester {

  private val opaUrl = "http://localhost:8181/v1/data/kafka/authz/allow"
  private val objectMapper = (new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)
  private lazy val opaResponse = testOpaConnection()

  override def withFixture(test: NoArgTest): Outcome = {
    assume(opaResponse.isDefined, s"Assumed OPA would respond to request at $opaUrl")

    val resp = opaResponse.get
    assume(resp.statusCode() == 200, "Assumed OPA would respond with status code 200")
    assume(!objectMapper.readTree(resp.body()).at("/result").asBoolean, "Assumed OPA would return negative result")

    super.withFixture(test)
  }

  "Request object" should "serialize to JSON" in {
    val request = createRequest("bob", "bob-topic", Read)

    objectMapper.writeValueAsString(request) should not be "{}"
  }

  "OpaAuthorizer" should "authorize when username matches name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "alice-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (true)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "not authorize when username does not match name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "bob-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "not authorize read request for producer" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-producer", "alice-topic", Read)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "not authorize write request for consumer" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-consumer", "alice-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "cache the first request" in {
    val opaAuthorizer = setupAuthorizer()
    val request = createRequest("alice-consumer", "alice-topic", Read)
    val input = request.input

    for (_ <- 1 until 5) {
      opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (true)
    }

    opaAuthorizer.getCache.size should be (1)

    val otherRequest = createRequest("bob-consumer", "bob-topic", Read)
    val nextInput = otherRequest.input

    for (_ <- 1 until 5) {
      opaAuthorizer.authorize(nextInput.session, nextInput.operation, nextInput.resource) should be(true)
    }

    opaAuthorizer.getCache.size should be (2)
  }

  "OpaAuthorizer" should "not cache decisions while errors occur" in {
    val opaAuthorizer = setupAuthorizer("http://localhost/broken")
    val request = createRequest("alice-consumer", "alice-topic", Write)
    val input = request.input

    opaAuthorizer.authorize(input.session, input.operation, input.resource) should be (false)
    opaAuthorizer.getCache.size should be (0)
  }

  def setupAuthorizer(url: String = opaUrl): OpaAuthorizer = {
    val opaAuthorizer = new OpaAuthorizer()
    val config = new java.util.HashMap[String, String]
    config.put("opa.authorizer.url", url)
    config.put("opa.authorizer.allow.on.error", "false")
    opaAuthorizer.configure(config)
    opaAuthorizer
  }

  def createRequest(username: String, topic: String, operation: Operation): Request = {
    val session = Session(new KafkaPrincipal("User", username), InetAddress.getLoopbackAddress)
    val resource = Resource(Topic, topic, PatternType.LITERAL)

    Request(Input(session, operation, resource))
  }

  def testOpaConnection(): Option[HttpResponse[String]] = {
    try {
      val req = HttpRequest.newBuilder.uri(new URI(opaUrl)).POST(BodyPublishers.ofString("{}")).build
      Option(HttpClient.newBuilder.build.send(req, BodyHandlers.ofString))
    } catch {
      case _: Exception => Option.empty
    }
  }
}
