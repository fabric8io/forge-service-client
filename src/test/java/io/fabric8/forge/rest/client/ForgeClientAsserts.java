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
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import io.fabric8.forge.rest.client.dto.CommandInputDTO;
import io.fabric8.forge.rest.client.dto.ExecutionRequest;
import io.fabric8.forge.rest.client.dto.ExecutionResult;
import io.fabric8.forge.rest.client.dto.PropertyDTO;
import io.fabric8.forge.rest.client.dto.ValidationResult;
import io.fabric8.forge.rest.client.dto.WizardState;
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HasMetadataAssert;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigAssert;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Files;
import io.fabric8.utils.Function;
import io.fabric8.utils.URLUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static io.fabric8.forge.rest.client.ForgeClientHelpers.createJenkinsServer;
import static io.fabric8.forge.rest.client.ForgeClientHelpers.getJenkinsURL;
import static io.fabric8.forge.rest.client.ForgeClientHelpers.tailLog;
import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateAnnotations;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ForgeClientAsserts {
    private static final transient Logger LOG = LoggerFactory.getLogger(ForgeClientAsserts.class);

    private static final long DEFAULT_TIMEOUT_MILLIS = 60 * 60 * 1000;

    protected static boolean doAssert = true;

    public static File getBasedir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    public static void assertValidAndCanExecute(ValidationResult result) {
        assertThat(result).isNotNull();

        if (doAssert) {
            assertThat(result.isValid()).describedAs("isValid").isTrue();
            assertThat(result.isCanExecute()).describedAs("isCanExecute").isTrue();
        } else {
            LOG.info("isValid: " + result.isValid());
            LOG.info("isCanExecute: " + result.isCanExecute());
        }
    }


    public static void assertValidAndCanMoveNext(ExecutionRequest executionRequest, ValidationResult result) {
        assertThat(result).isNotNull();
        int stepIndex = executionRequest.stepIndex();
        String prefix = "page " + stepIndex + ": ";

        if (doAssert) {
            assertThat(result.isValid()).describedAs(prefix + "isValid " + result.validationMessage()).isTrue();
            assertThat(result.isCanMoveToNextStep()).describedAs(prefix + "isCanMoveToNextStep "+ result.validationMessage()).isTrue();
        } else {
            LOG.info(prefix + "isValid: " + result.isValid() + " " + result.validationMessage());
            LOG.info(prefix + "isCanMoveToNextStep: " + result.isCanMoveToNextStep()+ " " + result.validationMessage());
        }
    }


    public static void assertExecutionWorked(ExecutionResult result) {
        LOG.info("Got result: " + result);
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).describedAs("isSuccessful: " + result).isTrue();
    }

    public static Object assertChooseValue(String propertyName, PropertyDTO property, int pageNumber, String value) {
        List<Object> valueChoices = property.getValueChoices();
        if (valueChoices == null || valueChoices.isEmpty()) {
            valueChoices = property.getTypeaheadData();
        }
        if (valueChoices == null || valueChoices.isEmpty()) {
            // lets assume that we are in the initial request - and that validate will populate this!
            return null;
        }

        Object text = null;
        boolean contained = valueChoices.contains(value);
        if (!contained) {
            // we may have maps containing a value property
            for (Object choice : valueChoices) {
                if (choice instanceof Map) {
                    Map map = (Map) choice;
                    text = map.get("value");
                    if (text == null) {
                        text = map.get("id");
                    }
                    if (text == null) {
                        text = map.get("name");
                    }
                    if (text != null && value.equals(text)) {
                        contained = true;
                        break;
                    }

                }
            }
        }
        assertThat(contained).describedAs(("Choices for property " + propertyName + " on page " + pageNumber + " with choices: " + valueChoices) + " does not contain " + value).isTrue();
        return text;
    }


    /**
     * Asserts that a Build is created and that it completes successfully within the default time period
     */
    public static Build assertBuildCompletes(ForgeClient forgeClient, String projectName) throws Exception {
        return assertBuildCompletes(forgeClient, projectName, DEFAULT_TIMEOUT_MILLIS);
    }


    public static String asserGetAppGitCloneURL(ForgeClient forgeClient, String projectName) throws URISyntaxException, IOException {
        BuildConfig buildConfig = assertGetBuildConfig(forgeClient, projectName);

        BuildConfigAssert.assertThat(buildConfig).metadata().annotations().isNotEmpty();

        return assertAnnotation(buildConfig, Annotations.Builds.GIT_CLONE_URL);
    }

    /**
     * Asserts that the given resource has the given annotation and returns the value
     */
    public static String assertAnnotation(HasMetadata hasMetadata, String annotationKey) {
        HasMetadataAssert.assertThat(hasMetadata).isNotNull().metadata().annotations().isNotEmpty();
        Map<String, String> annotations = getOrCreateAnnotations(hasMetadata);

        String answer = annotations.get(annotationKey);
        assertThat(answer).describedAs("" + hasMetadata + " does not have annotation '" + annotationKey + "' but has annotations: " + annotations).isNotEmpty();
        return answer;
    }

    public static BuildConfig assertGetBuildConfig(ForgeClient forgeClient, String projectName) throws URISyntaxException, IOException {
        OpenShiftClient openShiftClient = forgeClient.getOpenShiftOrJenkinshiftClient();
        String namespace = forgeClient.getNamespace();
        BuildConfig buildConfig = openShiftClient.buildConfigs().inNamespace(namespace).withName(projectName).get();
        assertThat(buildConfig).describedAs("No BuildConfig found in " + namespace + " called " + projectName).isNotNull();
        return buildConfig;
    }

    /**
     * Asserts that we can git clone the given repository
     */
    public static Git assertGitCloneRepo(String cloneUrl, File outputFolder) throws GitAPIException, IOException {
        LOG.info("Cloning git repo: " + cloneUrl + " to folder: " + outputFolder);

        Files.recursiveDelete(outputFolder);
        outputFolder.mkdirs();

        CloneCommand command = Git.cloneRepository();
        command = command.setCloneAllBranches(false).setURI(cloneUrl).setDirectory(outputFolder).setRemote("origin");

        Git git;
        try {
            git = command.call();
        } catch (Exception e) {
            LOG.error("Failed to git clone remote repo " + cloneUrl + " due: " + e.getMessage(), e);
            throw e;
        }
        return git;
    }

    /**
     * Asserts that a Build is created and that it completes successfully within the given time period
     */
    public static Build assertBuildCompletes(ForgeClient forgeClient, String projectName, long timeoutMillis) throws Exception {

        Asserts.assertWaitFor(10 * 60 * 1000, new Block() {
            @Override
            public void invoke() throws Exception {
                JobWithDetails job = assertJob(projectName);
                Build lastBuild = job.getLastBuild();
                assertThat(lastBuild.getNumber()).describedAs("Waiting for latest build for job " + projectName + " to have a valid build number").isGreaterThan(0);
                String buildUrl = lastBuild.getUrl();
                assertThat(buildUrl).describedAs("Waiting for latest build for job " + projectName + " to have a valid URL").isNotEmpty().doesNotContain("UNKNOWN");
            }
        });

        JobWithDetails job = assertJob(projectName);
        Build lastBuild = job.getLastBuild();
        assertThat(lastBuild).describedAs("No Jenkins Build for Job: " + projectName).isNotNull();
        BuildWithDetails details = null;
        String description = "Job " + projectName + " build " + lastBuild.getNumber();

        LOG.info("Waiting for build " + description + " to complete...");

        String logUri = getBuildConsoleTextUrl(lastBuild, description);

        long end = System.currentTimeMillis() + timeoutMillis;
        TailResults tailResults = TailResults.START;
        while (true) {
            int sleepMillis = 5000;
            long start = System.currentTimeMillis();
            tailResults = tailLog(logUri, tailResults, new Function<String, Void>() {
                @Override
                public Void apply(String line) {
                    System.out.println("Build:" + lastBuild.getNumber() + ": " + line);
                    return null;
                }
            });

            details = lastBuild.details();
            if (!details.isBuilding()) {
                break;
            }
            if (end < System.currentTimeMillis()) {
                break;
            }
            long duration = System.currentTimeMillis() - start;
            long sleepTime = sleepMillis - duration;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        details = lastBuild.details();
        dumpBuildLog(lastBuild, description);
        LOG.info("");

        BuildResult result = details.getResult();
        assertThat(result).describedAs("Status of " + description).isEqualTo(BuildResult.SUCCESS);
        assertThat(details.isBuilding()).describedAs("Build not finshed for " + description).isFalse();

        return lastBuild;
    }

    public static JobWithDetails assertJob(String projectName) throws URISyntaxException, IOException {
        JenkinsServer jenkins = createJenkinsServer();
        JobWithDetails job = jenkins.getJob(projectName);
        assertThat(job).describedAs("No Jenkins Job found for name: " + projectName).isNotNull();
        return job;
    }

    public static void dumpBuildLog(Build lastBuild, String description) throws IOException {
        String logUri = getBuildConsoleTextUrl(lastBuild, description);

        URL logURL = new URL(logUri);
        InputStream inputStream = logURL.openStream();

        printBuildLog(inputStream, description);
    }

    public static String getBuildConsoleTextUrl(Build lastBuild, String description) {
        String url = lastBuild.getUrl();

        LOG.info("Build URL: " + url);
        String logUri = url + "/consoleText";
        if (logUri.indexOf("://") < 0) {
            logUri = URLUtils.pathJoin(getJenkinsURL(), logUri);
        }
        LOG.info("Tailing " + description + " at URL:" + logUri);
        return logUri;
    }

    protected static void printBuildLog(InputStream inputStream, String name) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                LOG.info(line);
            }

        } catch (Exception e) {
            LOG.error("Failed to read build log for: " + name + ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    public static void assertValidCommandInput(CommandInputDTO info) {
        assertThat(info).isNotNull();
        assertThat(info.getMetadata()).describedAs("getMetadata()").isNotNull();
        assertThat(info.getInputs()).describedAs("getInputs()").isNotEmpty();
        assertThat(info.getState()).describedAs("getState()").isNotNull();
    }

}
