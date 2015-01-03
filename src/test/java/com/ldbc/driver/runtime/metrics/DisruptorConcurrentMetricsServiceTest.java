package com.ldbc.driver.runtime.metrics;

import com.ldbc.driver.Operation;
import com.ldbc.driver.WorkloadException;
import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.temporal.SystemTimeSource;
import com.ldbc.driver.temporal.TimeSource;
import com.ldbc.driver.util.csv.SimpleCsvFileWriter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery2;
import com.ldbc.driver.workloads.ldbc.snb.interactive.db.DummyLdbcSnbInteractiveOperationInstances;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DisruptorConcurrentMetricsServiceTest {
    private TimeSource timeSource = new SystemTimeSource();

    @Test
    public void shouldNotAcceptOperationResultsAfterShutdownWhenBlockingQueueIsUsed() throws WorkloadException, MetricsCollectionException {
        ConcurrentErrorReporter errorReporter = new ConcurrentErrorReporter();
        SimpleCsvFileWriter csvResultsLogWriter = null;
        Map<Integer, Class<? extends Operation<?>>> operationTypeToClassMapping = new HashMap<>();
        operationTypeToClassMapping.put(LdbcQuery1.TYPE, LdbcQuery1.class);
        operationTypeToClassMapping.put(LdbcQuery2.TYPE, LdbcQuery2.class);
        ConcurrentMetricsService metricsService = new DisruptorConcurrentMetricsService(
                timeSource,
                errorReporter,
                TimeUnit.MILLISECONDS,
                DisruptorConcurrentMetricsService.DEFAULT_HIGHEST_EXPECTED_RUNTIME_DURATION_AS_NANO,
                csvResultsLogWriter,
                operationTypeToClassMapping
        );
        metricsService.shutdown();
        boolean exceptionThrown = false;
        try {
            shouldReturnCorrectMeasurements(metricsService);
        } catch (MetricsCollectionException e) {
            exceptionThrown = true;
        }
        assertThat(exceptionThrown, is(true));
    }

    @Test
    public void shouldNotAcceptOperationResultsAfterShutdownWhenNonBlockingQueueIsUsed() throws WorkloadException, MetricsCollectionException {
        ConcurrentErrorReporter errorReporter = new ConcurrentErrorReporter();
        SimpleCsvFileWriter csvResultsLogWriter = null;
        Map<Integer, Class<? extends Operation<?>>> operationTypeToClassMapping = new HashMap<>();
        operationTypeToClassMapping.put(LdbcQuery1.TYPE, LdbcQuery1.class);
        operationTypeToClassMapping.put(LdbcQuery2.TYPE, LdbcQuery2.class);
        ConcurrentMetricsService metricsService = new DisruptorConcurrentMetricsService(
                timeSource,
                errorReporter,
                TimeUnit.MILLISECONDS,
                DisruptorConcurrentMetricsService.DEFAULT_HIGHEST_EXPECTED_RUNTIME_DURATION_AS_NANO,
                csvResultsLogWriter,
                operationTypeToClassMapping
        );

        metricsService.shutdown();
        boolean exceptionThrown = false;
        try {
            shouldReturnCorrectMeasurements(metricsService);
        } catch (MetricsCollectionException e) {
            exceptionThrown = true;
        }
        assertThat(exceptionThrown, is(true));
    }

    @Test
    public void shouldReturnCorrectMeasurementsWhenBlockingQueueIsUsed() throws WorkloadException, MetricsCollectionException {
        ConcurrentErrorReporter errorReporter = new ConcurrentErrorReporter();
        SimpleCsvFileWriter csvResultsLogWriter = null;
        Map<Integer, Class<? extends Operation<?>>> operationTypeToClassMapping = new HashMap<>();
        operationTypeToClassMapping.put(LdbcQuery1.TYPE, LdbcQuery1.class);
        operationTypeToClassMapping.put(LdbcQuery2.TYPE, LdbcQuery2.class);
        ConcurrentMetricsService metricsService = new DisruptorConcurrentMetricsService(
                timeSource,
                errorReporter,
                TimeUnit.MILLISECONDS,
                DisruptorConcurrentMetricsService.DEFAULT_HIGHEST_EXPECTED_RUNTIME_DURATION_AS_NANO,
                csvResultsLogWriter,
                operationTypeToClassMapping
        );
        try {
            shouldReturnCorrectMeasurements(metricsService);
        } finally {
            System.out.println(errorReporter.toString());
            metricsService.shutdown();
        }
    }

    @Test
    public void shouldReturnCorrectMeasurementsWhenNonBlockingQueueIsUsed() throws WorkloadException, MetricsCollectionException {
        ConcurrentErrorReporter errorReporter = new ConcurrentErrorReporter();
        SimpleCsvFileWriter csvResultsLogWriter = null;
        Map<Integer, Class<? extends Operation<?>>> operationTypeToClassMapping = new HashMap<>();
        operationTypeToClassMapping.put(LdbcQuery1.TYPE, LdbcQuery1.class);
        operationTypeToClassMapping.put(LdbcQuery2.TYPE, LdbcQuery2.class);
        ConcurrentMetricsService metricsService = new DisruptorConcurrentMetricsService(
                timeSource,
                errorReporter,
                TimeUnit.MILLISECONDS,
                DisruptorConcurrentMetricsService.DEFAULT_HIGHEST_EXPECTED_RUNTIME_DURATION_AS_NANO,
                csvResultsLogWriter,
                operationTypeToClassMapping
        );
        try {
            shouldReturnCorrectMeasurements(metricsService);
        } finally {
            System.out.println(errorReporter.toString());
            metricsService.shutdown();
        }
    }

    public void shouldReturnCorrectMeasurements(ConcurrentMetricsService metricsService) throws WorkloadException, MetricsCollectionException {
        assertThat(metricsService.results().startTimeAsMilli(), equalTo(-1l));
        assertThat(metricsService.results().latestFinishTimeAsMilli(), is(-1l));

        // scheduled: 1, actual: 2, duration: 1
        Operation<?> operation1 = DummyLdbcSnbInteractiveOperationInstances.read1();
        operation1.setScheduledStartTimeAsMilli(1l);
        operation1.setTimeStamp(1l);
        int operation1ResultCode = 1;
        long operation1ActualStartTime = 2;
        long operation1RunDuration = TimeUnit.MILLISECONDS.toNanos(1);

        metricsService.submitOperationResult(operation1.type(), operation1.scheduledStartTimeAsMilli(), operation1ActualStartTime, operation1RunDuration, operation1ResultCode);

        assertThat(metricsService.results().startTimeAsMilli(), equalTo(2l));
        assertThat(metricsService.results().latestFinishTimeAsMilli(), equalTo(3l));

        Operation<?> operation2 = DummyLdbcSnbInteractiveOperationInstances.read1();
        operation2.setScheduledStartTimeAsMilli(1l);
        operation2.setTimeStamp(1l);
        int operation2ResultCode = 2;
        long operation2ActualStartTime = 8;
        long operation2RunDuration = TimeUnit.MILLISECONDS.toNanos(3);

        metricsService.submitOperationResult(operation2.type(), operation2.scheduledStartTimeAsMilli(), operation2ActualStartTime, operation2RunDuration, operation2ResultCode);

        assertThat(metricsService.results().startTimeAsMilli(), equalTo(2l));
        assertThat(metricsService.results().latestFinishTimeAsMilli(), equalTo(11l));

        Operation<?> operation3 = DummyLdbcSnbInteractiveOperationInstances.read2();
        operation3.setScheduledStartTimeAsMilli(1l);
        operation3.setTimeStamp(1l);
        int operation3ResultCode = 2;
        long operation3ActualStartTime = 11;
        long operation3RunDuration = TimeUnit.MILLISECONDS.toNanos(5);

        metricsService.submitOperationResult(operation3.type(), operation3.scheduledStartTimeAsMilli(), operation3ActualStartTime, operation3RunDuration, operation3ResultCode);

        WorkloadResultsSnapshot results = metricsService.results();
        assertThat(results.startTimeAsMilli(), equalTo(2l));
        assertThat(results.latestFinishTimeAsMilli(), equalTo(16l));
    }
}
