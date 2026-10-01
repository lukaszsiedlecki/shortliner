package ovh.lukis.shortliner.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClickEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ClickEventProducer producer = new ClickEventProducer(kafkaTemplate, meterRegistry);

    @Test
    void countsSuccessfulPublish() {
        when(kafkaTemplate.send(eq("shortliner.clicks"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendClickEvent(event());

        assertThat(published("success")).isEqualTo(1.0);
    }

    @Test
    void countsAsyncFailure() {
        when(kafkaTemplate.send(any(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker down")));

        producer.sendClickEvent(event());

        assertThat(published("failure")).isEqualTo(1.0);
    }

    @Test
    void countsSynchronousFailureWithoutThrowing() {
        when(kafkaTemplate.send(any(), anyString(), anyString()))
                .thenThrow(new TimeoutException("metadata not available"));

        producer.sendClickEvent(event());

        assertThat(published("failure")).isEqualTo(1.0);
    }

    private double published(String result) {
        var counter = meterRegistry.find("shortliner.click.events.published").tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private static ClickEvent event() {
        return ClickEvent.create("abc123", null, "127.0.0.1", "test-agent", null);
    }
}
