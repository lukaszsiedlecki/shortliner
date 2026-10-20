package ovh.lukis.shortliner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableResilientMethods
public class ShortLinerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinerApplication.class, args);
    }
}
