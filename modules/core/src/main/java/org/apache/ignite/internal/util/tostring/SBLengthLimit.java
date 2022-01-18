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

package org.apache.ignite.internal.util.tostring;

import org.apache.ignite.IgniteSystemProperties;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_TO_STRING_MAX_LENGTH;
import static org.apache.ignite.internal.util.tostring.GridToStringBuilder.DFLT_TO_STRING_MAX_LENGTH;

/**
 *
 */
class SBLengthLimit {
    /** */
    private static final int MAX_TO_STR_LEN = IgniteSystemProperties.getInteger(IGNITE_TO_STRING_MAX_LENGTH,
        DFLT_TO_STRING_MAX_LENGTH);

    /** Length of tail part of message. */
    private static final int TAIL_LEN = MAX_TO_STR_LEN / 10 * 2;

    /** Length of head part of message. */
    private static final int HEAD_LEN = MAX_TO_STR_LEN - TAIL_LEN;

    /** */
    private int len;

    /**
     * @return Current length.
     */
    int length() {
        return len;
    }

    /**
     *
     */
    void reset() {
        len = 0;
    }

    /**
     * @param sb String builder.
     * @param writtenLen Written length.
     */
    void onWrite(SBLimitedLength sb, int writtenLen) {
        len += writtenLen;

        if (overflowed(sb) && (sb.getTail() == null || sb.getTail().length() == 0)) {
            CircularStringBuilder tail = getTail();

            int newSbLen = Math.min(sb.length(), HEAD_LEN + 1);
            tail.append(sb.impl().substring(newSbLen));

            sb.setTail(tail);
            sb.setLength(newSbLen);
        }
    }

    CircularStringBuilder getTail() {
        return new CircularStringBuilder(TAIL_LEN);
    }

    /**
     * @return {@code True} if reached limit.
     */
    boolean overflowed(SBLimitedLength sb) {
        return sb.impl().length() > HEAD_LEN;
    }
}
