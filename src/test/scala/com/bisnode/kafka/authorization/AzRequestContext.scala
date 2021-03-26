package com.bisnode.kafka.authorization

import java.net.InetAddress
import org.apache.kafka.common.security.auth.{KafkaPrincipal, SecurityProtocol}
import org.apache.kafka.server.authorizer.{AuthorizableRequestContext, AuthorizerServerInfo}
import org.apache.kafka.common.{Endpoint, ClusterResource}
import java.util.Collection

case class AzRequestContext(
  clientId: String,
  requestType: Int,
  listenerName: String,
  clientAddress: InetAddress,
  principal: KafkaPrincipal,
  securityProtocol: SecurityProtocol,
  correlationId: Int,
  requestVersion: Int) extends AuthorizableRequestContext

case class AzServerInfo(
  brokerId: Int,
  clusterResource: ClusterResource,
  endpoints: Collection[Endpoint],
  interBrokerEndpoint: Endpoint) extends AuthorizerServerInfo
