package com.bisnode.kafka.authorization

import java.net.InetAddress
import java.util
import java.util.Collections
import java.util.concurrent.TimeUnit

import kafka.network.RequestChannel
import kafka.network.RequestChannel.Session
import org.apache.kafka.common.acl.AclOperation
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourceType.TOPIC
import org.apache.kafka.common.security.auth.{KafkaPrincipal, SecurityProtocol}
import org.apache.kafka.server.authorizer.{AuthorizableRequestContext, Action}
import scala.jdk.CollectionConverters._

object OpaAuthorizerBenchmark {
  private val opaUrl = "http://localhost:8181/v1/data/kafka/authz/allow"

  def main(args: Array[String]): Unit = {
    val startTime = System.nanoTime
    val benchmark = new OpaAuthorizerBenchmark
    val numCalls = 10000
    for (_ <- 1 until numCalls) {
      val input = benchmark.createRequest
      benchmark.getAuthorizer.authorize(input.requestContext, input.actions.asJava)
    }

    val endTime = System.nanoTime
    val totalTime = endTime - startTime
    val perCallTime = totalTime / numCalls
    val totalTimeMs = TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS)
    val perCallMs = TimeUnit.MILLISECONDS.convert(perCallTime, TimeUnit.NANOSECONDS)

    println("Tests run in " + totalTimeMs + " milliseconds")
    println("Time per call is " + perCallMs + " ms")
  }
}
class OpaAuthorizerBenchmark {
  private val authorizer = new OpaAuthorizer

  val config: util.HashMap[String, String] = new util.HashMap[String, String](2)
  config.put("opa.authorizer.url", OpaAuthorizerBenchmark.opaUrl)
  config.put("opa.authorizer.allow.on.error", "false")
  authorizer.configure(config)

  def createRequest = {
    val principal = new KafkaPrincipal("User", "user-" + new scala.util.Random().nextInt())
    val session = RequestChannel.Session(principal, InetAddress.getLoopbackAddress)
    val resource = new ResourcePattern(TOPIC, "my-topic", PatternType.LITERAL)
    val authzReqContext = new AzRequestContext(
      clientId = "rdkafka",
      requestType = 1,
      listenerName = "SASL_PLAINTEXT",
      clientAddress = session.clientAddress,
      principal = session.principal,
      securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
      correlationId = new scala.util.Random().nextInt(1000),
      requestVersion = 4)
    val actions = List(new Action(AclOperation.WRITE, resource, 1, true, true))

    FullRequest(authzReqContext, actions)
  }

  private def getAuthorizer = authorizer
}
