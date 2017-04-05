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
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.View;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;

import static io.fabric8.forge.rest.client.ForgeClientHelpers.createJenkinsServer;

/**
 */
public class JenkinsListJobs {
    public static void main(String[] args) {
        try {
            KubernetesClient kubernetesClient = new DefaultKubernetesClient();
            String jenkinsNamespace = kubernetesClient.getNamespace();
            if (args.length > 0) {
                jenkinsNamespace = args[0];
            }
            JenkinsServer jenkins = createJenkinsServer(kubernetesClient, jenkinsNamespace);

            System.out.println("Views:");
            Map<String, View> views = jenkins.getViews();
            for (Map.Entry<String, View> entry : views.entrySet()) {
                System.out.println("" + entry.getKey() + ": " + entry.getValue().getUrl());
            }

            System.out.println("Jobs:");
            Map<String, Job> jobs = jenkins.getJobs();
            for (Map.Entry<String, Job> entry : jobs.entrySet()) {
                System.out.println("" + entry.getKey() + ": " + entry.getValue().getUrl());
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();;
        }
    }
}
