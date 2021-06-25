/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.test.impl;

import org.wso2.am.integration.clients.gateway.api.ApiClient;
import org.wso2.am.integration.clients.gateway.api.ApiException;
import org.wso2.am.integration.clients.gateway.api.v1.ReDeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v1.GetApiArtifactsApi;
import org.wso2.am.integration.clients.gateway.api.v1.UndeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.SequencesDTO;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class RestAPIGatewayImpl {
    private String tenantDomain = null;
    GetApiArtifactsApi getApiArtifactsApi = new GetApiArtifactsApi();
    ReDeployApiApi reDeployApiApi = new ReDeployApiApi();
    UndeployApiApi undeployApiApi = new UndeployApiApi();

    public RestAPIGatewayImpl(String username, String password, String tenantDomain) {
        ApiClient apiClient = new ApiClient();
        String basicEncoded =
                DatatypeConverter.printBase64Binary((username + ':' + password).getBytes(StandardCharsets.UTF_8));
        apiClient.addDefaultHeader("Authorization", "Basic " + basicEncoded);
        apiClient.setDebugging(true);
        apiClient.setBasePath("https://localhost:9943/api/am/gateway/v1");
        apiClient.setReadTimeout(600000);
        apiClient.setConnectTimeout(600000);
        apiClient.setWriteTimeout(600000);
        getApiArtifactsApi.setApiClient(apiClient);
        reDeployApiApi.setApiClient(apiClient);
        undeployApiApi.setApiClient(apiClient);
        this.tenantDomain = tenantDomain;
    }

    public APIDTO retrieveAPI(String name, String version) throws ApiException {
        return getApiArtifactsApi.apiArtifactGet(name, version, tenantDomain);
    }

    public EndpointsDTO retrieveEndpoints(String name, String version) throws ApiException {
        return getApiArtifactsApi.endPointsGet(name, version, tenantDomain);
    }

    public LocalEntryDTO retrieveLocalEntries(String name, String version) throws ApiException {
        return getApiArtifactsApi.localEntryGet(name, version, tenantDomain);
    }

    public SequencesDTO retrieveSequences(String name, String version) throws ApiException {
        return getApiArtifactsApi.sequenceGet(name, version, tenantDomain);
    }
}