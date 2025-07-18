package com.github.myzhan.locust4j.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * @author myzhan
 */
public class StatsError {
    protected String name;
    protected String method;
    protected String error;
    protected long occurrences;

    protected StatsError(String name, String method, String error) {
        this.name = name;
        this.method = method;
        this.error = error;
    }

    protected void occured() {
        this.occurrences++;
    }

    protected Map<String, Object> toMap() {
        return Map.of(
            "name",        this.name,
            "method",      this.method,
            "error",       this.error,
            "occurrences", this.occurrences);
    }
}
