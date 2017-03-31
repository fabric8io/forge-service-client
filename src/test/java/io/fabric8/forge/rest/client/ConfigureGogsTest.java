/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.client;

import io.fabric8.forge.rest.client.dto.CommandInputDTO;
import io.fabric8.forge.rest.client.dto.PropertyDTO;
import org.junit.Test;

/**
 */
public class ConfigureGogsTest extends ForgeTestSupport {

    @Test
    public void testConfigureGogs() throws Exception {
        CommandInputDTO info = forgeClient.getCommandInput(CommandNames.CONFIGURE_GIT_ACCOUNT);
        System.out.println("commandInput: " + info);
        ForgeClientAsserts.assertValidCommandInput(info);

        ValueProvider projectTypeValues = new ValueProvider() {
            @Override
            public Object getValue(String propertyName, PropertyDTO property, int pageNumber) {
                switch (propertyName) {
                    case "gitProvider":
                        return "gogs";
                    case "gitUserName":
                        return "gogsadmin";
                    case "gitEmail":
                        return "fabric8io@googlegroups.com";
                    case "gitPassword":
                        return "RedHat$1";
                }
                return super.getValue(propertyName, property, pageNumber);

            }
        };

        executeWizardCommand(CommandNames.CONFIGURE_GIT_ACCOUNT, projectTypeValues, 2);
    }
}
