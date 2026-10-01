package ovh.lukis.shortliner.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(ClickEventProducer.class);
    private static final String TOPIC = "shortliner.clicks";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public ClickEventProducer(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
    }

    public void sendClickEvent(ClickEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.shortCode(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to send click event for shortCode={}: {}",
                                    event.shortCode(), ex.getMessage());
                            countPublish("failure");
                        } else {
                            logger.debug("Click event sent for shortCode={}", event.shortCode());
                            countPublish("success");
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize click event: {}", e.getMessage());
            countPublish("failure");
        } catch (RuntimeException e) {
            // send() can throw synchronously (e.g. metadata fetch timeout when the broker is down);
            // click tracking is fire-and-forget, so it must not break the redirect.
            logger.error("Failed to send click event for shortCode={}: {}", event.shortCode(), e.getMessage());
            countPublish("failure");
        }
    }

    private void countPublish(String result) {
        Counter.builder("shortliner.click.events.published")
                .description("Outcomes of publishing click events to Kafka")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }
}
