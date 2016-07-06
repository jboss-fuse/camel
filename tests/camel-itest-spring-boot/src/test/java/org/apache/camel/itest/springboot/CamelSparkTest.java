/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.springboot;

import org.apache.camel.itest.springboot.util.ArquillianPackager;
import org.apache.camel.itest.springboot.util.DependencyResolver;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
public class CamelSparkTest extends AbstractSpringBootTestSupport {

    @Deployment
    public static Archive<?> createSpringBootPackage() throws Exception {
        return ArquillianPackager.springBootPackage(createTestConfig());
    }

    public static ITestConfig createTestConfig() {
        return new ITestConfigBuilder()
                .module(inferModuleName(CamelSparkTest.class))
                .dependency(DependencyResolver.withVersion("org.hibernate:hibernate-validator"))
                .dependency(DependencyResolver.withVersion("version_spark_", "com.fasterxml.jackson.core:jackson-core"))
                .dependency(DependencyResolver.withVersion("version_spark_", "com.fasterxml.jackson.core:jackson-annotations"))
                .dependency(DependencyResolver.withVersion("version_spark_", "com.fasterxml.jackson.core:jackson-databind"))
                .dependency(DependencyResolver.withVersion("version_spark_", "com.fasterxml.jackson.module:jackson-module-scala_2.11"))
                .exclusion("log4j:apache-log4j-extras")
                .build();
    }

    @Test
    public void componentTests() throws Exception {
        this.runComponentTest(config);
        this.runModuleUnitTestsIfEnabled(config);
    }


}
