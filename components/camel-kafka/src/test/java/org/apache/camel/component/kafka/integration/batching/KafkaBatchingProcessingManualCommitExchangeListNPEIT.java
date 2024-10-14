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
package org.apache.camel.component.kafka.integration.batching;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.apache.camel.component.kafka.integration.common.KafkaTestUtil;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaBatchingProcessingManualCommitExchangeListNPEIT extends BatchingProcessingITSupport {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaBatchingProcessingManualCommitExchangeListNPEIT.class);
    private static final int MAX_POLL_RECORDS = 50;

    public static final String TOPIC = "testBatchingProcessingManualCommit";
    private volatile boolean invalidExchangeFormat = false;

    @AfterEach
    public void after() {
        cleanupKafka(TOPIC);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // allowManualCommit=true&autoOffsetReset=earliest
        String from = "kafka:" + TOPIC
                      + "?groupId=KafkaBatchingProcessingIT"
                      + "&pollTimeoutMs=5000"
                      + "&maxPollIntervalMs=10000"
                      + "&batching=true"
                      + "&autoCommitEnable=false"
                      + "&breakOnFirstError=true"
                      + "&allowManualCommit=true"
                      + "&maxPollRecords=" + MAX_POLL_RECORDS
                      + "&commitTimeoutMs=10000"
                      + "&sessionTimeoutMs=90000"
                      + "&pollOnError=RETRY"
                      + "&autoOffsetReset=earliest"
                      + "&kafkaManualCommitFactory=#class:org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommitFactory";

        return new RouteBuilder() {

            @Override
            public void configure() {
                from(from).routeId("batching").process(e -> {
                    // The received records are stored as exchanges in a list. This gets the list of those exchanges
                    final List<?> exchanges = e.getMessage().getBody(List.class);

                    // Ensure we are actually receiving what we are asking for
                    if (exchanges == null || exchanges.isEmpty()) {
                        return;
                    }

                    /*
                    Every exchange in that list should contain a reference to the manual commit object. We use the reference
                    for the last exchange in the list to commit the whole batch
                     */
                    final Object tmp = exchanges.get(exchanges.size() - 1);
                    if (tmp instanceof Exchange exchange) {
                        KafkaManualCommit manual
                                = exchange.getMessage().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
                        LOG.debug("Performing manual commit");
                        manual.commit();
                        LOG.debug("Done performing manual commit");
                    } else {
                        invalidExchangeFormat = true;
                    }

                }).to(KafkaTestUtil.MOCK_RESULT);
            }
        };
    }

    @Test
    public void kafkaManualCommit() throws Exception {
        int recordsCount = 1_000_000;
        to.expectedMessageCount(recordsCount / MAX_POLL_RECORDS);

        for (int position = 0; position < recordsCount; position++) {
            String msg = "message-" + position;
            ProducerRecord<String, String> data = new ProducerRecord<>(TOPIC, 0, String.valueOf(position), msg);
            producer.send(data);
        }

        MockEndpoint.assertIsSatisfied(30, TimeUnit.SECONDS, to);

        final List<Exchange> firstExchangeBatch = to.getExchanges();
        validateReceivedExchanges(MAX_POLL_RECORDS, firstExchangeBatch);
        Assertions.assertFalse(invalidExchangeFormat, "The exchange list should be composed of exchanges");
    }

}
