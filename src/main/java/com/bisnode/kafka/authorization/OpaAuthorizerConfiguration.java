package com.bisnode.kafka.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

class OpaAuthorizerConfiguration {

    private static final Logger _logger = LoggerFactory.getLogger(OpaAuthorizerConfiguration.class);

    private static final String OPA_AUTHORIZER_URL_CONFIG = "opa.authorizer.url";
    private static final String OPA_AUTHORIZER_ALLOW_ON_ERROR_CONFIG = "opa.authorizer.allow.on.error";

    private final URL opaUrl;
    private final boolean allowOnError;

    OpaAuthorizerConfiguration(Map<String, ?> config) {
        _logger.trace("Configuration object initialized: {}", config);

        try {
            opaUrl = new URL((String) config.get(OPA_AUTHORIZER_URL_CONFIG));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        allowOnError = Boolean.parseBoolean((String) config.get(OPA_AUTHORIZER_ALLOW_ON_ERROR_CONFIG));
    }

    URL getOpaUrl() {
        return opaUrl;
    }

    boolean allowOnError() {
        return allowOnError;
    }
}
