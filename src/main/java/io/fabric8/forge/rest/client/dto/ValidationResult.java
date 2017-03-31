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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ValidationResult {
    private WizardState state;
    private List<PropertyDTO> inputs = new ArrayList<>();
    private List<UIMessageDTO> messages;

/*
    // TODO verify these fields are still valid!
    private boolean valid;
    private boolean canExecute;
    private String out;
    private String err;
    private WizardResultsDTO wizardResults;
*/

    @JsonInclude
    public boolean isValid() {
        return state != null ? state.isValid() : false;
    }

    @JsonInclude
    public boolean isCanMoveToNextStep() {
        return state != null ? state.isCanMoveToNextStep() : false;
    }

    @JsonInclude
    public boolean isCanExecute() {
        return state != null ? state.isCanExecute() : false;
    }

    @JsonInclude
    public boolean isCanMoveToPreviousStep() {
        return state != null ? state.isCanMoveToPreviousStep() : false;
    }

    public WizardState getState() {
        return state;
    }

    public void setState(WizardState state) {
        this.state = state;
    }

    public List<PropertyDTO> getInputs() {
        return inputs;
    }

    public void setInputs(List<PropertyDTO> inputs) {
        this.inputs = inputs;
    }

    public List<UIMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<UIMessageDTO> messages) {
        this.messages = messages;
    }
}
