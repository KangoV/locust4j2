package com.github.myzhan.locust4j.stats;

import com.github.myzhan.locust4j.utils.ImmutableStyle;
import org.immutables.value.Value;

import java.util.function.UnaryOperator;

/**
 * @author myzhan
 */
@Value.Immutable
@ImmutableStyle
public interface RequestSuccess {

    class Builder extends RequestSuccessImpl.Builder {}

    static RequestSuccess create(UnaryOperator<Builder> spec) { return spec.apply(builder()).build(); }
    static RequestSuccess createRequestFailure(UnaryOperator<Builder> spec) { return create(spec); }
    static RequestSuccess.Builder builder(UnaryOperator<Builder> spec) { return spec.apply(builder()); }
    static RequestSuccess.Builder builder() { return new Builder(); }

    String getRequestType();
    String getName();
    long getResponseTime();
    long getContentLength();

}
