package com.erp.rag.supabase.pool;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Connection Pool Metrics Validation Tests
 * 
 * Story 1.3 – AC6: Verify connection pooling for vector workloads.
 * 
 * Tests cover:
 * - Pool sizing configuration (min 2, max 10)
 * - Metric exposure (active, idle, pending, total connections)
 * - Concurrent load behavior
 * - Connection acquisition latency
 * - Leak detection threshold
 */
@Testcontainers
@DisplayName("AC6: Connection Pool Metrics for Vector Workloads")
class ConnectionPoolMetricsTest {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPoolMetricsTest.class);

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private static HikariDataSource dataSource;
    private static io.micrometer.core.instrument.simple.SimpleMeterRegistry meterRegistry;

    static {
        // Initialize test datasource with HikariCP pool configuration (AC6)
        postgres.start();
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(10);
        dataSource.setConnectionTimeout(5000);
        dataSource.setLeakDetectionThreshold(10000);
        dataSource.setPoolName("VectorWorkloadPool");

        // Initialize MeterRegistry for metrics validation
        meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        dataSource.setMetricRegistry(meterRegistry);
    }

    @Test
    @DisplayName("Should configure pool with AC6 sizing requirements (min=2, max=10)")
    void shouldConfigurePoolWithCorrectSizing() throws Exception {
        // Given: HikariCP pool configured for vector workloads
        assertThat(dataSource).isNotNull();
        HikariDataSource hikariDataSource = dataSource;

        // When: Query pool configuration
        int minIdle = hikariDataSource.getMinimumIdle();
        int maxPool = hikariDataSource.getMaximumPoolSize();
        long connectionTimeout = hikariDataSource.getConnectionTimeout();
        long leakDetectionThreshold = hikariDataSource.getLeakDetectionThreshold();
        String poolName = hikariDataSource.getPoolName();

        // Then: AC6 - Pool sized for vector workloads (lightweight queries)
        assertThat(minIdle).isEqualTo(2).as("Minimum idle connections should be 2 for baseline availability");
        assertThat(maxPool).isEqualTo(10).as("Maximum pool size should be 10 for concurrent vector queries");
        assertThat(connectionTimeout).isEqualTo(5000)
                .as("Connection timeout should be 5 seconds (aggressive for fast fail)");
        assertThat(leakDetectionThreshold).isEqualTo(10000).as("Leak detection threshold should be 10 seconds");
        assertThat(poolName).isEqualTo("VectorWorkloadPool");

        log.info("✓ Pool configuration validated: min={}, max={}, timeout={}ms, leak={}ms",
                minIdle, maxPool, connectionTimeout, leakDetectionThreshold);
    }

    @Test
    @DisplayName("Should expose HikariCP metrics via MeterRegistry")
    void shouldExposeHikariCPMetrics() throws Exception {
        // Given: Spring Boot Actuator with HikariCP metrics enabled
        assertThat(meterRegistry).isNotNull().as("MeterRegistry should be available for metrics collection");

        // When: Acquire a connection to activate pool
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(1);
        }

        // Then: HikariCP metrics should be registered
        var activeConnections = meterRegistry.find("hikaricp.connections.active")
                .tag("pool", "VectorWorkloadPool")
                .gauge();
        var idleConnections = meterRegistry.find("hikaricp.connections.idle")
                .tag("pool", "VectorWorkloadPool")
                .gauge();
        var totalConnections = meterRegistry.find("hikaricp.connections")
                .tag("pool", "VectorWorkloadPool")
                .gauge();
        var pendingThreads = meterRegistry.find("hikaricp.connections.pending")
                .tag("pool", "VectorWorkloadPool")
                .gauge();

        assertThat(activeConnections).isNotNull().as("Active connections metric should exist");
        assertThat(idleConnections).isNotNull().as("Idle connections metric should exist");
        assertThat(totalConnections).isNotNull().as("Total connections metric should exist");
        assertThat(pendingThreads).isNotNull().as("Pending threads metric should exist");

        log.info("✓ HikariCP metrics exposed: active={}, idle={}, total={}, pending={}",
                activeConnections.value(), idleConnections.value(),
                totalConnections.value(), pendingThreads.value());
    }

    @Test
    @DisplayName("Should handle concurrent vector queries within pool limits")
    void shouldHandleConcurrentVectorQueries() throws Exception {
        // Given: 8 concurrent queries (below max pool size of 10)
        HikariDataSource hikariDataSource = dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
        int concurrentQueries = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentQueries);
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentQueries);
        List<Long> acquisitionTimes = new ArrayList<>();

        // When: Simulate concurrent vector similarity queries
        for (int i = 0; i < concurrentQueries; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Synchronized start
                    long startTime = System.currentTimeMillis();
                    try (Connection conn = dataSource.getConnection()) {
                        long acquisitionTime = System.currentTimeMillis() - startTime;
                        synchronized (acquisitionTimes) {
                            acquisitionTimes.add(acquisitionTime);
                        }
                        // Simulate vector query latency (100ms avg)
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    log.error("Connection acquisition failed", e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: All queries should complete within pool capacity
        assertThat(completed).isTrue().as("All concurrent queries should complete within 10 seconds");
        assertThat(acquisitionTimes).hasSize(concurrentQueries);

        long maxAcquisitionTime = acquisitionTimes.stream().max(Long::compare).orElse(0L);
        long avgAcquisitionTime = (long) acquisitionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        int peakActive = poolMXBean.getActiveConnections();
        int peakIdle = poolMXBean.getIdleConnections();
        int peakTotal = poolMXBean.getTotalConnections();

        assertThat(maxAcquisitionTime).isLessThan(1000)
                .as("P99 connection acquisition should be <1s under normal load");
        assertThat(avgAcquisitionTime).isLessThan(100)
                .as("Average connection acquisition should be <100ms (pool warmup threshold)");
        assertThat(peakTotal).isLessThanOrEqualTo(10).as("Peak total connections should not exceed max pool size");

        log.info(
                "✓ Concurrent load test: {} queries completed | Acquisition: avg={}ms, max={}ms | Pool: active={}, idle={}, total={}",
                concurrentQueries, avgAcquisitionTime, maxAcquisitionTime, peakActive, peakIdle, peakTotal);
    }

    @Test
    @DisplayName("Should maintain minimum idle connections during idle periods")
    void shouldMaintainMinimumIdleConnections() throws Exception {
        // Given: Pool with min idle = 2
        HikariDataSource hikariDataSource = dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        // When: Wait for pool to stabilize (idle timeout = 10 minutes by default)
        Thread.sleep(1000); // Wait 1 second for pool warmup

        // Then: Idle connections should be >= min idle
        int idleConnections = poolMXBean.getIdleConnections();
        int totalConnections = poolMXBean.getTotalConnections();
        int activeConnections = poolMXBean.getActiveConnections();

        assertThat(idleConnections).isGreaterThanOrEqualTo(2)
                .as("Pool should maintain minimum 2 idle connections for instant availability");
        assertThat(totalConnections).isEqualTo(idleConnections + activeConnections)
                .as("Total connections should equal idle + active");

        log.info("✓ Idle period validation: idle={}, active={}, total={}", idleConnections, activeConnections,
                totalConnections);
    }

    @Test
    @DisplayName("Should track connection wait time under saturation")
    void shouldTrackConnectionWaitTimeUnderSaturation() throws Exception {
        // Given: 12 concurrent requests (exceeds max pool size of 10)
        int concurrentRequests = 12;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentRequests);
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        List<Long> waitTimes = new ArrayList<>();

        // When: Saturate the pool (hold connections for 200ms each)
        for (int i = 0; i < concurrentRequests; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    long startTime = System.currentTimeMillis();
                    try (Connection conn = dataSource.getConnection()) {
                        long waitTime = System.currentTimeMillis() - startTime;
                        synchronized (waitTimes) {
                            waitTimes.add(waitTime);
                        }
                        Thread.sleep(200); // Hold connection for 200ms
                    }
                } catch (Exception e) {
                    log.warn("Connection acquisition timeout (expected under saturation): {}", e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: Pending requests should experience wait times
        assertThat(completed).isTrue().as("All requests should complete (some after waiting for pool availability)");

        long maxWaitTime = waitTimes.stream().max(Long::compare).orElse(0L);
        long avgWaitTime = (long) waitTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long waitingRequests = waitTimes.stream().filter(t -> t > 50).count();

        assertThat(maxWaitTime).isLessThan(5000)
                .as("P99 wait time should be <5s (connection-timeout configured value)");
        assertThat(waitingRequests).isGreaterThan(0)
                .as("At least some requests should experience wait time due to pool saturation");

        log.info("✓ Pool saturation test: {}/{} requests waited | Wait times: avg={}ms, max={}ms",
                waitingRequests, concurrentRequests, avgWaitTime, maxWaitTime);
    }
}
