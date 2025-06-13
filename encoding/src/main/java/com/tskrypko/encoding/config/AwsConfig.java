package com.tskrypko.encoding.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Configuration for S3 client with support for both AWS and LocalStack environments.
 * 
 * <p>This configuration automatically detects the environment based on the presence of 
 * {@code aws.endpoint} property:
 * <ul>
 *   <li>If {@code aws.endpoint} is provided: Uses LocalStack with path-style access</li>
 *   <li>If {@code aws.endpoint} is empty/missing: Uses standard AWS S3</li>
 * </ul>
 * 
 * <h3>Required Properties (for AWS):</h3>
 * <ul>
 *   <li>{@code aws.access.key} - AWS Access Key ID</li>
 *   <li>{@code aws.secret.key} - AWS Secret Access Key</li>
 *   <li>{@code aws.region} - AWS Region (defaults to us-east-1)</li>
 * </ul>
 * 
 * <h3>Additional Properties (for LocalStack):</h3>
 * <ul>
 *   <li>{@code aws.endpoint} - LocalStack endpoint URL (e.g., http://localhost:4566)</li>
 * </ul>
 * 
 * <h3>Example configurations:</h3>
 * 
 * <h4>Production (AWS):</h4>
 * <pre>
 * aws.access.key=AKIAIOSFODNN7EXAMPLE
 * aws.secret.key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
 * aws.region=us-west-2
 * </pre>
 * 
 * <h4>Testing (LocalStack):</h4>
 * <pre>
 * aws.access.key=test
 * aws.secret.key=test
 * aws.region=us-east-1
 * aws.endpoint=http://localhost:4566
 * </pre>
 * 
 * @author Video Hosting Platform Team
 * @since 1.0
 */
@Configuration
public class AwsConfig {

    @Value("${aws.access.key:}")
    private String accessKey;

    @Value("${aws.secret.key:}")
    private String secretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.endpoint:}")
    private String endpoint;

    /**
     * Creates and configures an Amazon S3 client for either AWS or LocalStack.
     * 
     * <p>The client configuration adapts based on the presence of the endpoint property:
     * <ul>
     *   <li><strong>LocalStack mode</strong>: When endpoint is provided, enables path-style 
     *       access and uses custom endpoint configuration</li>
     *   <li><strong>AWS mode</strong>: When endpoint is empty, uses standard AWS regional 
     *       configuration</li>
     * </ul>
     * 
     * @return Configured {@link AmazonS3} client ready for use
     * @throws IllegalArgumentException if required properties are missing in AWS mode
     */
    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (endpoint != null && !endpoint.isEmpty()) {
            // LocalStack configuration: uses custom endpoint with path-style access
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                   .withPathStyleAccessEnabled(true);
        } else {
            // Standard AWS configuration: uses regional endpoints
            builder.withRegion(Regions.fromName(region));
        }

        return builder.build();
    }
} 