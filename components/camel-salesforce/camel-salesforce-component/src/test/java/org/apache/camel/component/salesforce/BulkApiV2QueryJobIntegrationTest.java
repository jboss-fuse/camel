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
package org.apache.camel.component.salesforce;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.component.salesforce.api.SalesforceException;
import org.apache.camel.component.salesforce.api.dto.bulkv2.JobStateEnum;
import org.apache.camel.component.salesforce.api.dto.bulkv2.OperationEnum;
import org.apache.camel.component.salesforce.api.dto.bulkv2.QueryJob;
import org.apache.camel.component.salesforce.api.dto.bulkv2.QueryJobs;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;


@SuppressWarnings("BusyWait")
public class BulkApiV2QueryJobIntegrationTest extends AbstractSalesforceTestBase {

    @Test
    public void testQueryLifecycle() throws Exception {
        QueryJob job = new QueryJob();
        job.setOperation(OperationEnum.QUERY);
        job.setQuery("SELECT Id, LastName FROM Contact");

        job = template().requestBody("salesforce:bulk2CreateQueryJob", job, QueryJob.class);
        assertNotNull(job.getId(), "JobId");

        job = template().requestBodyAndHeader("salesforce:bulk2GetQueryJob", "", "jobId",
                job.getId(), QueryJob.class);

        // wait for job to finish
        while (job.getState() != JobStateEnum.JOB_COMPLETE) {
            Thread.sleep(2000);
            job = template().requestBodyAndHeader("salesforce:bulk2GetQueryJob", "", "jobId",
                    job.getId(), QueryJob.class);
        }

        InputStream is = template().requestBodyAndHeader("salesforce:bulk2GetQueryJobResults",
                "", "jobId", job.getId(), InputStream.class);
        Assertions.assertThat(is).isNotNull().describedAs("Query Job results");
        List<String> results = IOUtils.readLines(is, StandardCharsets.UTF_8);
        Assertions.assertThat(results.size()).isGreaterThan(0).describedAs("Query Job results");
    }

    @Test
    public void testQueryAllLifecycle() throws Exception {
        QueryJob job = new QueryJob();
        job.setOperation(OperationEnum.QUERY_ALL);
        job.setQuery("SELECT Id, LastName FROM Contact");

        job = template().requestBody("salesforce:bulk2CreateQueryJob", job, QueryJob.class);
        Assertions.assertThat(job.getId()).isNotNull().describedAs("JobId");
        job = template().requestBodyAndHeader("salesforce:bulk2GetQueryJob", "", "jobId",
                job.getId(), QueryJob.class);

        // wait for job to finish
        while (job.getState() != JobStateEnum.JOB_COMPLETE) {
            Thread.sleep(2000);
            job = template().requestBodyAndHeader("salesforce:bulk2GetQueryJob", "", "jobId",
                    job.getId(), QueryJob.class);
        }

        InputStream is = template().requestBodyAndHeader("salesforce:bulk2GetQueryJobResults",
                "", "jobId", job.getId(), InputStream.class);
        Assertions.assertThat(is).isNotNull().describedAs("Query Job results");
        List<String> results = IOUtils.readLines(is, StandardCharsets.UTF_8);
        Assertions.assertThat(results.size()).isGreaterThan(0).describedAs("Query Job results");
    }

    @Test
    public void testAbort() {
        QueryJob job = new QueryJob();
        job.setOperation(OperationEnum.QUERY);
        job.setQuery("SELECT Id, LastName FROM Contact");

        job = template().requestBody("salesforce:bulk2CreateQueryJob", job, QueryJob.class);
        assertNotNull(job.getId(), "JobId");

        template().sendBodyAndHeader("salesforce:bulk2AbortQueryJob", "", "jobId", job.getId());

        job = template().requestBody("salesforce:bulk2GetQueryJob", job, QueryJob.class);
        Assertions.assertThat(job.getState() == JobStateEnum.ABORTED || job.getState() == JobStateEnum.FAILED)
                .isTrue().describedAs("Expected job to be aborted or failed.");
    }

    @Test
    public void testDelete() throws InterruptedException {
        QueryJob job = new QueryJob();
        job.setOperation(OperationEnum.QUERY);
        job.setQuery("SELECT Id, LastName FROM Contact");

        job = template().requestBody("salesforce:bulk2CreateQueryJob", job, QueryJob.class);
        Assertions.assertThat(job.getId()).as("JobId").isNotNull();

        job = template().requestBody("salesforce:bulk2GetQueryJob", job, QueryJob.class);
        int i = 0;
        while (job.getState() != JobStateEnum.JOB_COMPLETE) {
            i++;
            if (i == 5) {
                throw new IllegalStateException("Job failed to reach JOB_COMPLETE status.");
            }
            Thread.sleep(2000);
            job = template().requestBody("salesforce:bulk2GetQueryJob", job, QueryJob.class);
        }

        template().sendBodyAndHeader("salesforce:bulk2DeleteQueryJob", "", "jobId", job.getId());

        final QueryJob finalJob = job;

        Throwable thrown  = Assertions.catchThrowable(() -> template().requestBody("salesforce:bulk2GetQueryJob", finalJob, QueryJob.class));
        Assertions.assertThat(thrown).isInstanceOf(CamelExecutionException.class);
        CamelExecutionException ex = (CamelExecutionException) thrown;
        Assertions.assertThat(ex.getCause()).isInstanceOf(SalesforceException.class);
        SalesforceException sfEx = (SalesforceException) ex.getCause();
        Assertions.assertThat(sfEx.getStatusCode()).isEqualTo(404);
    }

    @Test
    public void testGetAll() {
        QueryJobs jobs = template().requestBody("salesforce:bulk2GetAllQueryJobs", "",
                QueryJobs.class);
        Assertions.assertThat(jobs).isNotNull();
    }

    /**
     *  Bulk API 2.0 is available in API version 41.0 and later.
     *  Query jobs in Bulk API 2.0 are available in API version 47.0 and later.
     */
    protected String salesforceApiVersionToUse() {
        return "47.0";
    }
}