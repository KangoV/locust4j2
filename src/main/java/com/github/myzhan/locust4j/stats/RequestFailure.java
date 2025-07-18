package com.github.myzhan.locust4j.stats;

import com.github.myzhan.locust4j.utils.ImmutableStyle;
import org.immutables.value.Value;

import java.util.function.UnaryOperator;

/**
 * @author myzhan
 */
@Value.Immutable
@ImmutableStyle
public interface RequestFailure {

    class Builder extends RequestFailureImpl.Builder {}

    static RequestFailure create(UnaryOperator<Builder> spec) { return spec.apply(builder()).build(); }
    static RequestFailure createRequestFailure(UnaryOperator<Builder> spec) { return create(spec); }
    static Builder builder(UnaryOperator<Builder> spec) { return spec.apply(builder()); }
    static Builder builder() { return new Builder(); }

    String getRequestType();
    String getName();
    long getResponseTime();
    String getError();

}
