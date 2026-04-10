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
package org.apache.camel.telemetry.propagation;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.telemetry.SpanContextPropagationExtractor;
import org.apache.camel.telemetry.decorators.JmsSpanDecorator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.camel.telemetry.propagation.CamelJMSHeadersSpanContextPropagationInjector.JMS_DASH;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that JMS dash-encoded header keys are properly decoded when extracting span context via JmsSpanDecorator.
 */
public class CamelMessagingHeadersExtractAdapterTest {

    private SpanContextPropagationExtractor createJmsExtractor(Map<String, Object> headers) {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getHeaders()).thenReturn(headers);
        return new JmsSpanDecorator().getExtractor(exchange);
    }

    @Test
    public void propertyWithDash() {
        Map<String, Object> map = new HashMap<>();
        map.put(JMS_DASH + "key" + JMS_DASH + "1" + JMS_DASH, "value1");
        SpanContextPropagationExtractor adapter = createJmsExtractor(map);
        assertEquals("value1", adapter.get("-key-1-"));
    }

    @Test
    public void traceparentWithEncodedDashes() {
        Map<String, Object> map = new HashMap<>();
        map.put("traceparent", "00-abc123-def456-01");
        SpanContextPropagationExtractor adapter = createJmsExtractor(map);
        assertEquals("00-abc123-def456-01", adapter.get("traceparent"));
    }

    @Test
    public void keyWithDifferentCase() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        SpanContextPropagationExtractor adapter = createJmsExtractor(map);
        assertEquals("value", adapter.get("KeY"));
    }

    @Test
    public void byteArrayPropertyWithDashDecode() {
        Map<String, Object> map = new HashMap<>();
        map.put(JMS_DASH + "traceparent", "00-abc-def-01".getBytes());
        SpanContextPropagationExtractor adapter = createJmsExtractor(map);
        assertEquals("00-abc-def-01", adapter.get("-traceparent"));
    }
}
