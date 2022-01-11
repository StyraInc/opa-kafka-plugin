package org.openpolicyagent.kafka

object MetricsLabel {
  val NAMESPACE = "opa.authorizer"

  val RESULT_GROUP = "authorization-result"
  val AUTHORIZED_REQUEST_COUNT = "authorized-request-count"
  val UNAUTHORIZED_REQUEST_COUNT = "unauthorized-request-count"

  val REQUEST_HANDLE_GROUP = "request-handle"
  val REQUEST_TO_OPA_COUNT = "request-to-opa-count"
  val CACHE_HIT_RATE = "cache-hit-rate"
  val CACHE_USAGE_PERCENTAGE = "cache-usage-percentage"
}
