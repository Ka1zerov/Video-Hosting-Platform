package com.tskrypko.upload.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routing DataSource that automatically switches between read and write databases
 * based on the current transaction's read-only status.
 * 
 * <p>This class extends {@link AbstractRoutingDataSource} to provide intelligent
 * routing for master-slave database replication:
 * <ul>
 *   <li><strong>Write operations</strong>: Routes to master database (HAProxy port 5435)</li>
 *   <li><strong>Read operations</strong>: Routes to slave database (HAProxy port 5436)</li>
 * </ul>
 * 
 * <p>The routing decision is made automatically based on:
 * {@code @Transactional(readOnly = true)} annotations and current transaction state.
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(ReadWriteRoutingDataSource.class);

    /**
     * Determines the current lookup key for datasource routing.
     * 
     * <p>The routing logic:
     * <ul>
     *   <li>If current transaction is read-only → returns "read" (routes to slave)</li>
     *   <li>If current transaction is read-write → returns "write" (routes to master)</li>
     *   <li>If no transaction is active → defaults to "write" (safe fallback)</li>
     * </ul>
     * 
     * @return "read" for read-only transactions, "write" for write transactions
     */
    @Override
    protected Object determineCurrentLookupKey() {
        // Check if we're in a read-only transaction
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        if (logger.isDebugEnabled()) {
            String dataSource = isReadOnly ? "read (slave)" : "write (master)";
            logger.debug("Routing to {} datasource for transaction: readOnly={}", dataSource, isReadOnly);
        }
        
        return isReadOnly ? "read" : "write";
    }
    
    /**
     * Determines the datasource to use when no transaction is active.
     * Defaults to write datasource for safety.
     * 
     * @return "write" datasource key
     */
    @Override
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        if (lookupKey == null) {
            logger.warn("No lookup key specified, defaulting to write datasource");
            return "write";
        }
        return super.resolveSpecifiedLookupKey(lookupKey);
    }
} 