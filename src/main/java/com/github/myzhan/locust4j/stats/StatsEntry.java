package com.github.myzhan.locust4j.stats;

import java.util.HashMap;
import java.util.Map;

import com.github.myzhan.locust4j.message.LongIntMap;
import com.github.myzhan.locust4j.utils.Utils;

/**
 * @author myzhan
 */
public class StatsEntry {

    private final String name;
    private final String method;
    private long numRequests;
    private long numFailures;
    private long totalResponseTime;
    private long minResponseTime;
    private long maxResponseTime;
    private LongIntMap numReqsPerSec;
    private LongIntMap numFailPerSec;
    private LongIntMap responseTimes;
    private long totalContentLength;
    private long startTime;
    private long lastRequestTimestamp;

    public StatsEntry(String name) {
        this(name, "");
    }

    public StatsEntry(String name, String method) {
        this.name = name;
        this.method = method;
    }

    public void reset() {
        this.startTime = Utils.currentTimeInSeconds();
        this.numRequests = 0;
        this.numFailures = 0;
        this.totalResponseTime = 0;
        this.responseTimes = new LongIntMap();
        this.minResponseTime = 0;
        this.maxResponseTime = 0;
        this.lastRequestTimestamp = Utils.currentTimeInSeconds();
        this.numReqsPerSec = new LongIntMap();
        this.numFailPerSec = new LongIntMap();
        this.totalContentLength = 0;
    }

    public void log(long responseTime, long contentLength) {
        this.numRequests++;
        this.logTimeOfRequest();
        this.logResponseTime(responseTime);
        this.totalContentLength += contentLength;
    }

    public void logTimeOfRequest() {
        long now = Utils.currentTimeInSeconds();
        this.numReqsPerSec.add(now);
        this.lastRequestTimestamp = now;
    }

    public void logResponseTime(long responseTime) {
        this.totalResponseTime += responseTime;

        if (this.minResponseTime == 0) {
            this.minResponseTime = responseTime;
        }

        if (responseTime < this.minResponseTime) {
            this.minResponseTime = responseTime;
        }

        if (responseTime > this.maxResponseTime) {
            this.maxResponseTime = responseTime;
        }

        long roundedResponseTime;

        if (responseTime < 100) {
            roundedResponseTime = responseTime;
        } else if (responseTime < 1000) {
            roundedResponseTime = Utils.round(responseTime, -1);
        } else if (responseTime < 10000) {
            roundedResponseTime = Utils.round(responseTime, -2);
        } else {
            roundedResponseTime = Utils.round(responseTime, -3);
        }

        this.responseTimes.add(roundedResponseTime);
    }

    public void logError(String error) {
        this.numFailures++;
        this.numFailPerSec.add(Utils.currentTimeInSeconds());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>(13);
        result.put("name", this.name);
        result.put("method", this.method);
        result.put("last_request_timestamp", this.lastRequestTimestamp);
        result.put("start_time", this.startTime);
        result.put("num_requests", this.numRequests);
        // Locust4j doesn't allow None response time for requests like locust.
        // num_none_requests is added to keep compatible with locust.
        result.put("num_none_requests", 0);
        result.put("num_failures", this.numFailures);
        result.put("total_response_time", this.totalResponseTime);
        result.put("max_response_time", this.maxResponseTime);
        result.put("min_response_time", this.minResponseTime);
        result.put("total_content_length", this.totalContentLength);
        result.put("response_times", this.responseTimes);
        result.put("num_reqs_per_sec", this.numReqsPerSec);
        result.put("num_fail_per_sec", this.numFailPerSec);
        return result;
    }

    public Map<String, Object> getStrippedReport() {
        Map<String, Object> report = this.serialize();
        this.reset();
        return report;
    }

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public long getNumRequests() {
        return numRequests;
    }

    public long getNumFailures() {
        return numFailures;
    }

    public long getTotalResponseTime() {
        return totalResponseTime;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public LongIntMap getResponseTimes() {
        return responseTimes;
    }

    public long getTotalContentLength() {
        return totalContentLength;
    }

    public boolean canReport() {
        return getNumRequests() > 0 || getNumFailures() > 0;
    }

}
