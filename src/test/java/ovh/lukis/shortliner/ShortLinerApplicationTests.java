package ovh.lukis.shortliner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import ovh.lukis.shortliner.kafka.ClickEvent;
import ovh.lukis.shortliner.kafka.ClickEventProducer;

@SpringBootTest
@Import(ShortLinerApplicationTests.TestConfig.class)
class ShortLinerApplicationTests {

    static class TestConfig {
        @Bean
        @Primary
        public ClickEventProducer clickEventProducer() {
            return new ClickEventProducer(null) {
                @Override
                public void sendClickEvent(ClickEvent event) {
                    // No-op for tests
                }
            };
        }
    }

    @Test
    void contextLoads() {
    }
}
