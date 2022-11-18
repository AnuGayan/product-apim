/*
 *Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.server.restart;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class APIRevisionServerRestartTestCase extends APIManagerLifecycleBaseTest {

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    private String apiRevisionApiId;
    private String revisionUUID;

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws Exception {
        super.init();
        apiRevisionApiId = (String) ctx.getAttribute("apiRevisionApiId");

    }

    @Test(groups = {"wso2.am"}, description = "API Revision create test case")
    public void testAddingAPIRevision() throws Exception {
        // Create the API Revision creation request object
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiRevisionApiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        // Add the API Revision using the API Publisher
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        Assert.assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        revisionUUID = revisionResponseData.getString("id");
    }
    @Test(groups = {"wso2.am"}, description = "Check the availability of API Revision in publisher before deploying.",
            dependsOnMethods = "testAddingAPIRevision")
    public void testGetAPIRevisions() throws Exception {
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiRevisionApiId,null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            revisionUUID = revision.getString("id");
        }
        assertNotNull(revisionUUID, "Unable to retrieve revision UUID");
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments",
            dependsOnMethods = "testGetAPIRevisions")
    public void testDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiRevisionApiId, revisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" +apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision to gateway environments",
            dependsOnMethods = "testDeployAPIRevisions")
    public void testUnDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiRevisionApiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());

    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API using created API Revision",
            dependsOnMethods = "testUnDeployAPIRevisions")
    public void testRestoreAPIRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiRevisionApiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to resotre API Revisions:" + apiRevisionsRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API using created API Revision",
            dependsOnMethods = "testRestoreAPIRevision")
    public void testDeleteAPIRevision() throws Exception {
        HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIRevision(apiRevisionApiId, revisionUUID);
        assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Revisions:" + apiRevisionsDeleteResponse.getData());
    }

}
