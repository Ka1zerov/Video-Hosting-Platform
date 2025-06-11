package com.tskrypko.authentication.config.integration;

import org.junit.jupiter.api.AfterEach;

/**
 * Enforces a teardown method for integration tests.
 * <p>
 * Implementing classes **must** provide a cleanup method and annotate it with {@link AfterEach}:
 * </p>
 * <pre>
 * &#64;AfterEach
 * &#64;Override
 * public void tearDown() {
 *     // Cleanup logic here
 * }
 * </pre>
 */

public interface BaseTestTeardownLifecycle {
    //It has to be done with annotation @AfterEach
    @AfterEach
    void tearDown();
}
