/*
 * Copyright (c) 2022,2023, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.util;

import java.lang.ref.Cleaner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retains open database connections.
 * 
 * @author simpsons
 */
public class SQLConnectionPool {
    /**
     * Creates new database connections.
     *
     * @author simpsons
     */
    public static interface Factory {
        /**
         * Create a new database connection.
         *
         * @return the new connection
         *
         * @throws SQLException if an SQL error occurred
         */
        Connection newConnection() throws SQLException;
    }

    private final Lock lock = new ReentrantLock();

    private final Factory factory;

    private final AutoCloseableProxifier<Connection,
                                         Consumer<Connection>> proxifier;

    /**
     * Create a pool.
     * 
     * @param factory the means to create a fresh connection
     */
    public SQLConnectionPool(Factory factory) {
        proxifier =
            new AutoCloseableProxifier<>(Connection.class, this::recover);
        this.factory = factory;
        this.state = new State();
        this.cleanable = cleaner.register(this, state);
    }

    private static class State implements Runnable {
        private final List<Connection> bases = new ArrayList<>();

        @Override
        public void run() {
            bases.forEach(base -> {
                try {
                    base.close();
                } catch (SQLException ex) {
                    logger.log(Level.WARNING, "release", ex);
                }
            });
        }
    }

    private final State state;

    private void recover(Connection base) {
        try {
            if (!base.getAutoCommit()) base.rollback();
            try {
                lock.lock();
                state.bases.add(base);
            } finally {
                lock.unlock();
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "reset", ex);
        }
    }

    /**
     * Get a connection. One is taken from the pool if available.
     * Otherwise, the factory will be invoked. The returned object, when
     * closed, will automatically return to the pool. The returned
     * connection will be fresh (if not actually new), with auto-commit
     * enabled, and nothing to roll back to.
     * 
     * @return the fresh connection
     * 
     * @throws SQLException if a database error occurs in creating a
     * fresh connection, or in enabling auto-commit
     */
    public Connection open() throws SQLException {
        try {
            lock.lock();
            final Connection base;
            if (state.bases.isEmpty()) {
                base = factory.newConnection();
            } else {
                base = state.bases.remove(0);
            }
            base.setAutoCommit(true);

            return proxifier.proxy(base);
        } finally {
            lock.unlock();
        }
    }

    private final Cleaner.Cleanable cleanable;

    private static final Cleaner cleaner = Cleaner.create();

    private static final Logger logger =
        Logger.getLogger(SQLConnectionPool.class.getPackageName() + ".db");
}
