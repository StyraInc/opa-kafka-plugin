package com.bisnode.kafka.authorization

import java.net.InetAddress
import java.util
import java.util.concurrent.TimeUnit

import kafka.network.RequestChannel
import kafka.security.auth.{Operation, Resource, ResourceType}
import org.apache.kafka.common.acl.AclOperation
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourceType.TOPIC
import org.apache.kafka.common.security.auth.KafkaPrincipal

object OpaAuthorizerBenchmark {
  private val opaUrl = "http://localhost:8181/v1/data/kafka/authz/allow"

  def main(args: Array[String]): Unit = {
    val startTime = System.nanoTime
    val benchmark = new OpaAuthorizerBenchmark

    val numCalls = 10000

    for( _ <- 1 until numCalls){
      val input = benchmark.createRequest.input
      benchmark.getAuthorizer.authorize(input.session, input.operation, input.resource)
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

  private def createRequest = {
    // Adding random value to user to ensure cache misses - change as appropriate to test cache hits or both
    val user = new KafkaPrincipal("User", "user-" + new scala.util.Random().nextInt)
    val resource = new Resource(ResourceType.fromJava(TOPIC), "my-topic", PatternType.LITERAL)
    val session = RequestChannel.Session(user, InetAddress.getLoopbackAddress)
    Request(Input(session, Operation.fromJava(AclOperation.WRITE), resource))
  }

  private def getAuthorizer = authorizer
}
