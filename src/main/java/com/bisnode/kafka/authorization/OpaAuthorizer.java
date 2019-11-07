package com.bisnode.kafka.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import kafka.network.RequestChannel;
import kafka.security.auth.Acl;
import kafka.security.auth.Authorizer;
import kafka.security.auth.Operation;
import kafka.security.auth.Resource;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.immutable.Set;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpaAuthorizer implements Authorizer {

    private static final Logger _logger = LoggerFactory.getLogger(OpaAuthorizer.class);
    private static final ObjectMapper _objectMapper = new ObjectMapper();
    private static final Cache<Request, Boolean> _cache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(1024L)
            .expireAfterWrite(60L, TimeUnit.MINUTES)
            .build();

    @Nullable
    private OpaAuthorizerConfiguration _opaAuthorizerConfiguration;

    @Override
    public boolean authorize(RequestChannel.Session session, Operation operation, Resource resource) {
        if (_opaAuthorizerConfiguration == null) {
            _logger.warn("OPA configuration not loaded yet");
            return false;
        }

        Request request = new Request(session, operation, resource);
        try {
            return _cache.get(request, () -> allow(request));
        } catch (ExecutionException e) {
            _logger.warn("Exception in cache retrieval", e);
            return _opaAuthorizerConfiguration.allowOnError();
        }
    }

    private boolean allow(Request request) {
        assert _opaAuthorizerConfiguration != null;
        try {
            HttpURLConnection conn = (HttpURLConnection) _opaAuthorizerConfiguration.getOpaUrl().openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String json = _objectMapper.writeValueAsString(request);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            _logger.trace("Request body: {}", json);
            _logger.trace("Response code: {}", conn.getResponseCode());

            return _objectMapper.readTree(new InputStreamReader(conn.getInputStream(), Charsets.UTF_8))
                    .at("result")
                    .asBoolean();

        } catch (JsonProcessingException e) {
            _logger.warn("Error processing JSON", e);
        } catch (ProtocolException e) {
            _logger.warn("Protocol exception", e);
        } catch (IOException e) {
            _logger.warn("IO exception when connecting to OPA", e);
        }

        return _opaAuthorizerConfiguration.allowOnError();
    }

    @Override
    public void addAcls(scala.collection.immutable.Set<Acl> acls, Resource resource) {

    }

    @Override
    public boolean removeAcls(scala.collection.immutable.Set<Acl> acls, Resource resource) {
        return false;
    }

    @Override
    public boolean removeAcls(Resource resource) {
        return false;
    }

    @Override
    @Nullable
    public Set<Acl> getAcls(Resource resource) {
        return null;
    }

    @Override
    @Nullable
    public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls(KafkaPrincipal principal) {
        return null;
    }

    @Override
    @Nullable
    public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> config) {
        _opaAuthorizerConfiguration = new OpaAuthorizerConfiguration(config);
    }
}
