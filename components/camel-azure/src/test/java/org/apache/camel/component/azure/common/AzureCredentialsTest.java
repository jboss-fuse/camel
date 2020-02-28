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
package org.apache.camel.component.azure.common;

import java.net.URI;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import org.apache.camel.component.azure.blob.BlobServiceComponent;
import org.apache.camel.component.azure.blob.BlobServiceConfiguration;
import org.apache.camel.component.azure.blob.BlobServiceEndpoint;
import org.apache.camel.component.azure.blob.BlobServiceUtil;
import org.apache.camel.component.azure.queue.QueueServiceComponent;
import org.apache.camel.component.azure.queue.QueueServiceConfiguration;
import org.apache.camel.component.azure.queue.QueueServiceEndpoint;
import org.apache.camel.component.azure.queue.QueueServiceUtil;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;


public class AzureCredentialsTest extends CamelTestSupport {

    private String accountName = "camelazure";
    private String containerName = "container1";
    private String blobName = "blobBlock";
    private String credentialsAccountName = "xxxx";
    private String credentialsAccountKey = "Y3l5eXl+5Kw=";
    private String queueName = "myQueue";
    private StorageCredentialsAccountAndKey creds = new StorageCredentialsAccountAndKey("aaaa", "bbbb");

// Blob Tests

    private String inlineCredentialBlobURIEndpoint = new StringBuilder()
            .append("azure-blob://")
            .append(accountName)
            .append("/").append(containerName)
            .append("/").append(blobName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .toString();


    private String inlineCredentialBlobURIAndCredentialRefEndpoint = new StringBuilder()
            .append("azure-blob://")
            .append(accountName)
            .append("/").append(containerName)
            .append("/").append(blobName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .append("&credentials=#creds")
            .toString();

    private String inlineCredentialBlobURIAndCredentialRefEndpointAndAzureClient = new StringBuilder()
            .append("azure-blob://")
            .append(accountName)
            .append("/").append(containerName)
            .append("/").append(blobName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .append("&credentials=#creds")
            .append("&azureBlobClient=#blobClient")
            .toString();

    private String inlineCredentialQueueURIEndpoint = new StringBuilder()
            .append("azure-queue://")
            .append(accountName)
            .append("/").append(queueName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .toString();


    private String inlineCredentialQueueURIAndCredentialRefEndpoint = new StringBuilder()
            .append("azure-queue://")
            .append(accountName)
            .append("/").append(queueName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .append("&credentials=#creds")
            .toString();

    private String inlineCredentialQueueURIAndCredentialRefEndpointAndAzureClient = new StringBuilder()
            .append("azure-queue://")
            .append(accountName)
            .append("/").append(queueName).append("?")
            .append("credentialsAccountName=").append(credentialsAccountName)
            .append("&credentialsAccountKey=RAW(").append(credentialsAccountKey).append(")")
            .append("&credentials=#creds")
            .append("&azureQueueClient=#queueClient")
            .toString();



    @Test
    public void createBlobEndpointWithAccountCredentials() throws Exception {
        executeBlobAssertions(inlineCredentialBlobURIEndpoint, credentialsAccountName, credentialsAccountKey);
    }

    @Test
    public void createBlobEndpointWithAccountCredentialsAndCredentalRef() throws Exception {
        executeBlobAssertions(inlineCredentialBlobURIAndCredentialRefEndpoint, "aaaa", "bbbb");
    }

    @Test
    public void createBlobEndpointWithAccountCredentialsAndCredentalRefAndAzureClient() throws Exception {
        BlobServiceComponent component = new BlobServiceComponent(context);
        BlobServiceEndpoint endpoint = (BlobServiceEndpoint) component.createEndpoint(inlineCredentialBlobURIAndCredentialRefEndpointAndAzureClient);
        CloudBlob client = context.getRegistry().lookupByNameAndType("blobClient", CloudBlockBlob.class);
        executeBlobAccountCredentialsAssertion(client, endpoint.getConfiguration());
        executeBlobCredentialsAssertion(client, "cccc", "dddd");
    }



    // Queue Tests

    @Test
    public void createQueueEndpointWithAccountCredentials() throws Exception {
        executeQueueAssertions(inlineCredentialQueueURIEndpoint, credentialsAccountName, credentialsAccountKey);
    }

    @Test
    public void createQueueEndpointWithAccountCredentialsAndCredentalRef() throws Exception {
        executeQueueAssertions(inlineCredentialQueueURIAndCredentialRefEndpoint, "aaaa", "bbbb");
    }

    @Test
    public void createQueueEndpointWithAccountCredentialsAndCredentalRefAndAzureClient() throws Exception {
        QueueServiceComponent component = new QueueServiceComponent(context);
        QueueServiceEndpoint endpoint = (QueueServiceEndpoint) component.createEndpoint(inlineCredentialQueueURIAndCredentialRefEndpointAndAzureClient);
        CloudQueue client = context.getRegistry().lookupByNameAndType("queueClient", CloudQueue.class);
        executeQueueAccountCredentialsAssertion(client, endpoint.getConfiguration());
        executeQueueCredentialsAssertion(client, "cccc", "dddd");
    }

    private void executeQueueAssertions(String uriString, String expectedAccountName, String  expectedAccountKey) throws Exception {
        QueueServiceComponent component = new QueueServiceComponent(context);
        QueueServiceEndpoint endpoint = (QueueServiceEndpoint) component.createEndpoint(uriString);

        CloudQueue queueClient = QueueServiceUtil.createQueueClient(endpoint.getConfiguration());
        executeQueueAccountCredentialsAssertion(queueClient, endpoint.getConfiguration());
        executeQueueCredentialsAssertion(queueClient, expectedAccountName, expectedAccountKey);

    }


    private void executeQueueAccountCredentialsAssertion(CloudQueue client, QueueServiceConfiguration configuration) {
        assertNotNull(client);
        assertEquals(accountName, configuration.getAccountName());
        assertEquals(credentialsAccountName, configuration.getCredentialsAccountName());
        assertEquals(credentialsAccountKey, configuration.getCredentialsAccountKey());
    }

    private void executeQueueCredentialsAssertion(CloudQueue client, String expectedAccountName, String expectedAccountKey) {
        assertNotNull(client.getServiceClient().getCredentials());

        assertEquals(expectedAccountKey, ((StorageCredentialsAccountAndKey)client.getServiceClient()
                .getCredentials()).exportBase64EncodedKey());

        assertEquals(expectedAccountName, (client.getServiceClient().getCredentials()).getAccountName());
    }

    private void executeBlobAssertions(String uriString, String expectedAccountName, String  expectedAccountKey) throws Exception {
        BlobServiceComponent component = new BlobServiceComponent(context);
        BlobServiceEndpoint endpoint = (BlobServiceEndpoint) component.createEndpoint(uriString);

        CloudBlob pageBlobClient = BlobServiceUtil.createPageBlobClient(endpoint.getConfiguration());
        executeBlobAccountCredentialsAssertion(pageBlobClient, endpoint.getConfiguration());
        executeBlobCredentialsAssertion(pageBlobClient, expectedAccountName, expectedAccountKey);

        CloudBlob blockBlobClient = BlobServiceUtil.createBlockBlobClient(endpoint.getConfiguration());
        executeBlobAccountCredentialsAssertion(blockBlobClient, endpoint.getConfiguration());
        executeBlobCredentialsAssertion(blockBlobClient, expectedAccountName, expectedAccountKey);

        CloudBlob appendBlobClient = BlobServiceUtil.createAppendBlobClient(endpoint.getConfiguration());
        executeBlobAccountCredentialsAssertion(appendBlobClient, endpoint.getConfiguration());
        executeBlobCredentialsAssertion(appendBlobClient, expectedAccountName, expectedAccountKey);
    }


    private void executeBlobAccountCredentialsAssertion(CloudBlob client, BlobServiceConfiguration configuration) {
        assertNotNull(client);
        assertEquals(accountName, configuration.getAccountName());
        assertEquals(containerName, configuration.getContainerName());
        assertEquals(blobName, configuration.getBlobName());
        assertEquals(credentialsAccountName, configuration.getCredentialsAccountName());
        assertEquals(credentialsAccountKey, configuration.getCredentialsAccountKey());
    }

    private void executeBlobCredentialsAssertion(CloudBlob client, String expectedAccountName, String expectedAccountKey) {
        assertNotNull(client.getServiceClient().getCredentials());

        assertEquals(expectedAccountKey, ((StorageCredentialsAccountAndKey)client.getServiceClient()
                .getCredentials()).exportBase64EncodedKey());

        assertEquals(expectedAccountName, (client.getServiceClient().getCredentials()).getAccountName());
    }

    private static CloudBlockBlob createBlockBlobClient() throws Exception {
        URI uri = new URI("https://camelazure.blob.core.windows.net/container1/blobBlock");
        StorageCredentials creds = new StorageCredentialsAccountAndKey("cccc", "dddd");
        CloudBlockBlob client = new CloudBlockBlob(uri, creds);
        return client;
    }
    private static CloudQueue createQueueClient() throws Exception {
        URI uri = new URI("https://camelazure.queue.core.windows.net/testqueue/");
        StorageCredentials creds = new StorageCredentialsAccountAndKey("cccc", "dddd");
        CloudQueue client = new CloudQueue(uri, creds);
        return client;
    }


    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();

        registry.bind("creds", creds);
        registry.bind("blobClient", createBlockBlobClient());
        registry.bind("queueClient", createQueueClient());

        return registry;
    }
}
