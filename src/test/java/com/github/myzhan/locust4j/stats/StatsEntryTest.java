package com.github.myzhan.locust4j.stats;

import java.util.Map;

import com.github.myzhan.locust4j.message.LongIntMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author myzhan
 */
public class StatsEntryTest {

    @Test
    public void TestRoundedResponseTime() {
        StatsEntry entry = new StatsEntry("http");
        entry.reset();
        entry.logResponseTime(99);
        entry.logResponseTime(147);
        entry.logResponseTime(3432);
        entry.logResponseTime(58760);

        LongIntMap responseTimes = entry.getResponseTimes();

        assertThat(responseTimes.get(99L).intValue()).isOne();
        assertThat(responseTimes.get(150L).intValue()).isOne();
        assertThat(responseTimes.get(3400L).intValue()).isOne();
        assertThat(responseTimes.get(59000L).intValue()).isOne();
    }

    @Test
    public void TestGetStrippedReport() {
        StatsEntry entry = new StatsEntry("http", "success");
        entry.reset();

        entry.log(1, 10);
        entry.log(2, 20);
        entry.logError("400 ERROR");

        Map<String, Object> serialized = entry.getStrippedReport();
        assertThat(serialized).containsOnlyKeys(
            "name",
            "method",
            "last_request_timestamp",
            "start_time",
            "num_requests",
            "num_failures",
            "total_response_time",
            "max_response_time",
            "min_response_time",
            "total_content_length",
            "response_times",
            "num_reqs_per_sec",
            "num_fail_per_sec",
            "num_none_requests"
        );

        // getStrippedReport() will call reset()
        assertThat(entry.getNumRequests()).isZero();
        assertThat(entry.getNumFailures()).isZero();

    }
}
