package com.github.myzhan.locust4j.test;

import com.github.myzhan.locust4j.stats.StatsEntry;
import org.assertj.core.api.AbstractAssert;

import java.util.Objects;

public class StatsEntryAssert extends AbstractAssert<StatsEntryAssert, StatsEntry> {

    public StatsEntryAssert(StatsEntry actual) {
        super(actual, StatsEntryAssert.class);
    }

    public static StatsEntryAssert assertThat(StatsEntry actual) {
        return new StatsEntryAssert(actual);
    }

    public StatsEntryAssert hasName(String name) {
        isNotNull();
        if (!Objects.equals(actual.getName(), name)) {
            failWithMessage("Expected StatsEntry's name to be <%s> but was <%s>", name, actual.getName());
        }
        return this;
    }

    public StatsEntryAssert hasMethod(String method) {
        isNotNull();
        if (!Objects.equals(actual.getMethod(), method)) {
            failWithMessage("Expected StatsEntry's method to be <%s> but was <%s>", method, actual.getMethod());
        }
        return this;
    }

    public StatsEntryAssert hasMinResponseTime(long minResponseTime) {
        isNotNull();
        if (actual.getMinResponseTime() !=  minResponseTime) {
            failWithMessage("Expected StatsEntry's minResponseTime to be <%s> but was <%s>", minResponseTime, actual.getMinResponseTime());
        }
        return this;
    }

    public StatsEntryAssert hasMaxResponseTime(long maxResponseTime) {
        isNotNull();
        if (actual.getMaxResponseTime() !=  maxResponseTime) {
            failWithMessage("Expected StatsEntry's maxMinResponseTime to be <%s> but was <%s>", maxResponseTime, actual.getMaxResponseTime());
        }
        return this;
    }

    public StatsEntryAssert hasTotalResponseTime(long totalResponseTime) {
        isNotNull();
        if (actual.getTotalResponseTime() !=  totalResponseTime) {
            failWithMessage("Expected StatsEntry's totalResponseTime to be <%s> but was <%s>", totalResponseTime, actual.getTotalResponseTime());
        }
        return this;
    }

    public StatsEntryAssert hasTotalContentLength(long totalContentLength) {
        isNotNull();
        if (actual.getTotalContentLength() !=  totalContentLength) {
            failWithMessage("Expected StatsEntry's totalContentLength to be <%s> but was <%s>", totalContentLength, actual.getTotalContentLength());
        }
        return this;
    }

    public StatsEntryAssert hasNumRequests(long numRequests) {
        isNotNull();
        if (actual.getNumRequests() !=  numRequests) {
            failWithMessage("Expected StatsEntry's numRequests to be <%s> but was <%s>", numRequests, actual.getNumRequests());
        }
        return this;
    }

    public StatsEntryAssert hasNumFailures(long numFailures) {
        isNotNull();
        if (actual.getNumFailures() !=  numFailures) {
            failWithMessage("Expected StatsEntry's numFailures to be <%s> but was <%s>", numFailures, actual.getNumFailures());
        }
        return this;
    }

}
