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

import java.util.Properties;
import org.apache.camel.component.properties.DefaultPropertiesParser;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;

import static org.jasypt.properties.PropertyValueEncryptionUtils.decrypt;
import static org.jasypt.properties.PropertyValueEncryptionUtils.isEncryptedValue;


public class EncryptedPropertiesSpringParser extends DefaultPropertiesParser {

    private PropertyResolver propertyResolver;

    private StringEncryptor stringEncryptor;
    @Autowired
    public EncryptedPropertiesSpringParser(PropertyResolver propertyResolver, StringEncryptor stringEncryptor) {
        this.propertyResolver = propertyResolver;
        this.stringEncryptor = stringEncryptor;
    }

    public String parseProperty(String key, String value, Properties properties) {
        String originalValue = propertyResolver.getProperty(key);
        return isEncryptedValue(originalValue) ? decrypt(originalValue, stringEncryptor) : originalValue;
    }
}