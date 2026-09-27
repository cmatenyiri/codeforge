package com.codeforge.config;

import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liquibase's DATABASECHANGELOG table, created safely when several backend
 * instances start against an empty database at the same moment.
 *
 * <p>Liquibase serialises migrations through a row lock in
 * DATABASECHANGELOGLOCK, but it creates DATABASECHANGELOG one step before it
 * takes that lock. Two replicas starting together both find no table, both try
 * to create it, and the slower one fails with "Table 'DATABASECHANGELOG'
 * already exists" — and with it the whole application start.
 *
 * <p>So a failed create is looked at again: if the table is there now, another
 * instance made it, and this one carries on exactly as a later start would —
 * it waits for the lock while the other migrates, then finds nothing left to
 * do. Liquibase already guards its lock table this way (see
 * {@code StandardLockService#init}); this is the same guard for the table it
 * missed.
 *
 * <p>Liquibase finds it through {@code META-INF/services} and picks it over
 * the standard service because of its higher priority.
 */
public class ConcurrentChangeLogHistoryService extends StandardChangeLogHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentChangeLogHistoryService.class);

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    @Override
    public void init() throws DatabaseException {
        try {
            super.init();
        } catch (DatabaseException e) {
            // Forget the "no table" this instance saw before it tried to create
            // one, and look again.
            reset();
            getDatabase().rollback();
            if (!hasDatabaseChangeLogTable()) {
                throw e;
            }
            log.info("{} was created by another instance starting at the same time", getDatabaseChangeLogTableName());
            super.init();
        }
    }
}
