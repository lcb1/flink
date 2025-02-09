/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * imitations under the License.
 */

package org.apache.flink.table.planner.runtime.batch.sql;

import org.apache.flink.api.common.BatchShuffleMode;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.table.planner.factories.TestValuesTableFactory;
import org.apache.flink.table.planner.runtime.utils.BatchTestBase;
import org.apache.flink.table.planner.runtime.utils.TestData;
import org.apache.flink.table.planner.utils.JavaScalaConversionUtil;
import org.apache.flink.types.Row;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/** Tests for ForwardHashExchangeProcessor. */
public class ForwardHashExchangeITCase extends BatchTestBase {

    @Before
    public void before() {
        super.before();
        env().getConfig().setDynamicGraph(true);
        env().disableOperatorChaining();
        tEnv().getConfig()
                .getConfiguration()
                .set(ExecutionOptions.BATCH_SHUFFLE_MODE, BatchShuffleMode.ALL_EXCHANGES_BLOCKING);

        String testDataId = TestValuesTableFactory.registerData(TestData.data3());
        String ddl =
                "CREATE TABLE MyTable (\n"
                        + "  a int,\n"
                        + "  b bigint,\n"
                        + "  c string\n"
                        + ") WITH (\n"
                        + "  'connector' = 'values',\n"
                        + "  'data-id' = '"
                        + testDataId
                        + "',\n"
                        + "  'bounded' = 'true'\n"
                        + ")";
        tEnv().executeSql(ddl);
    }

    @Test
    public void testAgg() {
        checkResult(
                "SELECT\n"
                        + "   b,\n"
                        + "   SUM(a) sum_a,\n"
                        + "   AVG(SUM(a)) OVER (PARTITION BY b) avg_a,\n"
                        + "   RANK() OVER (PARTITION BY b ORDER BY b) rn\n"
                        + " FROM MyTable\n"
                        + " GROUP BY b",
                JavaScalaConversionUtil.toScala(
                        Arrays.asList(
                                Row.of(1, 1, 1, 1),
                                Row.of(2, 5, 5, 1),
                                Row.of(3, 15, 15, 1),
                                Row.of(4, 34, 34, 1),
                                Row.of(5, 65, 65, 1),
                                Row.of(6, 111, 111, 1))),
                false);
    }
}
