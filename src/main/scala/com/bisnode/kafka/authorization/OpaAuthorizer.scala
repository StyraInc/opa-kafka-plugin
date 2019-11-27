package com.bisnode.kafka.authorization

import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.net.{URI, URL}
import java.time.Duration.ofSeconds
import java.util
import java.util.concurrent.{Callable, ExecutionException, TimeUnit}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.google.common.cache.{Cache, CacheBuilder}
import com.typesafe.scalalogging.LazyLogging
import kafka.network.RequestChannel
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Acl, Authorizer, Operation, Resource}
import org.apache.kafka.common.security.auth.KafkaPrincipal

import scala.collection.JavaConverters._

//noinspection NotImplementedCode
class OpaAuthorizer extends Authorizer with LazyLogging {
  private var config: Map[String, String] = Map.empty
  private lazy val opaUrl = new URL(config("opa.authorizer.url")).toURI
  private lazy val allowOnError = config.getOrElse("opa.authorizer.allow.on.error", "false").toBoolean

  private lazy val cache = CacheBuilder.newBuilder
    .initialCapacity(config.getOrElse("opa.authorizer.cache.initial.capacity", "5000").toInt)
    .maximumSize(config.getOrElse("opa.authorizer.cache.maximum.size", "50000").toInt)
    .expireAfterWrite(config.getOrElse("opa.authorizer.cache.expire.after.seconds", "3600").toInt, TimeUnit.SECONDS)
    .build
    .asInstanceOf[Cache[Request, Boolean]]

  override def authorize(session: RequestChannel.Session, operation: Operation, resource: Resource): Boolean = {
    val request = Request(Input(session, operation, resource))
    try cache.get(request, new AllowCallable(request, opaUrl, allowOnError))
    catch {
      case e: ExecutionException =>
        logger.warn("Exception in decision retrieval", e.getCause)
        logger.trace("Exception trace", e)
        allowOnError
    }
  }

  override def configure(configs: util.Map[String, _]): Unit = {
    logger.debug(s"Call to configure() with config $configs")
    config = configs.asScala.mapValues(_.asInstanceOf[String]).toMap
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

object AllowCallable {
  private val objectMapper = (new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)
  private val client = HttpClient.newBuilder.connectTimeout(ofSeconds(5)).build
  private val requestBuilder = HttpRequest.newBuilder.timeout(ofSeconds(5)).header("Content-Type", "application/json")
}
class AllowCallable(request: Request, opaUrl: URI, allowOnError: Boolean) extends Callable[Boolean] with LazyLogging {
  override def call(): Boolean = {
    logger.debug("Cache miss, querying OPA for decision")
    val reqJson = AllowCallable.objectMapper.writeValueAsString(request)
    val req = AllowCallable.requestBuilder.uri(opaUrl).POST(BodyPublishers.ofString(reqJson)).build

    logger.trace(s"Querying OPA for object: $reqJson")
    val resp = AllowCallable.client.send(req, BodyHandlers.ofString)
    logger.trace(s"Response code: ${resp.statusCode}, body: ${resp.body}")

    AllowCallable.objectMapper.readTree(resp.body()).at("/result").asBoolean
  }
}

case class Input(session: Session, operation: Operation, resource: Resource)
case class Request(input: Input)
