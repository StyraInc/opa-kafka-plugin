package com.bisnode.kafka.authorization;

@FunctionalInterface
public interface OpaAdapter {

    String sendQuery(String request);

}
