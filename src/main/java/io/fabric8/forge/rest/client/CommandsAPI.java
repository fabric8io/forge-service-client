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


import io.fabric8.forge.rest.client.dto.ExecutionRequest;
import io.fabric8.forge.rest.client.dto.VersionDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 */
@Path("/forge")
public interface CommandsAPI {

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    VersionDTO getInfo();

    @GET
    @Path("/commandNames")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getCommandNames();


    @POST
    @Path("/commands/{name}/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response validateCommand(@PathParam("name") String name, ExecutionRequest executionRequest) throws Exception;


    @GET
    @Path("/commands/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getCommandInput(@PathParam("name") String name) throws Exception;


    @POST
    @Path("/commands/{commandName}/next")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response nextStep(@PathParam("commandName") String name, ExecutionRequest executionRequest) throws Exception;

    @POST
    @Path("/commands/{commandName}/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response executeCommand(@PathParam("commandName") String name, ExecutionRequest executionRequest) throws Exception;

    @POST
    @javax.ws.rs.Path("/commands/{commandName}/execute")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response executeCommand(@PathParam("commandName") String commandName, Form form) throws Exception;


}
