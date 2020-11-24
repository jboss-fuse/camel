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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import org.apache.camel.component.aws.sqs.SqsConfiguration;
import org.apache.camel.component.aws.sqs.client.SQSClient;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage an AWS sqs client for all users to use.
 * This implementation is for local instances to use a static and solid credential set.
 */
public class SQSClientStandardImpl implements SQSClient {
    private static final Logger LOG = LoggerFactory.getLogger(SQSClientStandardImpl.class);
    private SqsConfiguration configuration;

    /**
     * Constructor that uses the config file.
     */
    public SQSClientStandardImpl(SqsConfiguration configuration) {
        LOG.trace("Creating an AWS SQS manager using static credentials.");
        this.configuration = configuration;
    }

    /**
     * Getting the sqs aws client that is used.
     * @return Amazon SQS Client.
     */
	@Override
	public AmazonSQS getSQSClient() {
        AmazonSQS client = null;
        AmazonSQSClientBuilder clientBuilder = null;
        ClientConfiguration clientConfiguration = null;
        boolean isClientConfigFound = false;
        if (ObjectHelper.isNotEmpty(configuration.getProxyHost()) && ObjectHelper.isNotEmpty(configuration.getProxyPort())) {
            clientConfiguration = new ClientConfiguration();
            clientConfiguration.setProxyHost(configuration.getProxyHost());
            clientConfiguration.setProxyPort(configuration.getProxyPort());
            isClientConfigFound = true;
        }
        if (configuration.getAccessKey() != null && configuration.getSecretKey() != null) {
            AWSCredentials credentials = new BasicAWSCredentials(configuration.getAccessKey(), configuration.getSecretKey());
            AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
            if (isClientConfigFound) {
                clientBuilder = AmazonSQSClientBuilder.standard().withClientConfiguration(clientConfiguration).withCredentials(credentialsProvider);
            } else {
                clientBuilder = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider);
            }
        } else {
            if (isClientConfigFound) {
                clientBuilder = AmazonSQSClientBuilder.standard();
            } else {
                clientBuilder = AmazonSQSClientBuilder.standard().withClientConfiguration(clientConfiguration);
            }
        }
        if (ObjectHelper.isNotEmpty(configuration.getRegion())) {
            clientBuilder = clientBuilder.withRegion(Regions.valueOf(configuration.getRegion()));
        }
        client = clientBuilder.build();
        return client;
	}
}
