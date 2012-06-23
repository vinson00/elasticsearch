/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.field.data.strings;

import org.elasticsearch.common.RamUsage;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.concurrent.ThreadLocals;

/**
 *
 */
public class SingleValueStringFieldData extends StringFieldData {

    private static ThreadLocal<ThreadLocals.CleanableValue<String[]>> valuesCache = new ThreadLocal<ThreadLocals.CleanableValue<String[]>>() {
        @Override
        protected ThreadLocals.CleanableValue<String[]> initialValue() {
            return new ThreadLocals.CleanableValue<String[]>(new String[1]);
        }
    };

    // order with value 0 indicates no value
    private final int[] ordinals;

    public SingleValueStringFieldData(String fieldName, int[] ordinals, String[] values) {
        super(fieldName, values);
        this.ordinals = ordinals;
    }

    @Override
    protected long computeSizeInBytes() {
        return super.computeSizeInBytes() +
                RamUsage.NUM_BYTES_INT * ordinals.length + RamUsage.NUM_BYTES_ARRAY_HEADER;
    }

    int[] ordinals() {
        return ordinals;
    }

    @Override
    public void forEachOrdinalInDoc(int docId, OrdinalInDocProc proc) {
        proc.onOrdinal(docId, ordinals[docId]);
    }

    @Override
    public boolean multiValued() {
        return false;
    }

    @Override
    public boolean hasValue(int docId) {
        return ordinals[docId] != 0;
    }

    @Override
    public void forEachValueInDoc(int docId, StringValueInDocProc proc) {
        int loc = ordinals[docId];
        if (loc == 0) {
            proc.onMissing(docId);
            return;
        }
        proc.onValue(docId, values[loc]);
    }

    @Override
    public String value(int docId) {
        return values[ordinals[docId]];
    }

    @Override
    public String[] values(int docId) {
        int loc = ordinals[docId];
        if (loc == 0) {
            return Strings.EMPTY_ARRAY;
        }
        String[] ret = valuesCache.get().get();
        ret[0] = values[loc];
        return ret;
    }
}
