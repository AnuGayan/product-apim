/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.BandwidthLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomAttributeDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SubscriptionThrottlingPolicyTestCase extends APIMIntegrationBaseTest {

    private String displayName = "Test Policy";
    private String description = "This is a test subscription throttle policy";
    private String timeUnit = "min";
    private Integer unitTime = 1;
    private Integer graphQLMaxComplexity = 400;
    private Integer graphQLMaxDepth = 10;
    private Integer rateLimitCount = -1;
    private String rateLimitTimeUnit = "NA";
    private boolean stopQuotaOnReach = false;
    private String billingPlan = "COMMERCIAL";
    private Integer subscriberCount = 0;
    private List<CustomAttributeDTO> customAttributes = null;
    private SubscriptionThrottlePolicyDTO requestCountPolicyDTO;
    private SubscriptionThrottlePolicyDTO bandwidthPolicyDTO;
    private AdminApiTestHelper adminApiTestHelper;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_NAME = "TestAPI";
    private final String API_CONTEXT = "testapi";
    private final String API_VERSION = "1.0.0";
    private String apiId;
    private String appId;
    private String providerName;
    private ArrayList grantTypes;

    @Factory(dataProvider = "userModeDataProvider")
    public SubscriptionThrottlingPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
        customAttributes = new ArrayList<>();
        CustomAttributeDTO attribute = new CustomAttributeDTO();
        attribute.setName("testAttribute");
        attribute.setValue("testValue");
        customAttributes.add(attribute);
        providerName = user.getUserName();
        grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
    }

    @Test(groups = {"wso2.am"}, description = "Test add subscription throttling policy with request count limit")
    public void testAddPolicyWithRequestCountLimit() throws Exception {

        //Create the subscription throttling policy DTO with request count limit
        String policyName = "TestPolicyOne";
        Long requestCount = 50L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount, rateLimitTimeUnit, customAttributes,
                        stopQuotaOnReach, billingPlan, subscriberCount);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created subscription throttling policy DTO
        adminApiTestHelper.verifySubscriptionThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add subscription throttling policy with bandwidth limit",
            dependsOnMethods = "testAddPolicyWithRequestCountLimit")
    public void testAddPolicyWithBandwidthLimit() throws Exception {

        //Create the subscription throttling policy DTO with bandwidth limit
        String policyName = "TestPolicyTwo";
        Long dataAmount = 2L;
        String dataUnit = "KB";
        BandwidthLimitDTO bandwidthLimit = DtoFactory.createBandwidthLimitDTO(timeUnit, unitTime, dataAmount, dataUnit);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT, null, bandwidthLimit);
        bandwidthPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount, rateLimitTimeUnit, customAttributes,
                        stopQuotaOnReach, billingPlan, subscriberCount);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(bandwidthPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        bandwidthPolicyDTO.setPolicyId(policyId);
        bandwidthPolicyDTO.setIsDeployed(true);
        //Verify the created subscription throttling policy DTO
        adminApiTestHelper.verifySubscriptionThrottlePolicyDTO(bandwidthPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test check whether subscription throttling policy works",
            dependsOnMethods = "testAddPolicyWithBandwidthLimit")
    public void testSubscriptionLevelThrottling() throws Exception {

        // Add the subscription throttle policy (10per1hour)
        String policyName1 = "SubscriptionThrottlePolicy10per1hour";
        RequestCountLimitDTO requestCountLimit1 = DtoFactory.createRequestCountLimitDTO("hours", 1, 10L);
        ThrottleLimitDTO defaultLimit1 = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit1, null);
        SubscriptionThrottlePolicyDTO sampleSubscriptionThrottlePolicyDTO1 =
                DtoFactory.createSubscriptionThrottlePolicyDTO(policyName1, policyName1, description, false,
                        defaultLimit1, graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount, rateLimitTimeUnit,
                        customAttributes, true, billingPlan, subscriberCount);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy1 =
                restAPIAdmin.addSubscriptionThrottlingPolicy(sampleSubscriptionThrottlePolicyDTO1);
        Assert.assertEquals(addedPolicy1.getStatusCode(), HttpStatus.SC_CREATED);

        // Add the subscription throttle policy (20per1hour)
        String policyName2 = "SubscriptionThrottlePolicy20per1hour";
        RequestCountLimitDTO requestCountLimit2 = DtoFactory.createRequestCountLimitDTO("hours", 1, 20L);
        ThrottleLimitDTO defaultLimit2 = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit2, null);
        SubscriptionThrottlePolicyDTO sampleSubscriptionThrottlePolicyDTO2 =
                DtoFactory.createSubscriptionThrottlePolicyDTO(policyName2, policyName2, description, false,
                        defaultLimit2, graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount, rateLimitTimeUnit,
                        customAttributes, true, billingPlan, subscriberCount);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy2 =
                restAPIAdmin.addSubscriptionThrottlingPolicy(sampleSubscriptionThrottlePolicyDTO2);
        Assert.assertEquals(addedPolicy2.getStatusCode(), HttpStatus.SC_CREATED);

        // Create and publish an API
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(backEndServerUrl.getWebAppURLHttps()
                + API_END_POINT_POSTFIX_URL));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(policyName1 + "," + policyName2 + "," + APIMIntegrationConstants.API_TIER.BRONZE
                + "," + "TestPolicyOne");
        apiRequest.setProvider(providerName);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        apiId = apiResponse.getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeployment();

        // Create an application
        ApplicationDTO applicationDTO = restAPIStore.addApplication("TestApp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "TestApp Description");
        appId = applicationDTO.getApplicationId();

        // Subscribe to throttling policy and generate access token
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId, policyName1);
        Assert.assertNotNull(subscriptionDTO);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Invoke API until throttling out
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        HttpResponse response1;
        boolean isThrottled1 = false;
        int totalNumberOfRequests = 15;
        ThrottlingUtils.waitUntilNextClockHourIfCurrentHourIsInLastNMinutes(3);
        for (int i = 0; i < 15; i++) {
            if (i == totalNumberOfRequests - 1) {
                Thread.sleep(ThrottlingUtils.WAIT_FOR_JMS_THROTTLE_EVENT_IN_MILLISECONDS);
            }
            response1 = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + API_END_POINT_METHOD,
                    requestHeaders);
            if (response1.getResponseCode() == 429) {
                Assert.assertTrue(i >= 10);
                isThrottled1 = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled1, "Request not throttled by " + policyName1);
        HttpResponse removeSubscriptionResponse = restAPIStore.removeSubscription(subscriptionDTO);
        Assert.assertEquals(removeSubscriptionResponse.getResponseCode(), 200, " Subscription not removed");
        // Subscribe to throttling policy and generate access token
        subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId, policyName2);
        Assert.assertNotNull(subscriptionDTO);
        // Invoke API until throttling out
        HttpResponse response2;
        boolean isThrottled2 = false;
        for (int i = 0; i < 25; i++) {
            response2 = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + API_END_POINT_METHOD,
                    requestHeaders);
            if (response2.getResponseCode() == 429) {
                Assert.assertTrue(i >= 20);
                isThrottled2 = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled2, "Request not throttled by " + policyName2);
        restAPIStore.removeSubscription(subscriptionDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test get and update subscription throttling policy",
            dependsOnMethods = "testSubscriptionLevelThrottling")
    public void testGetAndUpdatePolicy() throws Exception {

        //Get the added subscription throttling policy with request count limit
        String policyId = requestCountPolicyDTO.getPolicyId();
        ApiResponse<SubscriptionThrottlePolicyDTO> retrievedPolicy =
                restAPIAdmin.getSubscriptionThrottlingPolicy(policyId);
        SubscriptionThrottlePolicyDTO retrievedPolicyDTO = retrievedPolicy.getData();
        Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the retrieved subscription throttling policy DTO
        adminApiTestHelper.verifySubscriptionThrottlePolicyDTO(requestCountPolicyDTO, retrievedPolicyDTO);

        //Update the subscription throttling policy
        String updatedDescription = "This is a updated test subscription throttle policy";
        requestCountPolicyDTO.setDescription(updatedDescription);
        ApiResponse<SubscriptionThrottlePolicyDTO> updatedPolicy =
                restAPIAdmin.updateSubscriptionThrottlingPolicy(policyId, requestCountPolicyDTO);
        SubscriptionThrottlePolicyDTO updatedPolicyDTO = updatedPolicy.getData();
        Assert.assertEquals(updatedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated subscription throttling policy DTO
        adminApiTestHelper.verifySubscriptionThrottlePolicyDTO(requestCountPolicyDTO, updatedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete subscription throttling policy",
            dependsOnMethods = "testGetAndUpdatePolicy")
    public void testDeletePolicy() throws Exception {

        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteSubscriptionThrottlingPolicy(requestCountPolicyDTO.getPolicyId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test add subscription throttling policy with existing policy name",
            dependsOnMethods = "testDeletePolicy")
    public void testAddPolicyWithExistingPolicyName() {

        //Exception occurs when adding an subscription throttling policy with an existing policy name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addSubscriptionThrottlingPolicy(bandwidthPolicyDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete subscription throttling policy with non existing policy ID",
            dependsOnMethods = "testAddPolicyWithExistingPolicyName")
    public void testDeletePolicyWithNonExistingPolicyId() {

        //Exception occurs when deleting an subscription throttling policy with a non existing policy ID. The
        //status code in the Exception object is used to assert this scenario
        try {
            //The policy ID is created by combining two generated UUIDs
            restAPIAdmin
                    .deleteSubscriptionThrottlingPolicy(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(bandwidthPolicyDTO.getPolicyId());
        restAPIStore.deleteApplication(appId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
