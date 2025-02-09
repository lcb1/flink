/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.architecture.rules;

import org.apache.flink.core.testutils.AllCallbackWrapper;
import org.apache.flink.runtime.testutils.MiniClusterExtension;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.flink.test.util.MiniClusterWithClientResource;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static org.apache.flink.architecture.common.Conditions.fulfill;
import static org.apache.flink.architecture.common.GivenJavaClasses.javaClassesThat;
import static org.apache.flink.architecture.common.Predicates.arePublicFinalOfTypeWithAnnotation;
import static org.apache.flink.architecture.common.Predicates.arePublicStaticFinalOfType;
import static org.apache.flink.architecture.common.Predicates.arePublicStaticFinalOfTypeWithAnnotation;
import static org.apache.flink.architecture.common.Predicates.containAnyFieldsInClassHierarchyThat;

/** Rules for Integration Tests. */
public class ITCaseRules {

    @ArchTest
    public static final ArchRule INTEGRATION_TEST_ENDING_WITH_ITCASE =
            freeze(
                    javaClassesThat()
                            .areAssignableTo(AbstractTestBase.class)
                            .and()
                            .doNotHaveModifier(ABSTRACT)
                            .should()
                            .haveSimpleNameEndingWith("ITCase"));

    /**
     * In order to pass this check, IT cases must fulfill at least one of the following conditions.
     *
     * <p>1. For JUnit 5 test, both fields are required like:
     *
     * <pre>{@code
     * public static final MiniClusterExtension MINI_CLUSTER_RESOURCE =
     *         new MiniClusterExtension(
     *                 new MiniClusterResourceConfiguration.Builder()
     *                         .setConfiguration(getFlinkConfiguration())
     *                         .build());
     *
     * @RegisterExtension
     * public static AllCallbackWrapper allCallbackWrapper =
     *         new AllCallbackWrapper(MINI_CLUSTER_RESOURCE);
     * }</pre>
     *
     * <p>2. For JUnit 4 test via @Rule like:
     *
     * <pre>{@code
     * @Rule
     *  public final MiniClusterWithClientResource miniClusterResource =
     *          new MiniClusterWithClientResource(
     *                  new MiniClusterResourceConfiguration.Builder()
     *                          .setNumberTaskManagers(1)
     *                          .setNumberSlotsPerTaskManager(PARALLELISM)
     *                          .setRpcServiceSharing(RpcServiceSharing.DEDICATED)
     *                          .withHaLeadershipControl()
     *                          .build());
     * }</pre>
     *
     * <p>3. For JUnit 4 test via @ClassRule like:
     *
     * <pre>{@code
     * @ClassRule
     * public static final MiniClusterWithClientResource MINI_CLUSTER =
     *         new MiniClusterWithClientResource(
     *                 new MiniClusterResourceConfiguration.Builder()
     *                         .setConfiguration(new Configuration())
     *                         .build());
     * }</pre>
     */
    @ArchTest
    public static final ArchRule ITCASE_USE_MINICLUSTER =
            freeze(
                    javaClassesThat()
                            .haveSimpleNameEndingWith("ITCase")
                            .and()
                            .areTopLevelClasses()
                            .and()
                            .doNotHaveModifier(ABSTRACT)
                            .should(
                                    fulfill(
                                            // JUnit 5 violation check
                                            containAnyFieldsInClassHierarchyThat(
                                                            arePublicStaticFinalOfType(
                                                                    MiniClusterExtension.class))
                                                    .and(
                                                            containAnyFieldsInClassHierarchyThat(
                                                                    arePublicStaticFinalOfTypeWithAnnotation(
                                                                            AllCallbackWrapper
                                                                                    .class,
                                                                            RegisterExtension
                                                                                    .class)))
                                                    // JUnit 4 violation check, which should be
                                                    // removed
                                                    // after the JUnit 4->5 migration is closed.
                                                    // Please refer to FLINK-25858.
                                                    .or(
                                                            containAnyFieldsInClassHierarchyThat(
                                                                    arePublicStaticFinalOfTypeWithAnnotation(
                                                                            MiniClusterWithClientResource
                                                                                    .class,
                                                                            ClassRule.class)))
                                                    .or(
                                                            containAnyFieldsInClassHierarchyThat(
                                                                    arePublicFinalOfTypeWithAnnotation(
                                                                            MiniClusterWithClientResource
                                                                                    .class,
                                                                            Rule.class))))));
}
