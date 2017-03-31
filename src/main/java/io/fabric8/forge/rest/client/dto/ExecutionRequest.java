/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.rest.client.dto;

import java.util.List;

/**
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class ExecutionRequest {
    private String resource;
    private String projectName;
    private String namespace;
    private List<InputValueDTO> inputs;
    private Integer stepIndex;

    @Override
    public String toString() {
        return "ExecutionRequest{" +
                "resource='" + resource + '\'' +
                ", inputs=" + inputs +
                '}';
    }

    public List<InputValueDTO> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputValueDTO> inputs) {
        this.inputs = inputs;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    /**
     * Returns the wizard step number or 0 if one is not defined
     */
    public int stepIndex() {
        if (stepIndex != null) {
            return stepIndex.intValue();
        } else {
            return 0;
        }
    }
}
