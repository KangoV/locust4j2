package com.github.myzhan.locust4j.message;

import java.util.*;
import java.util.function.UnaryOperator;

import com.github.myzhan.locust4j.utils.ImmutableStyle;
import org.immutables.value.Value;

/**
 * @author vrajat
 */
@ImmutableStyle
@Value.Immutable
public abstract class Message {

    public static class Builder extends MessageImpl.Builder {}

    public static Message create(UnaryOperator<Builder> spec) { return spec.apply(builder()).build(); }
    public static Message createMessage(UnaryOperator<Builder> spec) { return create(spec); }
    public static Builder builder(UnaryOperator<Builder> spec) { return spec.apply(builder()); }
    public static Builder builder() { return new Builder(); }

    public static final Null NULL = Null.TYPE;

    public abstract String type();
    public abstract Optional<String> nodeId();
    public abstract Map<String, Object> data();

    @Value.Default
    public int version() {
        return -1;
    }

    @Value.Lazy
    public boolean hasData() {
        return !data().isEmpty();
    }

    @Override
    @Value.Lazy
    public String toString() {
        return String.format("%s-%s-%s", nodeId().orElse("nodeId?"), type(), data());
    }

}