package com.github.myzhan.locust4j.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.github.myzhan.locust4j.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stats collects test results from reportSuccessQueue and reportFailureQueue and reports to Runner every 3 seconds.
 *
 * @author myzhan
 */
public class Stats implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Stats.class);

    private Map<String, StatsEntry> entries;
    private Map<String, StatsError> errors;
    private StatsEntry total;

    private final ConcurrentLinkedQueue<RequestSuccess> reportSuccessQueue;
    private final ConcurrentLinkedQueue<RequestFailure> reportFailureQueue;
    private final ConcurrentLinkedQueue<Boolean> clearStatsQueue;
    private final ConcurrentLinkedQueue<Boolean> timeToReportQueue;
    private final BlockingQueue<Map<String, Object>> messageToRunnerQueue;

    private ExecutorService threadPool;
    private final AtomicInteger threadNumber;
    private final Object lock = new Object();

    /**
     * Probably, you don't need to create Stats unless you are writing unit tests.
     */
    public Stats() {
        reportSuccessQueue = new ConcurrentLinkedQueue<>();
        reportFailureQueue = new ConcurrentLinkedQueue<>();
        clearStatsQueue = new ConcurrentLinkedQueue<>();
        timeToReportQueue = new ConcurrentLinkedQueue<>();
        messageToRunnerQueue = new LinkedBlockingDeque<>();
        threadNumber = new AtomicInteger();

        this.entries = new HashMap<>(8);
        this.errors = new HashMap<>(8);
        this.total = new StatsEntry("Total");
        this.total.reset();
    }

    public static Stats getInstance() {
        return StatsInstanceHolder.INSTANCE;
    }

    public void start() {
        threadPool = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("locust4j-stats#%d#", threadNumber.getAndIncrement()));
                return thread;
            }
        });

        threadPool.submit(new StatsTimer(this));
        threadPool.submit(this);
    }

    public void stop() {
        threadPool.shutdownNow();
    }


    private Failures failures = new Failures();
    private Successes successes = new Successes();

    public Failures failures() {
        return failures;
    }
    public Successes successes() {
        return successes;
    }



    public Queue<Boolean> getClearStatsQueue() {
        return this.clearStatsQueue;
    }

    public BlockingQueue<Map<String, Object>> getMessageToRunnerQueue() {
        return this.messageToRunnerQueue;
    }

    public void wakeMeUp() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void sleep() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    /**
     * User code reports successful and failed records to Stats.
     * If the sending speed is too fast, single-threaded stats may be a bottleneck and reports wrong RPS.
     */
    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "stats");

        while (true) {

            boolean allEmpty = true;

            RequestSuccess successMessage = reportSuccessQueue.poll();
            if (successMessage != null) {
                this.logRequest(successMessage.getRequestType(), successMessage.getName(),
                    successMessage.getResponseTime()
                    , successMessage.getContentLength());
                allEmpty = false;
            }

            RequestFailure failureMessage = reportFailureQueue.poll();
            if (null != failureMessage) {
                this.logRequest(failureMessage.getRequestType(), failureMessage.getName(), failureMessage.getResponseTime(), 0);
                this.logError(failureMessage.getRequestType(), failureMessage.getName(), failureMessage.getError());
                allEmpty = false;
            }

            Boolean needToClearStats = clearStatsQueue.poll();
            if (null != needToClearStats && needToClearStats) {
                this.clearAll();
                allEmpty = false;
            }

            Boolean timeToReport = timeToReportQueue.poll();
            if (null != timeToReport) {
                Map<String, Object> data = this.collectReportData();
                messageToRunnerQueue.add(data);
                allEmpty = false;
            }

            if (allEmpty) {
                // if all the queues are empty, stats will sleep to avoid endless loop.
                this.sleep();
            }
        }
    }

    protected StatsEntry getTotal() {
        return this.total;
    }

    protected StatsEntry get(String name, String method) {
        StatsEntry entry = this.entries.get(name + method);
        if (null == entry) {
            entry = new StatsEntry(name, method);
            entry.reset();
            this.entries.put(name + method, entry);
        }
        return entry;
    }

    public void logRequest(String method, String name, long responseTime, long contentLength) {
        this.total.log(responseTime, contentLength);
        this.get(name, method).log(responseTime, contentLength);
    }

    public void logError(String method, String name, String error) {
        this.total.logError(error);
        this.get(name, method).logError(error);

        String key = Utils.md5(method, name, error);
        if (null == key) {
            key = method + name + error;
        }
        StatsError entry = this.errors.get(key);
        if (null == entry) {
            entry = new StatsError(name, method, error);
            this.errors.put(key, entry);
        }
        entry.occured();
    }

    public void clearAll() {
        this.total = new StatsEntry("Total");
        this.total.reset();
        this.entries = new HashMap<>(8);
        this.errors = new HashMap<>(8);
    }

    protected List<Map<String, Object>> serializeStats() {
        return this.entries.values().stream()
            .filter(StatsEntry::canReport)
            .map(StatsEntry::getStrippedReport)
            .toList();
    }

    public Map<String, Map<String, Object>> serializeErrors() {
        return this.errors.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,v -> v.getValue().toMap()));
    }

    protected Map<String, Object> collectReportData() {
        var data = Map.of(
            "stats",       this.serializeStats(),
            "stats_total", this.total.getStrippedReport(),
            "errors",      this.serializeErrors()
        );
        errors.clear();
        return data;
    }

    private static class StatsInstanceHolder {
        private static final Stats INSTANCE = new Stats();
    }

    private static class StatsTimer implements Runnable {
        private static final int SLAVE_REPORT_INTERVAL = 3000;
        protected Stats stats;

        private StatsTimer(Stats stats) {
            this.stats = stats;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "stats-timer");

            while (true) {
                try {
                    Thread.sleep(SLAVE_REPORT_INTERVAL);
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
                stats.timeToReportQueue.offer(true);
                stats.wakeMeUp();
            }
        }
    }

    public class Failures {
        public void offer(RequestFailure requestFailure) {
            reportFailureQueue.offer(requestFailure);
        }
        public void offer(UnaryOperator<RequestFailure.Builder> spec) {
            offer(RequestFailure.create(spec));
        }
        public void add(RequestFailure requestFailure) {
            reportFailureQueue.add(requestFailure);
        }
        public void add(UnaryOperator<RequestFailure.Builder> spec) {
            add(RequestFailure.create(spec));
        }
        public RequestFailure pop() {
            return reportFailureQueue.poll();
        }
        public void clear() {
            reportFailureQueue.clear();
        }
    }

    public class Successes {
        public void offer(RequestSuccess requestSuccess) {
            reportSuccessQueue.offer(requestSuccess);
        }
        public void offer(UnaryOperator<RequestSuccess.Builder> spec) {
            offer(RequestSuccess.create(spec));
        }
        public void add(RequestSuccess requestSuccess) {
            reportSuccessQueue.add(requestSuccess);
        }
        public void add(UnaryOperator<RequestSuccess.Builder> spec) {
            add(RequestSuccess.create(spec));
        }
        public RequestSuccess pop() {
            return reportSuccessQueue.poll();
        }
    }

}