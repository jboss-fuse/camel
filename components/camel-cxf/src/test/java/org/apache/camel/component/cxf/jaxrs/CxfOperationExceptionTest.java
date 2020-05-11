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

package org.apache.camel.component.cxf.jaxrs;

import javax.ws.rs.core.Response;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.component.cxf.CXFTestSupport;
import org.apache.camel.component.cxf.jaxrs.testbean.Customer;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.camel.Exchange.HTTP_METHOD;


public class CxfOperationExceptionTest extends CamelSpringTestSupport {
    private static final int PORT1 = CXFTestSupport.getPort1();

    private static final Logger LOG = LoggerFactory.getLogger(CxfOperationExceptionTest.class);


    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/camel/component/cxf/jaxrs/CxfOperationException.xml");
    }

    @Test(expected = CamelExecutionException.class)
    public void testRestServerDirectlyAddCustomer() {
        Customer input = new Customer();
        input.setName("Donald Duck");

        // we cannot convert directly to Customer as we need camel-jaxb
        String response = template.requestBodyAndHeader("cxfrs:http://localhost:" + PORT1 + "/CxfOperationExceptionTest/customerservice/customers?throwExceptionOnFailure=true", input,
            HTTP_METHOD, POST, String.class);

        assertNotNull(response);
        assertTrue(response.endsWith("<name>Donald Duck</name></Customer>"));
    }

    @Test
    public void testRestServerDirectlyAddCustomerWithExceptionsTurnedOff() {
        Customer input = new Customer();
        input.setName("Donald Duck");

        // we cannot convert directly to Customer as we need camel-jaxb
        Response response = template.requestBodyAndHeader("cxfrs:bean:rsClient?throwExceptionOnFailure=false", input,
                HTTP_METHOD, POST, Response.class);

        LOG.info("***response***: {}", response);

        assertNotNull(response);
        assertEquals(NOT_FOUND, response.getStatusInfo().toEnum());
    }
}
