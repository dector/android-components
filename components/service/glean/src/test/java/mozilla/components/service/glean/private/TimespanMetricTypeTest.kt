package mozilla.components.service.glean.private

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.glean.error.ErrorRecording.ErrorType
import mozilla.components.service.glean.error.ErrorRecording.testGetNumRecordedErrors
import mozilla.components.service.glean.resetGlean
import mozilla.components.service.glean.timing.TimingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimespanMetricTypeTest {

    @Before
    fun setUp() {
        resetGlean()
    }

    @Test
    fun `The API must record to its storage engine`() {
        // Define a timespan metric, which will be stored in "store1"
        val metric = TimespanMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1"),
            timeUnit = TimeUnit.Millisecond
        )

        // Record a timespan.
        metric.start(this)
        metric.stopAndSum(this)

        // Check that data was properly recorded.
        assertTrue(metric.testHasValue())
        assertTrue(metric.testGetValue() >= 0)
    }

    @Test
    fun `The API should not record if the metric is disabled`() {
        // Define a timespan metric, which will be stored in "store1"
        val metric = TimespanMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1"),
            timeUnit = TimeUnit.Millisecond
        )

        // Record a timespan.
        metric.start(this)
        metric.stopAndSum(this)

        // Let's also call cancel() to make sure it's a no-op.
        metric.cancel(this)

        // Check that data was not recorded.
        assertFalse("The API should not record a counter if metric is disabled",
            metric.testHasValue())
    }

    @Test
    fun `The API must correctly cancel`() {
        // Define a timespan metric, which will be stored in "store1"
        val metric = TimespanMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1"),
            timeUnit = TimeUnit.Millisecond
        )

        // Record a timespan.
        metric.start(this)
        metric.cancel(this)
        metric.stopAndSum(this)

        // Check that data was not recorded.
        assertFalse("The API should not record a counter if metric is cancelled",
            metric.testHasValue())
        assertEquals(1, testGetNumRecordedErrors(metric, ErrorType.InvalidValue))
    }

    @Test(expected = NullPointerException::class)
    fun `testGetValue() throws NullPointerException if nothing is stored`() {
        val metric = TimespanMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1"),
            timeUnit = TimeUnit.Millisecond
        )
        metric.testGetValue()
    }

    @Test
    fun `The API saves to secondary pings`() {
        // Define a timespan metric, which will be stored in "store1" and "store2"
        val metric = TimespanMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1", "store2"),
            timeUnit = TimeUnit.Millisecond
        )

        // Record a timespan.
        metric.start(this)
        metric.stopAndSum(this)

        // Check that data was properly recorded in the second ping.
        assertTrue(metric.testHasValue("store2"))
        assertTrue(metric.testGetValue("store2") >= 0)
    }

    @Test
    fun `Records an error if started twice`() {
        // Define a timespan metric, which will be stored in "store1" and "store2"
        val metric = TimespanMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "timespan_metric",
            sendInPings = listOf("store1", "store2"),
            timeUnit = TimeUnit.Millisecond
        )

        // Record a timespan.
        metric.start(this)
        metric.start(this)
        metric.stopAndSum(this)

        // Check that data was properly recorded in the second ping.
        assertTrue(metric.testHasValue("store2"))
        assertTrue(metric.testGetValue("store2") >= 0)
        assertEquals(1, testGetNumRecordedErrors(metric, ErrorType.InvalidValue))
    }

    @Test
    fun `test setRawNanos`() {
        val timespanNanos = 1200000000L

        val metric = TimespanMetricType(
            false,
            "telemetry",
            Lifetime.Ping,
            "explicit_timespan",
            listOf("store1"),
            timeUnit = TimeUnit.Second
        )

        // This should have no effect
        TimingManager.getElapsedNanos = { 0 }
        metric.start(this)
        TimingManager.getElapsedNanos = { 50 }
        metric.stopAndSum(this)

        metric.setRawNanos(timespanNanos)
        assertEquals(timespanNanos, metric.testGetValue())
    }

    @Test
    fun `test setRawNanos followed by other API`() {
        val timespanNanos = 1200000000L

        val metric = TimespanMetricType(
            false,
            "telemetry",
            Lifetime.Ping,
            "explicit_timespan",
            listOf("store1"),
            timeUnit = TimeUnit.Second
        )

        metric.setRawNanos(timespanNanos)
        assertEquals(timespanNanos, metric.testGetValue())

        TimingManager.getElapsedNanos = { 0 }
        metric.start(this)
        TimingManager.getElapsedNanos = { 50 }
        metric.stopAndSum(this)
        val value = metric.testGetValue()
        assertTrue(timespanNanos < value)
    }

    @Test
    fun `test sumRawNanos`() {
        val timespanNanos = 1200000000L

        val metric = TimespanMetricType(
            false,
            "telemetry",
            Lifetime.Ping,
            "explicit_timespan",
            listOf("store1"),
            timeUnit = TimeUnit.Second
        )

        metric.sumRawNanos(timespanNanos)
        assertEquals(timespanNanos, metric.testGetValue())
        metric.sumRawNanos(timespanNanos)
        assertEquals(timespanNanos * 2, metric.testGetValue())
    }
}
