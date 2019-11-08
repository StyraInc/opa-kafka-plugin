package com.bisnode.kafka.authorization

import java.io.{IOException, InputStreamReader}
import java.net.{HttpURLConnection, ProtocolException, URL}
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.{Callable, ExecutionException, TimeUnit}

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.common.base.Charsets
import com.google.common.cache.{Cache, CacheBuilder}
import kafka.network.RequestChannel
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Acl, Authorizer, Operation, Resource}
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class Input(val session: Session, val operation: Operation, val resource: Resource)
class Request(val input: Input)

//noinspection NotImplementedCode
class OpaAuthorizer extends Authorizer {
  private val logger = LoggerFactory.getLogger(classOf[OpaAuthorizer])

  private lazy val _cache = CacheBuilder.newBuilder
    .initialCapacity(_opaAuthorizerConfiguration.cacheInitialCapacity)
    .maximumSize(_opaAuthorizerConfiguration.cacheMaxSize)
    .expireAfterWrite(_opaAuthorizerConfiguration.cacheExpireAfterSeconds, TimeUnit.SECONDS)
    .build()
    .asInstanceOf[Cache[Request, Boolean]]

  private val _objectMapper = new ObjectMapper() with ScalaObjectMapper
  _objectMapper.registerModule(DefaultScalaModule)

  // To be populated by the configure method
  private var _opaAuthorizerConfiguration : OpaAuthorizerConfig = _

  override def authorize(session: RequestChannel.Session, operation: Operation, resource: Resource): Boolean = {
    if (_opaAuthorizerConfiguration == null) {
      logger.warn("OPA configuration not loaded yet")
      return false
    }

    val request = new Request(new Input(session, operation, resource))
    try _cache.get(request, new AllowCallable(request))
    catch {
      case e: ExecutionException =>
        logger.warn("Exception in cache retrieval", e)
        _opaAuthorizerConfiguration.allowOnError
    }
  }

  private class AllowCallable(request: Request) extends Callable[Boolean] {
    override def call(): Boolean = allow(request)
  }

  private def allow(request: Request): Boolean = {
    assert(_opaAuthorizerConfiguration != null)
    try {
      val conn = _opaAuthorizerConfiguration.opaUrl.openConnection.asInstanceOf[HttpURLConnection]
      conn.setDoOutput(true)
      conn.setRequestMethod("POST")
      conn.setRequestProperty("Content-Type", "application/json")
      val json = _objectMapper.writeValueAsString(request)

      val os = conn.getOutputStream
      try {
        os.write(json.getBytes(StandardCharsets.UTF_8))
        os.flush()
      } finally if (os != null) os.close()
      logger.trace(s"Request body: $json")
      logger.trace(s"Response code: ${conn.getResponseCode}")
      return _objectMapper.readTree(new InputStreamReader(conn.getInputStream, Charsets.UTF_8)).at("/result").asBoolean
    } catch {
      case e: JsonProcessingException => logger.warn("Error processing JSON", e)
      case e: ProtocolException => logger.warn("Protocol exception", e)
      case e: IOException => logger.warn("IO exception when connecting to OPA", e)
    }
    _opaAuthorizerConfiguration.allowOnError
  }

  override def configure(configs: util.Map[String, _]): Unit = {
    _opaAuthorizerConfiguration = new OpaAuthorizerConfig(configs.asScala.toMap)
  }

  // None of the below needs implementations here
  override def addAcls(acls: Set[Acl], resource: Resource): Unit = ???
  override def removeAcls(acls: Set[Acl], resource: Resource): Boolean = ???
  override def removeAcls(resource: Resource): Boolean = ???
  override def getAcls(resource: Resource): Set[Acl] = ???
  override def getAcls(principal: KafkaPrincipal): Map[Resource, Set[Acl]] = ???
  override def getAcls(): Map[Resource, Set[Acl]] = ???
  override def close(): Unit = ???
}

object OpaAuthorizerConfig {
  private val URL = "opa.authorizer.url"
  private val ALLOW_ON_ERROR = "opa.authorizer.allow.on.error"
  private val CACHE_INITIAL_CAPACITY = "opa.authorizer.cache.initial.capacity"
  private val CACHE_MAXIMUM_SIZE = "opa.authorizer.cache.maximum.size"
  private val CACHE_EXPIRE_AFTER_SECONDS = "opa.authorizer.cache.expire.after.seconds"

  final private val CACHE_INITIAL_CAPACITY_DEFAULT = "64"
  final private val CACHE_MAXIMUM_SIZE_DEFAULT = "256"
  final private val CACHE_EXPIRE_AFTER_SECONDS_DEFAULT = "3600"
}

class OpaAuthorizerConfig private[authorization](val config: Map[String, _]) {
  private val logger = LoggerFactory.getLogger(classOf[OpaAuthorizerConfig])
  logger.trace("Configuration object initialized: {}", config)

  final private val _opaUrl = new URL(config.getOrElse(OpaAuthorizerConfig.URL, "").asInstanceOf[String])
  final private val _allowOnError = config.getOrElse(OpaAuthorizerConfig.ALLOW_ON_ERROR, "false")
    .asInstanceOf[String].toBoolean

  final private val _cacheInitialCapacity = config.getOrElse(
    OpaAuthorizerConfig.CACHE_INITIAL_CAPACITY,
    OpaAuthorizerConfig.CACHE_INITIAL_CAPACITY_DEFAULT
  ).asInstanceOf[String].toInt

  final private val _cacheMaxSize = config.getOrElse(
    OpaAuthorizerConfig.CACHE_MAXIMUM_SIZE,
    OpaAuthorizerConfig.CACHE_MAXIMUM_SIZE_DEFAULT
  ).asInstanceOf[String].toInt

  final private val _cacheExpireAfterSeconds = config.getOrElse(
    OpaAuthorizerConfig.CACHE_EXPIRE_AFTER_SECONDS,
    OpaAuthorizerConfig.CACHE_EXPIRE_AFTER_SECONDS_DEFAULT
  ).asInstanceOf[String].toInt

  private[authorization] def opaUrl: URL = _opaUrl
  private[authorization] def allowOnError = _allowOnError
  private[authorization] def cacheInitialCapacity = _cacheInitialCapacity
  private[authorization] def cacheMaxSize = _cacheMaxSize
  private[authorization] def cacheExpireAfterSeconds = _cacheExpireAfterSeconds
}
