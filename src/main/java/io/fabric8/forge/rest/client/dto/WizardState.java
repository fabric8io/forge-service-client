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

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WizardState {
    private boolean valid;
    private boolean canMoveToPreviousStep;
    private boolean canMoveToNextStep;
    private boolean canExecute;
    private boolean wizard;
    private List<String> steps;


    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isCanMoveToPreviousStep() {
        return canMoveToPreviousStep;
    }

    public void setCanMoveToPreviousStep(boolean canMoveToPreviousStep) {
        this.canMoveToPreviousStep = canMoveToPreviousStep;
    }

    public boolean isCanMoveToNextStep() {
        return canMoveToNextStep;
    }

    public void setCanMoveToNextStep(boolean canMoveToNextStep) {
        this.canMoveToNextStep = canMoveToNextStep;
    }

    public boolean isCanExecute() {
        return canExecute;
    }

    public void setCanExecute(boolean canExecute) {
        this.canExecute = canExecute;
    }

    public boolean isWizard() {
        return wizard;
    }

    public void setWizard(boolean wizard) {
        this.wizard = wizard;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
}
