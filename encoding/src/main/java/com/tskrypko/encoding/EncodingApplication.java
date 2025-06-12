package com.tskrypko.encoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EncodingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncodingApplication.class, args);
    }

} 