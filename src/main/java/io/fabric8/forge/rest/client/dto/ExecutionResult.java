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
package io.fabric8.forge.rest.client.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 */
public class ExecutionResult {
    private final int status;
    private final String entity;
    private final JsonNode json;

    public ExecutionResult(int status, String entity, JsonNode json) {
        this.status = status;
        this.entity = entity;
        this.json = json;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "status=" + status +
                ", entity='" + entity + '\'' +
                ", json=" + json +
                '}';
    }

    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }

    public int getStatus() {
        return status;
    }

    public String getEntity() {
        return entity;
    }

    public JsonNode getJson() {
        return json;
    }
}
