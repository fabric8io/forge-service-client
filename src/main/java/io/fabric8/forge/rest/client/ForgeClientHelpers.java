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

import com.offbytwo.jenkins.JenkinsServer;
import io.fabric8.forge.rest.client.dto.CommandInputDTO;
import io.fabric8.forge.rest.client.dto.ExecutionRequest;
import io.fabric8.forge.rest.client.dto.InputValueDTO;
import io.fabric8.forge.rest.client.dto.PropertyDTO;
import io.fabric8.forge.rest.client.dto.ValidationResult;
import io.fabric8.forge.rest.client.dto.WizardState;
import io.fabric8.utils.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.forge.rest.client.EnvironmentVariables.getEnvironmentValue;

/**
 */
public class ForgeClientHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(ForgeClientHelpers.class);

    public static Map<String, Object> getLastPage(ExecutionRequest executionRequest) {
        Map<String, Object> page = new HashMap<>();
        List<InputValueDTO> inputList = executionRequest.getInputs();
        if (inputList != null) {
            for (InputValueDTO value : inputList) {
                page.put(value.getName(), value.getValue());
            }
        }
        return page;
    }

    public static Map<String, PropertyDTO> getCommandProperties(CommandInputDTO commandInput) {
        if (commandInput != null) {
            return inputsAsMap(commandInput.getInputs());
        }
        return Collections.EMPTY_MAP;
    }

    public static Map<String, PropertyDTO> inputsAsMap(List<PropertyDTO> inputs) {
        Map<String, PropertyDTO> properties = new HashMap<>();
        if (inputs != null) {
            for (PropertyDTO input : inputs) {
                properties.put(input.getName(), input);
            }
        }
        return properties;
    }

    public static Map<String, PropertyDTO> getCommandProperties(WizardState result) {

        // TODO
/*
        if (result != null) {
            return getCommandProperties(result.getWizardResults());
        }
*/
        return Collections.EMPTY_MAP;
    }

    public static Map<String, PropertyDTO> getCommandProperties(ValidationResult result) {
        return inputsAsMap(result.getInputs());
    }

    /*protected static Map<String, PropertyDTO> getCommandProperties(WizardResultsDTO wizardResults) {
        if (wizardResults != null) {
            List<CommandInputDTO> stepInputs = wizardResults.getStepInputs();
            if (stepInputs != null) {
                int size = stepInputs.size();
                if (size > 0) {
                    CommandInputDTO commandInput = stepInputs.get(size - 1);
                    return getCommandProperties(commandInput);
                }
            }
        }
        return Collections.EMPTY_MAP;
    }*/

    public static Map<String, Object> addPage(List<InputValueDTO> inputList, Map<String, PropertyDTO> properties, ValueProvider valueProvider) {
        Map<String, Object> page = createPage(inputList, properties, valueProvider);
        addPageValues(inputList, page);
        return page;
    }

    public static void addPageValues(List<InputValueDTO> inputList, Map<String, Object> page) {
        Set<Map.Entry<String, Object>> entries = page.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            Object value = entry.getValue();
            setInputListValue(inputList, name, value);
        }
    }

    private static void setInputListValue(List<InputValueDTO> inputList, String name, Object value) {
        for (InputValueDTO input : inputList) {
            if (input.getName().equals(name)) {
                input.setValue(value);
                return;
            }
        }
        inputList.add(new InputValueDTO(name, value));
    }

    protected static Map<String, Object> createPage(List<InputValueDTO> inputList, Map<String, PropertyDTO> properties, ValueProvider valueProvider) {
        Map<String, Object> page = new HashMap<>();
        updatePageValues(inputList, properties, valueProvider, page);
        return page;
    }

    public static void updatePageValues(List<InputValueDTO> inputList, Map<String, PropertyDTO> properties, ValueProvider valueProvider, Map<String, Object> page) {
        if (properties != null) {
            for (Map.Entry<String, PropertyDTO> entry : properties.entrySet()) {
                String key = entry.getKey();
                PropertyDTO property = entry.getValue();
                int pageNumber = inputList.size();
                Object value = valueProvider.getValue(key, property, pageNumber);
                if (value != null) {
                    page.put(key, value);
                }
            }
        }
    }

    public static JenkinsServer createJenkinsServer() throws URISyntaxException {
        String url = getJenkinsURL();
        return new JenkinsServer(new URI(url));
    }

    public static String getJenkinsURL() {
        return getEnvironmentValue(EnvironmentVariables.JENKINS_URL, "http://jenkins/");
    }


    /**
     * Tails the log of the given URL such as a build log, processing all new lines since the last results
     */
    public static TailResults tailLog(String uri, TailResults previousResults, Function<String, Void> lineProcessor) throws IOException {
        URL logURL = new URL(uri);
        try (InputStream inputStream = logURL.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            int count = 0;
            String lastLine = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lastLine = line;
                if (previousResults.isNewLine(line, count)) {
                    lineProcessor.apply(line);
                }
                count++;
            }
            return new TailResults(count, lastLine);
        }
    }
}
