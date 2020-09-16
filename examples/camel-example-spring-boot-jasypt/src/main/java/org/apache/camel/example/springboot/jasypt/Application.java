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

import org.apache.camel.component.properties.PropertiesParser;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;

// CHECKSTYLE:OFF
@SpringBootApplication
public class Application {

    private static final String ENCRYPTION_ALGORITHM = "PBEWITHHMACSHA256ANDAES_256";

    private static final String MASTER_PASSWORD_VARIABLE_NAME = "JASYPT_ENCRYPTION_PASSWORD";

    private static final String SECURE_RANDOM_ALGORITHM = "NativePRNG";


    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public EnvironmentStringPBEConfig environmentVariablesConfiguration() {
        EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
        environmentStringPBEConfig.setAlgorithm(ENCRYPTION_ALGORITHM);
        environmentStringPBEConfig.setPasswordEnvName(MASTER_PASSWORD_VARIABLE_NAME);
        // Set the IVGenerator only if an initialization vector is used during encryption
        environmentStringPBEConfig.setIvGenerator(new RandomIvGenerator(SECURE_RANDOM_ALGORITHM));
        environmentStringPBEConfig.setSaltGenerator(new RandomSaltGenerator(SECURE_RANDOM_ALGORITHM));
        return environmentStringPBEConfig;
    }

    @Bean
    public StringEncryptor configurationEncryptor(EnvironmentStringPBEConfig environmentVariablesConfiguration) {
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setConfig(environmentVariablesConfiguration);
        return standardPBEStringEncryptor;
    }

    @Bean
    public static EncryptablePropertyConfigurer propertyConfigurer(StringEncryptor stringEncryptor) {
        return new EncryptablePropertyConfigurer(stringEncryptor);
    }

    @Bean
    public PropertiesParser propertyParser(PropertyResolver propertyResolver, StringEncryptor stringEncryptor) {
        return new EncryptedPropertiesSpringParser(propertyResolver,stringEncryptor);
    }
}
// CHECKSTYLE:ON
