package com.bisnode.kafka.authorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kafka.network.RequestChannel;
import kafka.security.auth.Operation;
import kafka.security.auth.Resource;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
@Immutable
public class Request {
    private final RequestChannel.Session _session;
    private final Operation _operation;
    private final Resource _resource;

    @JsonCreator
    Request(
            @JsonProperty("session") RequestChannel.Session session,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("resource") Resource resource
    ) {
        _operation = operation;
        _resource = resource;
        _session = session;
    }

    public RequestChannel.Session getSession() {
        return _session;
    }

    public Operation getOperation() {
        return _operation;
    }

    public Resource getResource() {
        return _resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return _session.equals(request.getSession()) &&
                _operation.equals(request.getOperation()) &&
                _resource.equals(request.getResource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(_session, _operation, _resource);
    }
}
