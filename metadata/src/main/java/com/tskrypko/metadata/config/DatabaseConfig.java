package com.tskrypko.metadata.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@Configuration
@Profile("replica")
@EnableJpaRepositories(
    basePackages = "com.tskrypko.metadata.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class DatabaseConfig {

    /**
     * Primary DataSource for write operations (routes to Master via HAProxy)
     */
    @Primary
    @Bean(name = "writeDataSource")
    @ConfigurationProperties("spring.datasource")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Secondary DataSource for read operations (routes to Master + Slave via HAProxy)  
     */
    @Bean(name = "readDataSource")
    @ConfigurationProperties("spring.datasource-readonly")
    public DataSource readDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Routing DataSource that automatically switches between read/write based on transaction type
     */
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        
        ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.setTargetDataSources(Map.of(
            "write", writeDataSource,
            "read", readDataSource
        ));
        return routingDataSource;
    }

    /**
     * EntityManagerFactory using routing datasource
     */
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("routingDataSource") DataSource routingDataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(routingDataSource);
        em.setPackagesToScan("com.tskrypko.metadata.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.ddl-auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");
        em.setJpaProperties(properties);
        
        return em;
    }

    /**
     * Transaction Manager for routing datasource
     */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
} 