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
import com.offbytwo.jenkins.client.JenkinsHttpClient;
import io.fabric8.forge.rest.client.dto.CommandInputDTO;
import io.fabric8.forge.rest.client.dto.ExecutionRequest;
import io.fabric8.forge.rest.client.dto.InputValueDTO;
import io.fabric8.forge.rest.client.dto.PropertyDTO;
import io.fabric8.forge.rest.client.dto.ValidationResult;
import io.fabric8.forge.rest.client.dto.WizardState;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.utils.Function;
import io.fabric8.utils.Strings;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
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

    public static JenkinsServer createJenkinsServer(final KubernetesClient kubernetesClient, String jenkinsNamespace) throws URISyntaxException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, InvalidKeySpecException, IOException {
        final Config config = kubernetesClient.getConfiguration();
        String url = getJenkinsURL(kubernetesClient, jenkinsNamespace);
        if (Strings.isNullOrBlank(url) || url.startsWith("http://null:")) {
            throw new IllegalArgumentException("No Jenkins Service found in namespace: " + jenkinsNamespace);
        }
        System.out.println("Connecting to jenkins at: " + url);
        URI serverUri = new URI(url);
        HttpClientBuilder builder = HttpClientBuilder.create();
        configureSsl(builder, config);
        builder.addInterceptorFirst(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                String oauthToken = config.getOauthToken();
                if (Strings.isNullOrBlank(oauthToken)) {
                    System.out.println("No OpenShift OAuth Token!");
                } else {
                    request.setHeader("Authorization", "Bearer " + oauthToken);
                }
            }
        });
        JenkinsHttpClient client = new JenkinsHttpClient(serverUri, builder);
        return new JenkinsServer(client);
/*
        String username = System.getenv("JENKINS_USER");
        String password = System.getenv("JENKINS_PASSWORD");
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            return new JenkinsServer(serverUri, username, password);
        }
        return new JenkinsServer(serverUri);
*/
    }

    protected static void configureSsl(HttpClientBuilder httpClientBuilder, Config config) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException, UnrecoverableKeyException, InvalidKeySpecException {
        httpClientBuilder.setHostnameVerifier(new X509HostnameVerifier() {
            @Override
            public void verify(String s, SSLSocket sslSocket) throws IOException {
            }

            @Override
            public void verify(String s, X509Certificate x509Certificate) throws SSLException {
            }

            @Override
            public void verify(String s, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        TrustManager[] trustManagers = SSLUtils.trustManagers(config);
        KeyManager[] keyManagers = SSLUtils.keyManagers(config);

        if (keyManagers != null || trustManagers != null || config.isTrustCerts()) {
            X509TrustManager trustManager = null;
            if (trustManagers != null && trustManagers.length == 1) {
                trustManager = (X509TrustManager) trustManagers[0];
            }

            try {
                SSLContext sslContext = SSLUtils.sslContext(keyManagers, trustManagers, config.isTrustCerts());
                httpClientBuilder.setSslcontext(sslContext);
                //httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            } catch (GeneralSecurityException e) {
                throw new AssertionError(); // The system has no TLS. Just give up.
            }
        } else {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(keyManagers, trustManagers, null);
            httpClientBuilder.setSslcontext(context);
            //httpClientBuilder.sslSocketFactory(context.getSocketFactory(), (X509TrustManager) trustManagers[0]);
        }
    }

    public static String getJenkinsURL(KubernetesClient kubernetesClient, String namespace) {
        String jenkinsUrl = KubernetesHelper.getServiceURL(kubernetesClient, ServiceNames.JENKINS, namespace, "http", true);
        if (Strings.isNotBlank(jenkinsUrl)) {
            return jenkinsUrl;
        }
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
