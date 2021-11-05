package com.bisnode.kafka.authorization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.annotations.VisibleForTesting
import com.google.common.cache.{Cache, CacheBuilder}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.Endpoint
import org.apache.kafka.common.acl.{AclBinding, AclBindingFilter, AclOperation}
import org.apache.kafka.common.message.RequestHeaderData
import org.apache.kafka.common.network.ClientInformation
import org.apache.kafka.common.requests.{RequestContext, RequestHeader}
import org.apache.kafka.common.resource.{PatternType, ResourcePattern, ResourceType}
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.apache.kafka.server.authorizer._

import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.net.{URI, URL}
import java.time.Duration.ofSeconds
import java.util.concurrent._
import scala.jdk.CollectionConverters._

//noinspection NotImplementedCode
class OpaAuthorizer extends Authorizer with LazyLogging {
  private var config: Map[String, String] = Map.empty
  private lazy val opaUrl = new URL(config("opa.authorizer.url")).toURI
  private lazy val allowOnError = config.getOrElse("opa.authorizer.allow.on.error", "false").toBoolean
  private lazy val superUsers = config.getOrElse("super.users", "").split(";").toList

  private lazy val cache = CacheBuilder.newBuilder
    .initialCapacity(config.getOrElse("opa.authorizer.cache.initial.capacity", "5000").toInt)
    .maximumSize(config.getOrElse("opa.authorizer.cache.maximum.size", "50000").toInt)
    .expireAfterWrite(config.getOrElse("opa.authorizer.cache.expire.after.seconds", "3600").toInt, TimeUnit.SECONDS)
    .build
    .asInstanceOf[Cache[CachableRequest, Boolean]]

  override def authorize(requestContext: AuthorizableRequestContext, actions: java.util.List[Action]): java.util.List[AuthorizationResult] = {
    actions.asScala.map { action => authorizeAction(requestContext, action) }.asJava
  }

  override def configure(configs: java.util.Map[String, _]): Unit = {
    logger.debug(s"Call to configure() with config $configs")
    config = configs.asScala.view.mapValues(_.asInstanceOf[String]).toMap
  }

  // Not really used but has to be implemented for internal stuff. Can maybe be used to check OPA connectivity?
  // Just doing the same as the acl authorizer does here: https://github.com/apache/kafka/blob/trunk/core/src/main/scala/kafka/security/authorizer/AclAuthorizer.scala#L185
  override def start(authorizerServerInfo: AuthorizerServerInfo): java.util.Map[Endpoint, _ <: CompletionStage[Void] ] = {
    authorizerServerInfo.endpoints.asScala.map { endpoint =>
    endpoint -> CompletableFuture.completedFuture[Void](null) }.toMap.asJava
  }

  @VisibleForTesting
  private[authorization] def getCache = cache

  // None of the below needs implementations
  override def close(): Unit = { }
  override def acls(acls: AclBindingFilter): java.lang.Iterable[AclBinding] = ???
  override def deleteAcls(requestContext: AuthorizableRequestContext, aclBindingFilters: java.util.List[AclBindingFilter]): java.util.List[_ <: CompletionStage[AclDeleteResult]] = ???
  override def createAcls(requestContext: AuthorizableRequestContext, aclBindings: java.util.List[AclBinding]): java.util.List[_ <: CompletionStage[AclCreateResult]] = ???

  private def authorizeAction(requestContext: AuthorizableRequestContext, action: Action): AuthorizationResult = {
    val resource = action.resourcePattern
    if (resource.patternType != PatternType.LITERAL) {
      throw new IllegalArgumentException("Only literal resources are supported. Got: " + resource.patternType)
    }

    doAuthorize(requestContext, action)
  }

  override def authorizeByResourceType(requestContext: AuthorizableRequestContext, op: AclOperation, resourceType: ResourceType): AuthorizationResult = {
    doAuthorize(requestContext, new Action(op, new ResourcePattern(resourceType, "", PatternType.PREFIXED), 0, true, true))
  }

  private def doAuthorize(requestContext: AuthorizableRequestContext, action: Action) = {
    // ensure we compare identical classes
    val sessionPrincipal = requestContext.principal
    val principal = if (classOf[KafkaPrincipal] != sessionPrincipal.getClass)
      new KafkaPrincipal(sessionPrincipal.getPrincipalType, sessionPrincipal.getName)
    else
      sessionPrincipal

    val host = requestContext.clientAddress.getHostAddress

    val cachableRequest = CachableRequest(principal, action, host)
    val request = Request(Input(requestContext, action))

    def allowAccess = {
      try {
        cache.get(cachableRequest, new AllowCallable(request, opaUrl, allowOnError))
      }
      catch {
        case e: ExecutionException =>
          logger.warn(s"Exception in decision retrieval: ${e.getMessage}")
          logger.trace("Exception trace", e)
          allowOnError
      }
    }

    // Evaluate if operation is allowed
    val authorized = isSuperUser(principal) || allowAccess

    if (authorized) AuthorizationResult.ALLOWED else AuthorizationResult.DENIED
  }

  def isSuperUser(principal: KafkaPrincipal): Boolean = {
    if (superUsers.contains(principal.toString)) {
      logger.trace(s"User ${principal} is super user")
      return true
    } else false
  }
}

class ResourcePatternSerializer() extends JsonSerializer[ResourcePattern] {
  override def serialize(value: ResourcePattern, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("resourceType", value.resourceType().name())
    gen.writeStringField("name", value.name())
    gen.writeStringField("patternType", value.patternType().name())
    gen.writeBooleanField("unknown", value.isUnknown())
    gen.writeEndObject()
  }
}

class ActionSerializer() extends JsonSerializer[Action] {
  override def serialize(value: Action, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeObjectField("resourcePattern", value.resourcePattern())
    gen.writeStringField("operation", value.operation().name())
    gen.writeNumberField("resourceReferenceCount", value.resourceReferenceCount())
    gen.writeBooleanField("logIfAllowed", value.logIfAllowed())
    gen.writeBooleanField("logIfDenied", value.logIfDenied())
    gen.writeEndObject()
  }
}

class RequestContextSerializer() extends JsonSerializer[RequestContext] {
  override def serialize(value: RequestContext, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("clientAddress", value.clientAddress().toString)
    gen.writeObjectField("clientInformation", value.clientInformation)
    gen.writeStringField("connectionId", value.connectionId)
    gen.writeObjectField("header", value.header) //
    gen.writeStringField("listenerName", value.listenerName())
    gen.writeObjectField("principal", value.principal())
    gen.writeStringField("securityProtocol", value.securityProtocol().name())
    gen.writeEndObject()
  }
}

class ClientInformationSerializer() extends JsonSerializer[ClientInformation] {
  override def serialize(value: ClientInformation, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("softwareName", value.softwareName())
    gen.writeStringField("softwareVersion", value.softwareVersion())
    gen.writeEndObject()
  }
}

class KafkaPrincipalSerializer() extends JsonSerializer[KafkaPrincipal] {
  override def serialize(value: KafkaPrincipal, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("principalType", value.getPrincipalType())
    gen.writeStringField("name", value.getName())
    gen.writeEndObject()
  }
}

class RequestHeaderSerializer() extends JsonSerializer[RequestHeader] {
  override def serialize(value: RequestHeader, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeObjectField("name", value.data())
    // Jackson 2.10 does not support writeNumberField for shorts
    gen.writeFieldName("headerVersion")
    gen.writeNumber(value.headerVersion())
    gen.writeEndObject()
  }
}

class RequestHeaderDataSerializer() extends JsonSerializer[RequestHeaderData] {
  override def serialize(value: RequestHeaderData, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("clientId", value.clientId())
    gen.writeNumberField("correlationId", value.correlationId())
    gen.writeNumberField("requestApiKey", value.requestApiKey())
    gen.writeNumberField("requestApiVersion", value.requestApiVersion())
    gen.writeEndObject()
  }
}

object AllowCallable {
  private val requestSerializerModule = new SimpleModule()
    .addSerializer(classOf[ResourcePattern], new ResourcePatternSerializer)
    .addSerializer(classOf[Action], new ActionSerializer)
    .addSerializer(classOf[RequestContext], new RequestContextSerializer)
    .addSerializer(classOf[ClientInformation], new ClientInformationSerializer)
    .addSerializer(classOf[KafkaPrincipal], new KafkaPrincipalSerializer)
    .addSerializer(classOf[RequestHeader], new RequestHeaderSerializer)
    .addSerializer(classOf[RequestHeaderData], new RequestHeaderDataSerializer)
  private val objectMapper = JsonMapper.builder().addModule(requestSerializerModule).addModule(DefaultScalaModule).build()
  private val client = HttpClient.newBuilder.connectTimeout(ofSeconds(5)).build
  private val requestBuilder = HttpRequest.newBuilder.timeout(ofSeconds(5)).header("Content-Type", "application/json")
}
class AllowCallable(request: Request, opaUrl: URI, allowOnError: Boolean) extends Callable[Boolean] with LazyLogging {
  override def call(): Boolean = {
    logger.debug(s"Cache miss, querying OPA for decision")
    val reqJson = AllowCallable.objectMapper.writeValueAsString(request)
    val req = AllowCallable.requestBuilder.uri(opaUrl).POST(BodyPublishers.ofString(reqJson)).build
    logger.debug(s"Querying OPA with object: $reqJson")
    val resp = AllowCallable.client.send(req, BodyHandlers.ofString)
    logger.trace(s"Response code: ${resp.statusCode}, body: ${resp.body}")

    AllowCallable.objectMapper.readTree(resp.body()).at("/result").asBoolean
  }
}

case class Input(requestContext: AuthorizableRequestContext, action: Action)
case class Request(input: Input)
case class CachableRequest(principal: KafkaPrincipal, action: Action, host: String)