package com.tskrypko.authentication.config;

import com.tskrypko.authentication.config.integration.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;

public class BaseAuthenticationIntegrationTest extends BaseIntegrationTest {
    @AfterEach
    public void tearDown() {
        jdbcTemplate.execute("""
                    DO $$ 
                    DECLARE r RECORD;
                    BEGIN 
                        FOR r IN (SELECT schemaname, tablename FROM pg_catalog.pg_tables WHERE schemaname IN ('user_profiles', 'user_auth'))
                        LOOP 
                            EXECUTE 'TRUNCATE TABLE ' || r.schemaname || '.' || r.tablename || ' CASCADE'; 
                        END LOOP; 
                    END $$;
                """);
    }
}
