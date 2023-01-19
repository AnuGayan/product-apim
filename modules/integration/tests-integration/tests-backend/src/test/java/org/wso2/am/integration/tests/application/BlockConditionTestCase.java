/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.application;

import org.apache.commons.httpclient.*;
import org.apache.commons.logging.*;
import org.testng.*;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.impl.*;
import org.wso2.am.integration.test.utils.base.*;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.tests.api.lifecycle.*;
import org.wso2.carbon.automation.test.utils.http.client.*;
import org.wso2.carbon.integration.common.admin.client.*;

import java.net.*;
import java.util.*;

import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.*;

public class BlockConditionTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(BlockConditionTestCase.class);
    private final String[] subscriberRole = {APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER};
    private final String[] creatorPublisherRole = {APIMIntegrationConstants.APIM_INTERNAL_ROLE.CREATOR,
            APIMIntegrationConstants.APIM_INTERNAL_ROLE.PUBLISHER};
    private static final String USER_JOHN = "john-smith";
    private static final String USER_JOHN_PWD = "John1@";
    private static final String TENANT_ADMIN = "admin";
    private static final String TENANT_ADMIN_PWD = "admin1@";
    private static final String TENANT_USER1 = "mary";
    private static final String TENANT_USER2 = "smith-silva";
    private static final String TENANT_USER1_PWD = "user1@";
    private static final String TENANT_DOMAIN = "tenant.com";
    private RestAPIStoreImpl restAPIStoreClient1, restAPIStoreClientTenant;
    private RestAPIPublisherImpl restAPIPublisherTenant;
    private static final String TENANT_ADMIN_WITH_DOMAIN = TENANT_ADMIN + "@" + TENANT_DOMAIN;
    private static final String JOHN_APP = "johnApp";
    protected static final String TIER_UNLIMITED = "Unlimited";
    private String appIdOfJohnApp, appIdOfSmithApp;
    private final String API_NAME = "CopyAPIWithOutReSubscriptionTest";
    private final String API_CONTEXT = "CopyAPIWithOutReSubscription";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String apiId;
    private String apiId2;
    private APIRequest apiRequest;
    private String subscriptionId1, subscriptionId2;
    public static final String BLOCKED = "BLOCKED";
    public static final String PROD_ONLY_BLOCKED = "PROD_ONLY_BLOCKED";


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        // add user john-smith as subscribers
        userManagementClient.addUser(USER_JOHN, USER_JOHN_PWD, subscriberRole, USER_JOHN);

        // add a tenant domain
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, TENANT_ADMIN_PWD, TENANT_ADMIN, "demo");
        UserManagementClient userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT_ADMIN_WITH_DOMAIN, TENANT_ADMIN_PWD);
        // add users within the tenant domain
        userManagementClient1.addUser(TENANT_USER1, TENANT_USER1_PWD, creatorPublisherRole, TENANT_USER1);
        userManagementClient1.addUser(TENANT_USER2, TENANT_USER1_PWD, subscriberRole, TENANT_USER1);

        // create application in the store using super tenant user john-smith's credentials
        restAPIStoreClient1 = new RestAPIStoreImpl(USER_JOHN, USER_JOHN_PWD, SUPER_TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfJohnDTO = restAPIStoreClient1.addApplication(JOHN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user John");
        appIdOfJohnApp = appOfJohnDTO.getApplicationId();

        waitForKeyManagerDeployment(TENANT_DOMAIN, "Default");

        // create application for tenant
        restAPIStoreClientTenant = new RestAPIStoreImpl(TENANT_USER2, TENANT_USER1_PWD, TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfSmithDTO = restAPIStoreClientTenant.addApplication(JOHN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user Smith");
        appIdOfSmithApp = appOfSmithDTO.getApplicationId();

        // create api for super tenant
        ArrayList<String> grantTypes = new ArrayList<>();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        //create api for tenant
        restAPIPublisherTenant = new RestAPIPublisherImpl(TENANT_USER1, TENANT_USER1_PWD, TENANT_DOMAIN,
                publisherURLHttps);
        apiId2 = createAndPublishAPIUsingRest(apiRequest, restAPIPublisherTenant, false);

        //subscribe API for super tenant
        subscriptionId1 = restAPIStore.subscribeToAPI(apiId, appIdOfJohnApp, TIER_UNLIMITED).getSubscriptionId();
        //subscribe api for tenant
        subscriptionId2 = restAPIStoreClientTenant.subscribeToAPI(apiId2, appIdOfSmithApp, TIER_UNLIMITED)
                .getSubscriptionId();

    }

    @Test(groups = {"wso2.am"}, description = "Test block subscription")
    public void testBlockSubscriptionPI() throws Exception {
        // generate token for super tenant  production and invoke
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStoreClient1.generateKeys(appIdOfJohnApp, "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessTokenSuperTenantProduction = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessTokenSuperTenantProduction);

        HttpResponse serviceResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK);

        // generate token for super tenant  sandbox and invoke
        ApplicationKeyDTO applicationKeyDTO2 = restAPIStoreClient1.getApplicationKeyByKeyMappingId();.generateKeys(appIdOfJohnApp, "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String accessTokenSuperTenantSandbox = applicationKeyDTO2.getToken().getAccessToken();
        Map<String, String> requestHeaders2 = new HashMap<String, String>();
        requestHeaders2.put("accept", "text/xml");
        requestHeaders2.put("Authorization", "Bearer " + accessTokenSuperTenantSandbox);
        HttpResponse serviceResponse2 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(serviceResponse2.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(serviceResponse2.getData(), API_RESPONSE_DATA);

        // block subscription and test for production only for super tenant
        restAPIPublisher.blockSubscription(subscriptionId1, PROD_ONLY_BLOCKED);
        HttpResponse serviceResponse3 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(serviceResponse3.getResponseCode(), 900907);

        HttpResponse serviceResponse4 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(serviceResponse4.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(serviceResponse4.getData(), API_RESPONSE_DATA);

        // block subscription and test for production and sandbox for super tenant
        restAPIPublisher.blockSubscription(subscriptionId1, BLOCKED);
        HttpResponse serviceResponse5 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(serviceResponse5.getResponseCode(), 900907);

        HttpResponse serviceResponse6 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(serviceResponse6.getResponseCode(), 900907);


        // generate token for tenant production and invoke
        ArrayList<String> grantTypesTenant = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTOTenant = restAPIStoreClientTenant.generateKeys(appIdOfJohnApp, "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypesTenant);
        String accessTokenTenantProduction = applicationKeyDTOTenant.getToken().getAccessToken();
        Map<String, String> requestHeadersTenantProduction = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessTokenTenantProduction);

        HttpResponse serviceResponse7 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeadersTenantProduction);
        Assert.assertEquals(serviceResponse7.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(serviceResponse7.getData(), API_RESPONSE_DATA);

        // generate token for tenant sandbox and invoke
        ApplicationKeyDTO applicationKeyDTOTenant2 = restAPIStore.generateKeys(appIdOfJohnApp, "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypesTenant);
        String accessTokenTenantSandbox = applicationKeyDTOTenant2.getToken().getAccessToken();
        Map<String, String> requestHeadersTenantSandbox = new HashMap<String, String>();
        requestHeaders2.put("accept", "text/xml");
        requestHeaders2.put("Authorization", "Bearer " + accessTokenTenantSandbox);
        HttpResponse serviceResponse8 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(serviceResponse8.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(serviceResponse8.getData(), API_RESPONSE_DATA);

        // block subscription and test for production only for tenant
        restAPIPublisherTenant.blockSubscription(subscriptionId2, PROD_ONLY_BLOCKED);
        HttpResponse serviceResponse9 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeadersTenantProduction);
        Assert.assertEquals(serviceResponse9.getResponseCode(), 900907);

        HttpResponse serviceResponse10 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeadersTenantSandbox);
        Assert.assertEquals(serviceResponse10.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(serviceResponse10.getData(), API_RESPONSE_DATA);

        // block subscription and test for production and sandbox  for tenant
        restAPIPublisherTenant.blockSubscription(subscriptionId2, BLOCKED);
        HttpResponse serviceResponse11 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeadersTenantProduction);
        Assert.assertEquals(serviceResponse11.getResponseCode(), 900907);

        HttpResponse serviceResponse12 =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeadersTenantSandbox);
        Assert.assertEquals(serviceResponse12.getResponseCode(), 900907);
    }
}
