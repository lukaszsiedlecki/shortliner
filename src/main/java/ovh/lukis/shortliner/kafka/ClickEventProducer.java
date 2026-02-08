package ovh.lukis.shortliner.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(ClickEventProducer.class);
    private static final String TOPIC = "shortliner.clicks";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ClickEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
                        } else {
                            logger.debug("Click event sent for shortCode={}", event.shortCode());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize click event: {}", e.getMessage());
        }
    }
}
