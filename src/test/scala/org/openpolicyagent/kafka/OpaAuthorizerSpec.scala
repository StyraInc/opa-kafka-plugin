package org.openpolicyagent.kafka

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule

import java.net.{InetAddress, URI}
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import com.typesafe.scalalogging.LazyLogging
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.kafka.common.acl.AclOperation
import org.apache.kafka.common.network.{ClientInformation, ListenerName}
import org.apache.kafka.common.protocol.ApiKeys
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.security.auth.{KafkaPrincipal, SecurityProtocol}
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourceType
import org.apache.kafka.common.requests.{RequestContext, RequestHeader}
import org.apache.kafka.network.Session
import org.apache.kafka.server.authorizer.{Action, AuthorizationResult}
import org.scalatest._
import matchers.should._
import flatspec._
import org.apache.kafka.common.message.RequestHeaderData

import java.lang.management.ManagementFactory
import javax.management.ObjectName
import scala.jdk.CollectionConverters._

/**
 * Integration level tests of Kafka policy as described in src/main/rego/README.md
 * NOTE: Requires a running OPA instance with the provided policy loaded
 */
@RunWith(classOf[JUnitRunner])
class OpaAuthorizerSpec extends AnyFlatSpec with Matchers with PrivateMethodTester with LazyLogging {

  private val opaUrl = "http://localhost:8181/v1/data/kafka/authz/allow"
  private val requestSerializerModule = new SimpleModule()
    .addSerializer(classOf[ResourcePattern], new ResourcePatternSerializer)
    .addSerializer(classOf[Action], new ActionSerializer)
    .addSerializer(classOf[RequestContext], new RequestContextSerializer)
    .addSerializer(classOf[ClientInformation], new ClientInformationSerializer)
    .addSerializer(classOf[KafkaPrincipal], new KafkaPrincipalSerializer)
    .addSerializer(classOf[RequestHeader], new RequestHeaderSerializer)
    .addSerializer(classOf[RequestHeaderData], new RequestHeaderDataSerializer)
  private val objectMapper = JsonMapper.builder().addModule(requestSerializerModule).addModule(DefaultScalaModule).build()
  private val defaultCacheCapacity = 50000
  private lazy val opaResponse = testOpaConnection()

  override def withFixture(test: NoArgTest): Outcome = {
    assume(opaResponse.isDefined, s"Assumed OPA would respond to request at $opaUrl")

    val resp = opaResponse.get
    assume(resp.statusCode() == 200, "Assumed OPA would respond with status code 200")
    assume(!objectMapper.readTree(resp.body()).at("/result").asBoolean, "Assumed OPA would return negative result")

    super.withFixture(test)
  }

  "Request object" should "serialize to JSON" in {
    val actions = List(
      createAction("bob-topic", AclOperation.READ),
    )
    val request = createRequest("bob", actions)

    objectMapper.writeValueAsString(request) should not be "{}"
    objectMapper.writeValueAsString(request.actions.asJava) should not be "{}"
    objectMapper.writeValueAsString(request.requestContext) should not be "{}"
  }

  "OpaAuthorizer" should "authorize when username matches name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-producer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "return authorization results for multiple actions in the same request in right order" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
      createAction("alice-topic", AclOperation.WRITE),
      createAction("alice-topic", AclOperation.READ),
      createAction("alice-topic", AclOperation.READ),
      createAction("alice-topic", AclOperation.WRITE),
      createAction("alice-topic", AclOperation.DESCRIBE),
      createAction("alice-topic", AclOperation.CREATE),
    )
    val request = createRequest("alice-producer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(
      AuthorizationResult.ALLOWED,
      AuthorizationResult.ALLOWED,
      AuthorizationResult.DENIED,
      AuthorizationResult.DENIED,
      AuthorizationResult.ALLOWED,
      AuthorizationResult.ALLOWED,
      AuthorizationResult.ALLOWED).asJava)
    opaAuthorizer.getCache.size should be (4)
  }

  "OpaAuthorizer" should "not authorize when username does not match name of topic" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("bob-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-producer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.DENIED).asJava)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "not authorize read request for producer" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.READ),
    )
    val request = createRequest("alice-producer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.DENIED).asJava)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "not authorize write request for consumer" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-consumer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.DENIED).asJava)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "cache the first request" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.READ),
    )
    val request = createRequest("alice-consumer", actions)

    for (_ <- 1 until 5) {
      opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)
    }

    opaAuthorizer.getCache.size should be (1)

    val otherActions = List(
      createAction("bob-topic", AclOperation.READ),
    )
    val otherRequest = createRequest("bob-consumer", otherActions)

    for (_ <- 1 until 5) {
      opaAuthorizer.authorize(otherRequest.requestContext, otherRequest.actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)
    }

    opaAuthorizer.getCache.size should be (2)
  }

  "OpaAuthorizer" should "not cache decisions while errors occur" in {
    val opaAuthorizer = setupAuthorizer("http://localhost/broken")
    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-consumer", actions)

    opaAuthorizer.authorize(request.requestContext, request.actions.asJava) should be (List(AuthorizationResult.DENIED).asJava)
    opaAuthorizer.getCache.size should be (0)
  }

  "OpaAuthorizer" should "authorize super users without checking with OPA" in {
    val opaAuthorizer = setupAuthorizer(opaUrl)

    val actions1 = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request1 = createRequest("CN=my-user", actions1)
    opaAuthorizer.authorize(request1.requestContext, request1.actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)

    val actions2 = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request2 = createRequest("CN=my-user2,O=my-org", actions2)
    opaAuthorizer.authorize(request2.requestContext, request2.actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)

    val actions3 = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request3 = createRequest("CN=my-user3", actions3)
    opaAuthorizer.authorize(request3.requestContext, request3.actions.asJava) should be (List(AuthorizationResult.DENIED).asJava)
  }

  "OpaAuthorizer" should "authorize when RequestContext is used" in {
    val opaAuthorizer = setupAuthorizer()
    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val requestContext = new RequestContext(new RequestHeader(ApiKeys.PRODUCE, 2, "rdkafka", 5), "192.168.64.4:9092-192.168.64.1:58864-0", InetAddress.getLoopbackAddress, new KafkaPrincipal("User", "alice-producer"),
      new ListenerName("SASL_PLAINTEXT"), SecurityProtocol.SASL_PLAINTEXT, new ClientInformation("rdkafka", "1.0.0"), false)

    opaAuthorizer.authorize(requestContext, actions.asJava) should be (List(AuthorizationResult.ALLOWED).asJava)
    opaAuthorizer.getCache.size should be (1)
  }

  "OpaAuthorizer" should "set up metrics system if enabled" in {
    val opaAuthorizer = setupAuthorizer(metricsEnabled = true)
    opaAuthorizer.maybeSetupMetrics("dummy_cluster", 1)
    val server = ManagementFactory.getPlatformMBeanServer
    assert(server.isRegistered(new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.REQUEST_HANDLE_GROUP)))
    assert(server.isRegistered(new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.RESULT_GROUP)))
  }

  "OpaAuthorizer" should "record correct number of authorized request" in {
    val opaAuthorizer = setupAuthorizer(metricsEnabled = true)
    opaAuthorizer.maybeSetupMetrics("dummy_cluster", 1)

    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-producer", actions)
    opaAuthorizer.authorize(request.requestContext, request.actions.asJava)

    val server = ManagementFactory.getPlatformMBeanServer
    val authorizedRequestCountActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.RESULT_GROUP),
      MetricsLabel.AUTHORIZED_REQUEST_COUNT)
    assert(authorizedRequestCountActual == 1.0)

    val unauthorizedRequestCountActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.RESULT_GROUP),
      MetricsLabel.UNAUTHORIZED_REQUEST_COUNT)
    assert(unauthorizedRequestCountActual == 0.0)
  }

  "OpaAuthorizer" should "record correct number of unauthorized request" in {
    val opaAuthorizer = setupAuthorizer(metricsEnabled = true)
    opaAuthorizer.maybeSetupMetrics("dummy_cluster", 1)

    val actions = List(
      createAction("bob-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-producer", actions)
    opaAuthorizer.authorize(request.requestContext, request.actions.asJava)

    val server = ManagementFactory.getPlatformMBeanServer
    val authorizedRequestCountActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.RESULT_GROUP),
      MetricsLabel.AUTHORIZED_REQUEST_COUNT)
    assert(authorizedRequestCountActual == 0.0)

    val unauthorizedRequestCountActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.RESULT_GROUP),
      MetricsLabel.UNAUTHORIZED_REQUEST_COUNT)
    assert(unauthorizedRequestCountActual == 1.0)
  }

  "OpaAuthorizer" should "record correct usage statistic of cache and request to OPA" in {
    val opaAuthorizer = setupAuthorizer(metricsEnabled = true)
    opaAuthorizer.maybeSetupMetrics("dummy_cluster", 1)

    val actions = List(
      createAction("alice-topic", AclOperation.WRITE),
    )
    val request = createRequest("alice-producer", actions)
    for (_ <- 1 until 5) {
      opaAuthorizer.authorize(request.requestContext, request.actions.asJava)
    }

    val server = ManagementFactory.getPlatformMBeanServer
    val requestToOPACountActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.REQUEST_HANDLE_GROUP),
      MetricsLabel.REQUEST_TO_OPA_COUNT)
    assert(requestToOPACountActual == 1.0)

    val cacheHitRateActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.REQUEST_HANDLE_GROUP),
      MetricsLabel.CACHE_HIT_RATE)
    assert(cacheHitRateActual == opaAuthorizer.getCache.stats().hitRate())

    val cacheUsagePercentageActual = server.getAttribute(
      new ObjectName(MetricsLabel.NAMESPACE + ":type=" + MetricsLabel.REQUEST_HANDLE_GROUP),
      MetricsLabel.CACHE_USAGE_PERCENTAGE)
    assert(cacheUsagePercentageActual == (opaAuthorizer.getCache.size() / defaultCacheCapacity.toDouble))
  }


  def setupAuthorizer(url: String = opaUrl, metricsEnabled: Boolean = false): OpaAuthorizer = {
    val opaAuthorizer = new OpaAuthorizer()
    val config = new java.util.HashMap[String, String]
    config.put("opa.authorizer.url", url)
    config.put("opa.authorizer.allow.on.error", "false")
    config.put("super.users", "User:CN=my-user;User:CN=my-user2,O=my-org")
    if(metricsEnabled) {
      config.put("opa.authorizer.metrics.enabled", "true")
    }
    opaAuthorizer.configure(config)
    opaAuthorizer
  }

  def createRequest(username: String, actions: List[Action]): FullRequest = {
    val principal = new KafkaPrincipal("User", username)
    val session = new Session(principal, InetAddress.getLoopbackAddress)
    val authzReqContext = AzRequestContext(
      clientId = "rdkafka",
      requestType = 1,
      listenerName = "SASL_PLAINTEXT",
      clientAddress = session.clientAddress,
      principal = session.principal,
      securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
      correlationId = new scala.util.Random().nextInt(1000),
      requestVersion = 4)

    FullRequest(authzReqContext, actions)
  }

  def createAction(topic: String, operation: AclOperation): Action = {
    val resource = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL)
    new Action(operation, resource, 1, true, true)
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

case class FullRequest(requestContext: AzRequestContext, actions: List[Action])
