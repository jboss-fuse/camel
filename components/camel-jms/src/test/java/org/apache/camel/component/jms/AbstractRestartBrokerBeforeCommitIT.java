/*
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
package org.apache.camel.component.jms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.infra.common.TestUtils;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.util.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRestartBrokerBeforeCommitIT extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestartBrokerBeforeCommitIT.class);
    protected static final String ARTEMIS_USERNAME = "admin";
    protected static final String ARTEMIS_PASSWORD = "admin";
    protected static final String ARTEMIS_HOST = "localhost";
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final int DEFAULT_AMQP_PORT = 5672;
    private static final int DEFAULT_ADMIN_PORT = AvailablePortFinder.getNextAvailable();
    private static final int DEFAULT_ACCEPTOR_PORT = AvailablePortFinder.getNextAvailable();
    private static final String JMS_BROKER_ROOT = "/opt/camel-kafka-connector/artemis/";
    private static final String JMS_BROKER_ETC_OVERRIDE = JMS_BROKER_ROOT + "etc-override/broker.xml";
    private static final String BROKER_OVERRIDE_FILE_PATH = "org/apache/camel/component/jms/integration/artemis/broker.xml";
    protected final String SERVICE_ADDRESS = "jms:topic:artemis-demo-topic?jmsMessageType=Text";
    protected RestartAwareArtemisContainer broker;

    protected void performTest(int jmsExceptionCount, int okMessageExpectedCount, int exceptionMessageExpectedCount)
            throws IOException, InterruptedException, URISyntaxException {
        try {
            String uuid = UUID.randomUUID().toString();
            broker = new RestartAwareArtemisContainer();
            broker.start();

            MockEndpoint jmsException = getMockEndpoint("mock:jmsException");
            jmsException.setExpectedMessageCount(jmsExceptionCount);
            MockEndpoint okMock = getMockEndpoint("mock:ok");
            okMock.setExpectedMessageCount(okMessageExpectedCount);
            MockEndpoint genericExceptionMock = getMockEndpoint("mock:exception");
            genericExceptionMock.setExpectedMessageCount(exceptionMessageExpectedCount);
            template.send("seda:processMessage", ex -> {
                ex.getMessage().setHeader("TMPL_uuid", uuid);
                ex.getMessage().setBody("Hi!");
            });
            MockEndpoint.assertIsSatisfied(context, 30, SECONDS);
            int actualMessageCountonQueue = getMessageAdded();
            assertThat(actualMessageCountonQueue).isEqualTo(0);

        } finally {
            broker.stop();
        }
    }

    protected static int getMessageAdded() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(
                        "http://localhost:" + DEFAULT_ADMIN_PORT + "/console/jolokia/read/org.apache.activemq.artemis:broker=!%220.0.0.0!%22,component=addresses,address=!%22artemis-demo-topic!%22,subcomponent=queues,routing-type=!%22multicast!%22,queue=!%22sub1-artemis-demo-topic!%22/MessagesAdded"))
                .GET()
                .header("Authorization",
                        "Basic " + Base64.getEncoder().encodeToString((ARTEMIS_USERNAME + ":" + ARTEMIS_PASSWORD).getBytes()))
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isLessThan(300);
        LOG.info("Jolokia Response: ErrorCode: {} Body: {}", response.statusCode(), response.body());
        ObjectMapper mapper = new ObjectMapper();
        JsonObject jsonObject = mapper.readValue(response.body(), JsonObject.class);
        Integer value = jsonObject.getInteger("value");
        assertThat(value).isNotNull();
        return value;
    }

    @Override
    protected void bindToRegistry(Registry registry) {
        //Coonection Factory
        ActiveMQConnectionFactory factory
                = new ActiveMQConnectionFactory(ARTEMIS_USERNAME, ARTEMIS_PASSWORD, "tcp://" + ARTEMIS_HOST + ":" + DEFAULT_ACCEPTOR_PORT);
        registry.bind("factory", factory);

        // JMS Component and configuration that use JmsUtil.commitIfNecessary
        final CustomJmsConfiguration jmsConfiguration = getCustomJmsConfiguration(factory);
        registry.bind("jmsConfiguration", jmsConfiguration);

        //jms component
        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConfiguration(jmsConfiguration);
        registry.bind("jms", jmsComponent);
    }

    @NotNull
    private CustomJmsConfiguration getCustomJmsConfiguration( ActiveMQConnectionFactory factory) {
        CustomJmsConfiguration jmsConfiguration = new CustomJmsConfiguration();
        jmsConfiguration.setConnectionFactory(factory);
        jmsConfiguration.setTransacted(true);
        jmsConfiguration.setLazyCreateTransactionManager(true);
        jmsConfiguration.setDeliveryPersistent(true);
        jmsConfiguration.setRequestTimeout(10000);
        jmsConfiguration.setReceiveTimeout(1000);
        jmsConfiguration.setCacheLevelName("CACHE_NONE");
        jmsConfiguration.setAcknowledgementModeName("SESSION_TRANSACTED");
        return jmsConfiguration;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:processMessage")
                        .to(SERVICE_ADDRESS)
                        .to("mock:ok")
                        .log(LoggingLevel.INFO, "MESSAGE SENT: ${header.TMPL_uuid}")

                        .onException(JMSException.class)
                        .id("onJMSException")
                        .handled(true)
                        .maximumRedeliveries(0)
                        .logStackTrace(false)
                        .log(LoggingLevel.ERROR,
                                "MESSAGE Failed: ${header.TMPL_uuid} \n***** JMSException received *****: ${exception.message}")
                        .to("mock:jmsException")
                        .end()
                        .onException(Throwable.class)
                        .id("onException")
                        .handled(true)
                        .maximumRedeliveries(0)
                        .to("mock:exception")
                        .end();
            }
        };
    }

    // CUSTOM JMS CONFIGURATION
    class CustomJmsConfiguration extends JmsConfiguration {
        @Override
        protected CamelJmsTemplate createCamelJmsTemplate(ConnectionFactory connectionFactory) {
            return new CustomCamelJmsTemplate(this, connectionFactory);
        }
    }

    private  String loadFileContentFromResources(String filePath) throws IOException {
        ClassLoader classLoader = RestartAwareArtemisContainer.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(filePath)) {
            // the stream holding the file content
            if (inputStream == null) {
                throw new IllegalArgumentException("file not found! " + filePath);
            } else {
                return new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        }
    }

    protected abstract void commit(Session session) throws JMSException;

    // CUSTOM JMS TEMPLATE
    class CustomCamelJmsTemplate extends JmsConfiguration.CamelJmsTemplate {

        public CustomCamelJmsTemplate(JmsConfiguration config, ConnectionFactory connectionFactory) {
            super(config, connectionFactory);
        }

        @Override
        protected void commitIfNecessary(Session session) throws JMSException {
            // Transacted session created by this template -> commit.
            Assert.notNull(session, "Session must not be null");

            // Check commit - avoid commit call within a JTA transaction.
            if (session.getTransacted() && isSessionLocallyTransacted(session)) {
                broker.restart();
                commit(session);
            }
        }
    }

    // Restart Aware Artemis Container
    class RestartAwareArtemisContainer extends GenericContainer<RestartAwareArtemisContainer> {

        private static final String FROM_IMAGE_NAME = "fedora:38";
        private static final String FROM_IMAGE_ARG = "FROMIMAGE";

        public RestartAwareArtemisContainer() throws IOException {
            super(new ImageFromDockerfile("localhost/apache-artemis:camel", false)
                    .withFileFromClasspath("Dockerfile",
                            "org/apache/camel/component/jms/integration/artemis/Dockerfile")
                    .withBuildArg(FROM_IMAGE_ARG, TestUtils.prependHubImageNamePrefixIfNeeded(FROM_IMAGE_NAME)));
//            waitingFor(Wait.forLogMessage(".*AMQ241004: Artemis Console available at.*\\n", 1));
            setPortBindings(Arrays.asList(DEFAULT_ACCEPTOR_PORT+":61616", DEFAULT_ADMIN_PORT+":8161"));
            withEnv("ARTEMIS_USER", ARTEMIS_USERNAME );
            withEnv("ARTEMIS_PASSWORD", ARTEMIS_PASSWORD);
            withLogConsumer(new Slf4jLogConsumer(LOG));
            waitingFor(Wait.forLogMessage(".*AMQ241004: Artemis Console available at.*",1));
            String brokerXmlQueue = loadFileContentFromResources(BROKER_OVERRIDE_FILE_PATH);
            withCopyToContainer(Transferable.of(brokerXmlQueue, 0777), JMS_BROKER_ETC_OVERRIDE);
        }

        public void restart() {
            String tag = this.getContainerId();
            String snapshotId = dockerClient.commitCmd(this.getContainerId())
                    .withRepository("tempimg")
                    .withTag(tag).exec();
            this.stop();
            this.setDockerImageName("tempimg:" + tag);
            this.start();
        }

        /**
         * Gets the port number used for exchanging messages using the AMQP protocol
         *
         * @return the port number
         */
        public int amqpPort() {
            return getMappedPort(DEFAULT_AMQP_PORT);
        }

        /**
         * Gets the end point URL used exchanging messages using the AMQP protocol (ie.: tcp://host:${amqp.port})
         *
         * @return the end point URL as a string
         */
        public String amqpEndpoint() {
            return String.format("amqp://%s:%d", getHost(), amqpPort());
        }

        /**
         * Gets the port number used for exchanging messages using the MQTT protocol
         *
         * @return the port number
         */
        public int mqttPort() {
            return getMappedPort(DEFAULT_MQTT_PORT);
        }

        /**
         * Gets the end point URL used exchanging messages using the MQTT protocol (ie.: tcp://host:${mqtt.port})
         *
         * @return the end point URL as a string
         */
        public String mqttEndpoint() {
            return String.format("tcp://%s:%d", getHost(), mqttPort());
        }

        /**
         * Gets the port number used for accessing the web management console or the management API
         *
         * @return the port number
         */
        public int adminPort() {
            return getMappedPort(DEFAULT_ADMIN_PORT);
        }

        /**
         * Gets the end point URL used for accessing the web management console or the management API
         *
         * @return the admin URL as a string
         */
        public String adminURL() {
            return String.format("http://%s:%d", getHost(), adminPort());
        }

        /**
         * Gets the port number used for exchanging messages using the default acceptor port
         *
         * @return the port number
         */
        public int defaultAcceptorPort() {
            return getMappedPort(DEFAULT_ACCEPTOR_PORT);
        }

        /**
         * Gets the end point URL used exchanging messages through the default acceptor port
         *
         * @return the end point URL as a string
         */
        public String defaultEndpoint() {
            return String.format("tcp://%s:%d", getHost(), defaultAcceptorPort());
        }

        /**
         * Gets the port number used for exchanging messages using the Openwire protocol
         *
         * @return the port number
         */
        public int openwirePort() {
            return defaultAcceptorPort();
        }

        /**
         * Gets the end point URL used exchanging messages using the Openwire protocol (ie.: tcp://host:${amqp.port})
         *
         * @return the end point URL as a string
         */
        public String getOpenwireEndpoint() {
            return String.format("tcp://%s:%d", getHost(), openwirePort());
        }
    }




}
