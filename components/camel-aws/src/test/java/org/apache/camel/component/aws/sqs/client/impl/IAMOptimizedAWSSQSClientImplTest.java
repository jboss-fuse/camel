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

package org.apache.camel.component.aws.sqs.client.impl;

import com.amazonaws.services.sqs.AmazonSQS;

import org.apache.camel.component.aws.sqs.SqsConfiguration;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic testing to ensure that the IAMOptimizedAWSS3ClientImplTest class is returning a standard client that is
 * capable of encryption given certain parameters. This client is new to Camel as of 02-15-2018 and enables IAM
 * temporary credentials to improve security.
 */
public class IAMOptimizedAWSSQSClientImplTest {

    @Test
    public void iamOptimizedAWSS3ClientImplNoEncryption() {
        SQSClientIAMOptimizedImpl iamOptimizedAWSSQSClient = new SQSClientIAMOptimizedImpl(getSQSConfiguration());
        AmazonSQS sqsClient = iamOptimizedAWSSQSClient.getSQSClient();
        Assert.assertNotNull(sqsClient);
    }

    private SqsConfiguration getSQSConfiguration() {
        SqsConfiguration sqsConfiguration = mock(SqsConfiguration.class);
        when(sqsConfiguration.getRegion()).thenReturn("US_EAST_1");
        return sqsConfiguration;
    }
}
