package dev.zayt.atrilha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("dev.zayt.atrilha")
public class AtrilhaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtrilhaApplication.class, args);
    }
}
