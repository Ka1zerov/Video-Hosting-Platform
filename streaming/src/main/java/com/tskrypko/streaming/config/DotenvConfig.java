package com.tskrypko.streaming.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class that loads environment variables from .env file
 * if it exists in the application root directory.
 * 
 * This allows for easy local development configuration without exposing
 * sensitive credentials in the codebase.
 */
@Configuration
public class DotenvConfig {

    @PostConstruct
    public void loadDotenv() {
        try {
            // Load .env file if it exists, ignore if not found
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't fail if .env file doesn't exist
                    .load();

            // Set system properties for all loaded environment variables
            dotenv.entries().forEach(entry -> {
                // Only set if not already present as system property or environment variable
                if (System.getProperty(entry.getKey()) == null && 
                    System.getenv(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });

            System.out.println("✅ Loaded .env file for streaming service");
        } catch (Exception e) {
            // Log but don't fail - .env file is optional
            System.out.println("ℹ️  No .env file found for streaming service (this is optional)");
        }
    }
} 