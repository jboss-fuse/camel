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
package org.apache.camel.example.springboot.jasypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A bean that logs a message when you call the {@link #testEncryptedProperty()} method.
 * <p/>
 * Uses <tt>@Component("encryptedPropertiesBean")</tt> to register this bean with the name <tt>encryptedPropertiesBean</tt>
 * that we use in the Camel route to lookup this bean.
 */
@Component("encryptedPropertiesBean")
public class EncryptedPropertiesBean {

    private static final Logger LOG = LoggerFactory.getLogger(EncryptedPropertiesBean.class);

    @Value("${encrypted.password}")
    private String encryptedPassword;

    @Value("${unnencrypted.property}")
    private String unencryptedProperty;

    public void testEncryptedProperty() {
        LOG.info("test properties decryption outside camel context: test.password        = {}", encryptedPassword);
        LOG.info("test unencrypted properties outside camel context: unnencrypted.property  = {}", unencryptedProperty);
    }
}
