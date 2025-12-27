package com.fam.vest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableScheduling
@EnableCaching
public class Application {

	public static void main(String[] args) {
		// Enable virtual threads for better concurrency in Java 21
		System.setProperty("spring.threads.virtual.enabled", "true");
		SpringApplication.run(Application.class, args);
	}
}
