/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.platform.client.tx;

import java.util.concurrent.CompletionStage;

import com.ibm.asyncutil.locks.AsyncLock;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxLocal;
import org.apache.ignite.transactions.TransactionState;

/**
 * Client transaction context.
 */
public class ClientTxContext {
    /** Transaction id. */
    private final int txId;

    /** Transaction. */
    private final GridNearTxLocal tx;

    /** Lock. */
    private final AsyncLock lock = AsyncLock.create();

    /**
     * Constructor.
     */
    public ClientTxContext(int txId, GridNearTxLocal tx) {
        assert txId != 0;
        assert tx != null;

        this.txId = txId;
        this.tx = tx;
    }

    /**
     * Acquire context to work with transaction in the current thread.
     */
    public AsyncLock.LockToken acquire(boolean resumeTx) throws IgniteCheckedException {
        return acquireAsync(resumeTx).toCompletableFuture().join();
    }

    /**
     * Acquire context to work with transaction in the current thread.
     */
    public CompletionStage<AsyncLock.LockToken> acquireAsync(boolean resumeTx) {
        return lock.acquireLock().thenApply(token -> {
            if (resumeTx) {
                try {
                    tx.resume();
                } catch (IgniteCheckedException e) {
                    token.releaseLock();
                    throw new RuntimeException(e);
                }
            }

            return token;
        });
    }

    /**
     * Release context.
     */
    public void release(boolean suspendTx) throws IgniteCheckedException {
        try {
            try {
                if (suspendTx) {
                    TransactionState state = tx.state();

                    if (state == TransactionState.ACTIVE)
                        tx.suspend();
                }
            }
            finally {
                // In some cases thread can still hold the transaction (due to concurrent rollbacks), threadMap should
                // be forcibly cleared to avoid problems with resuming other transactions in the current worker.
                tx.context().tm().clearThreadMap(tx);
            }
        }
        finally {
            lock.r();
        }
    }

    /**
     * Gets transaction id.
     */
    public int txId() {
        return txId;
    }

    /**
     * Gets transaction.
     */
    public GridNearTxLocal tx() {
        return tx;
    }

    /**
     * Close transaction context.
     */
    public void close() {
        lock.lock();

        try {
            tx.close();
        }
        catch (Exception ignore) {
            // No-op.
        }
        finally {
            lock.unlock();
        }
    }
}
