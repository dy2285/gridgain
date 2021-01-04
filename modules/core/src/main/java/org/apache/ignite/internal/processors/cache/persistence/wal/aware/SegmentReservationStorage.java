/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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
package org.apache.ignite.internal.processors.cache.persistence.wal.aware;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * Segment reservations storage: Protects WAL segments from deletion during WAL log cleanup.
 */
class SegmentReservationStorage extends SegmentObservable {
    /**
     * Maps absolute segment index to reservation counter. If counter > 0 then we wouldn't delete all segments which has
     * index >= reserved segment index. Guarded by {@code this}.
     */
    private final NavigableMap<Long, Integer> reserved = new TreeMap<>();

    /** Maximum segment index that can be reserved. */
    private long minReserveIdx = -1;

    /**
     * Segment reservation. It will be successful if segment is {@code >} than the {@link #minReserveIdx minimum}.
     *
     * @param absIdx Index for reservation.
     * @return {@code True} if the reservation was successful.
     */
    boolean reserve(long absIdx) {
        boolean res = false;
        Long minReservedIdx = null;

        synchronized (this) {
            if (absIdx > minReserveIdx) {
                minReservedIdx = trackingMinReservedIdx(reserved -> reserved.merge(absIdx, 1, Integer::sum));

                res = true;
            }
        }

        if (minReservedIdx != null)
            notifyObservers(minReservedIdx);

        return res;
    }

    /**
     * Checks if segment is currently reserved (protected from deletion during WAL cleanup).
     *
     * @param absIdx Index for check reservation.
     * @return {@code True} if index is reserved.
     */
    synchronized boolean reserved(long absIdx) {
        return reserved.floorKey(absIdx) != null;
    }

    /**
     * @param absIdx Reserved index.
     */
    void release(long absIdx) {
        Long minReservedIdx;

        synchronized (this) {
            minReservedIdx = trackingMinReservedIdx(reserved -> {
                Integer cur = reserved.get(absIdx);

                assert cur != null && cur >= 1 : "cur=" + cur + ", absIdx=" + absIdx;

                if (cur == 1)
                    reserved.remove(absIdx);
                else
                    reserved.put(absIdx, cur - 1);
            });
        }

        if (minReservedIdx != null)
            notifyObservers(minReservedIdx);
    }

    /**
     * Increasing minimum segment index that can be reserved.
     * Value will be updated if it is greater than the current one.
     * If segment is already reserved, the update will fail.
     *
     * @param absIdx Absolut segment index.
     * @return {@code True} if update is successful.
     */
    synchronized boolean minReserveIndex(long absIdx) {
        if (reserved(absIdx))
            return false;

        minReserveIdx = Math.max(minReserveIdx, absIdx);

        return true;
    }

    /**
     * Updating {@link #reserved} with tracking changes of minimum reserved segment.
     *
     * @param updateFun {@link #reserved} update function.
     * @return New minimum reserved segment, {@code null} if there are no changes,
     *      {@code -1} if there are no reserved segments.
     */
    @Nullable private synchronized Long trackingMinReservedIdx(Consumer<NavigableMap<Long, Integer>> updateFun) {
        Map.Entry<Long, Integer> oldMinE = reserved.firstEntry();

        updateFun.accept(reserved);

        Map.Entry<Long, Integer> newMinE = reserved.firstEntry();

        Long oldMin = oldMinE == null ? null : oldMinE.getKey();
        Long newMin = newMinE == null ? null : newMinE.getKey();

        return Objects.equals(oldMin, newMin) ? null : newMin == null ? -1 : newMin;
    }
}
