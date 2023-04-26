/*
 * Copyright (c) 2023, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * This test case verifies the business information visibility on developer portal.
 */
public class DevPortalBusinessInformationViewTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(DevPortalBusinessInformationViewTestCase.class);

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public DevPortalBusinessInformationViewTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = "wso2.am", description = "This tests the retrieval of business owner name in devportal API search")
    public void testBusinessOwnerNameInDevPortal() throws Exception {

        String apiName = "testApi";
        String apiContext = "test-context";
        String apiVersion = "1.0.0";
        String apiDescription = "This API is added to test business info view in dev portal";
        String apiProductionEndPointUrl = "http://test.com";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiProductionEndPointUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTiersCollection("Gold,Bronze");
        apiRequest.setTier("Gold");
        apiRequest.setDefault_version_checked("true");

        apiRequest.setBusinessOwner("testBusinessOwner");
        apiRequest.setBusinessOwnerEmail("testbusinessowner@test.com");
        apiRequest.setTechnicalOwner("testTechnicalOwner");
        apiRequest.setTechnicalOwnerEmail("testtechnicalowner@test.com");

        String apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        int retries = 10;

        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStore
                    .searchAPIs("testApi");
            if (searchResultListDTO.getCount() == 1) {
                assertTrue(searchResultListDTO.toString().contains("testBusinessOwner"),
                        "Business owner name is not retrieved for search results");
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Search in dev portal failed. Received response : " + searchResultListDTO
                            .getCount());
                } else {
                    log.warn("Search in dev portal failed. Received response : " + searchResultListDTO
                            .getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        super.cleanUp();
    }
}
