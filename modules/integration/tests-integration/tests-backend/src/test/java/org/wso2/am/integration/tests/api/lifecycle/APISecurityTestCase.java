/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import com.google.gson.Gson;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This class tests the behaviour of API when there is choice of selection between oauth2 and mutual ssl in API Manager.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APISecurityTestCase extends APIManagerLifecycleBaseTest {

    private final String mutualSSLOnlyAPIName = "mutualsslOnlyAPI";
    private final String mutualSSLWithOAuthAPI = "mutualSSLWithOAuthAPI";
    private final String mutualSSLandOauthMandatoryAPI = "mutualSSLandOAuthMandatoryAPI";
    private final String OauthDisabledAPI = "OauthDisabledAPI";
    private final String OauthEnabledAPI = "OauthEnabledAPI";
    private final String mutualSSLOnlyAPIContext = "mutualsslOnlyAPI";
    private final String mutualSSLWithOAuthAPIContext = "mutualSSLWithOAuthAPI";
    private final String mutualSSLandOAuthMandatoryAPIContext = "mutualSSLandOAuthMandatoryAPI";
    private final String OauthDisabledAPIContext = "OauthDisabledAPI";
    private final String OauthEnabledAPIContext = "OauthEnabledAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String accessToken;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationId;
    private String apiId1, apiId2;
    private String apiId3;
    private String apiId4;
    private String apiId5;

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, IOException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException,
            InterruptedException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(mutualSSLOnlyAPIName, mutualSSLOnlyAPIContext, new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTags(API_TAGS);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest1.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("mutualssl");
        securitySchemes.add("mutualssl_mandatory");
        apiRequest1.setSecurityScheme(securitySchemes);
        apiRequest1.setDefault_version("true");
        apiRequest1.setHttps_checked("https");
        apiRequest1.setHttp_checked(null);
        apiRequest1.setDefault_version_checked("true");
        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certOne), "example", apiId1, APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIRequest apiRequest2 = new APIRequest(mutualSSLWithOAuthAPI, mutualSSLWithOAuthAPIContext, new URL(apiEndPointUrl));
        apiRequest2.setVersion(API_VERSION_1_0_0);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTags(API_TAGS);
        apiRequest2.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest2.setOperationsDTOS(operationsDTOS);
        apiRequest2.setDefault_version("true");
        apiRequest2.setHttps_checked("https");
        apiRequest2.setHttp_checked(null);
        apiRequest2.setDefault_version_checked("true");
        List<String> securitySchemes2 = new ArrayList<>();
        securitySchemes2.add("mutualssl");
        securitySchemes2.add("oauth2");
        securitySchemes2.add("api_key");
        securitySchemes2.add("oauth_basic_auth_api_key_mandatory");
        apiRequest2.setSecurityScheme(securitySchemes2);

        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = response2.getData();

        String certTwo = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certTwo), "abcde", apiId2, APIMIntegrationConstants.API_TIER.UNLIMITED);

        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());

        APIRequest apiRequest3 = new APIRequest(mutualSSLandOauthMandatoryAPI, mutualSSLandOAuthMandatoryAPIContext,
                new URL(apiEndPointUrl));
        apiRequest3.setVersion(API_VERSION_1_0_0);
        apiRequest3.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTags(API_TAGS);
        apiRequest3.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest3.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes3 = new ArrayList<>();
        securitySchemes3.add("mutualssl");
        securitySchemes3.add("oauth2");
        securitySchemes3.add("api_key");
        securitySchemes3.add("mutualssl_mandatory");
        securitySchemes3.add("oauth_basic_auth_api_key_mandatory");
        apiRequest3.setSecurityScheme(securitySchemes3);
        apiRequest3.setDefault_version("true");
        apiRequest3.setHttps_checked("https");
        apiRequest3.setHttp_checked(null);
        apiRequest3.setDefault_version_checked("true");
        HttpResponse response3 = restAPIPublisher.addAPI(apiRequest3);
        apiId3 = response3.getData();
        String certThree = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher
                .uploadCertificate(new File(certThree), "abcdef", apiId3, APIMIntegrationConstants.API_TIER.UNLIMITED);
        restAPIPublisher.changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction());

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId3, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        restAPIStore.subscribeToAPI(apiId2, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        Thread.sleep(120000);

        APIRequest apiRequest4 = new APIRequest(OauthDisabledAPI, OauthDisabledAPIContext,
                new URL(apiEndPointUrl));

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("GET");
        apiOperationsDTO2.setTarget("/customers/{id}");
        apiOperationsDTO2.setAuthType("None");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");
        APIOperationsDTO apiOperationsDTO3 = new APIOperationsDTO();
        apiOperationsDTO3.setVerb("POST");
        apiOperationsDTO3.setTarget("/customers/{id}");
        apiOperationsDTO3.setAuthType("None");
        apiOperationsDTO3.setThrottlingPolicy("Unlimited");
        List<APIOperationsDTO> operationsDTOS2 = new ArrayList<>();
        operationsDTOS2.add(apiOperationsDTO2);
        operationsDTOS2.add(apiOperationsDTO3);

        apiRequest4.setVersion(API_VERSION_1_0_0);
        apiRequest4.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest4.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest4.setTags(API_TAGS);
        apiRequest4.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest4.setOperationsDTOS(operationsDTOS2);
        List<String> securitySchemes4 = new ArrayList<>();
        securitySchemes4.add("oauth2");
        apiRequest4.setSecurityScheme(securitySchemes4);
        apiRequest4.setDefault_version("true");
        apiRequest4.setHttps_checked("https");
        apiRequest4.setHttp_checked(null);
        apiRequest4.setDefault_version_checked("true");

        HttpResponse response4 = restAPIPublisher.addAPI(apiRequest4);
        apiId4 = response4.getData();

        APIRequest apiRequest5 = new APIRequest(OauthEnabledAPI, OauthEnabledAPIContext,
                new URL(apiEndPointUrl));

        apiRequest5.setVersion(API_VERSION_1_0_0);
        apiRequest5.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest5.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest5.setTags(API_TAGS);
        apiRequest5.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        apiRequest5.setOperationsDTOS(operationsDTOS);
        apiRequest5.setSecurityScheme(securitySchemes4);
        apiRequest5.setDefault_version("true");
        apiRequest5.setHttps_checked("https");
        apiRequest5.setHttp_checked(null);
        apiRequest5.setDefault_version_checked("true");

        HttpResponse response5 = restAPIPublisher.addAPI(apiRequest5);
        apiId5 = response5.getData();

        restAPIPublisher.changeAPILifeCycleStatus(apiId4, APILifeCycleAction.PUBLISH.getAction());
    }

    @Test(description = "This test case tests the behaviour of APIs that are protected with mutual SSL and OAuth2 "
            + "when the client certificate is not presented but OAuth2 token is presented.")
    public void testCreateAndPublishAPIWithOAuth2() throws XPathExpressionException, IOException, JSONException {
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        JSONObject response = new JSONObject(apiResponse.getData());
        //fix test failure due to error code changes introduced in product-apim pull #7106
        assertEquals(response.getJSONObject("fault").getInt("code"), 900901,
                "API invocation succeeded with the access token without need for mutual ssl");
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Testing the invocation with API Keys", dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeys() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore
                    .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                            -1, null, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK);

    }

    @Test(description = "Invoke mutual SSL only API with not supported certificate", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLOnlyAPINegative()
            throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatory() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "Test with no application security header with valid cert", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative1() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
    }

    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative2() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual authentication success for oauth mandatory scenario");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual authentication success for oauth mandatory scenario");
    }


    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative3() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + UUID.randomUUID().toString());
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatory() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatoryNeagative1() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatoryNegative2() throws IOException, XPathExpressionException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) +
                                API_END_POINT_METHOD,
                        requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLHeader() throws IOException, XPathExpressionException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("X-WSO2-CLIENT-CERTIFICATE", generateBase64EncodedCertificate());
        HttpResponse response = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) +
                                API_END_POINT_METHOD,
                        requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "Testing the invocation with API Keys having IP restriction",
            dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeysWithIPCondition() throws Exception {
        String permittedIP = "152.23.5.6, 192.168.1.2/24, 2001:c00::/23";
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, permittedIP, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");

        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", "text/xml");
        requestHeaders1.put("apikey", apiKeyDTO.getApikey());
        requestHeaders1.put("X-Forwarded-For", "152.23.5.6"); // a permitted ipv4 address
        HttpResponse response1 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_OK);

        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", "text/xml");
        requestHeaders2.put("apikey", apiKeyDTO.getApikey());
        requestHeaders2.put("X-Forwarded-For", "192.168.1.6"); // a permitted ipv4 address
        HttpResponse response2 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_OK);

        Map<String, String> requestHeaders3 = new HashMap<>();
        requestHeaders3.put("accept", "text/xml");
        requestHeaders3.put("apikey", apiKeyDTO.getApikey());
        requestHeaders3.put("X-Forwarded-For", "192.168.5.6"); // a forbidden ipv4 address
        HttpResponse response3 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders3);
        Assert.assertEquals(response3.getResponseCode(), HttpStatus.SC_FORBIDDEN);

        Map<String, String> requestHeaders4 = new HashMap<>();
        requestHeaders4.put("accept", "text/xml");
        requestHeaders4.put("apikey", apiKeyDTO.getApikey());
        requestHeaders4.put("X-Forwarded-For", "2001:c00:0:0:0:0:c:4"); // a permitted ipv6 address
        HttpResponse response4 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders4);
        Assert.assertEquals(response4.getResponseCode(), HttpStatus.SC_OK);

        Map<String, String> requestHeaders5 = new HashMap<>();
        requestHeaders5.put("accept", "text/xml");
        requestHeaders5.put("apikey", apiKeyDTO.getApikey());
        requestHeaders5.put("X-Forwarded-For", "2061:c00:0:0:0:0:0:0"); // a forbidden ipv6 address
        HttpResponse response5 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders5);
        Assert.assertEquals(response5.getResponseCode(), HttpStatus.SC_FORBIDDEN);
    }

    @Test(description = "Testing the invocation with API Keys having Http Referer restriction",
            dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeysWithRefererCondition() throws Exception {
        String permittedReferer = "www.abc.com/path, sub.cds.com/*, *.gef.com/*";
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, permittedReferer);

        assertNotNull(apiKeyDTO, "API Key generation failed");

        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", "text/xml");
        requestHeaders1.put("apikey", apiKeyDTO.getApikey());
        // matches against a permitted referer which matches an exact referer path
        requestHeaders1.put("Referer", "www.abc.com/path");
        HttpResponse response1 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_OK);

        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", "text/xml");
        requestHeaders2.put("apikey", apiKeyDTO.getApikey());
        // does not match against any permitted referer
        requestHeaders2.put("Referer", "www.abc.com/path2");
        HttpResponse response2 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_FORBIDDEN);


        Map<String, String> requestHeaders3 = new HashMap<>();
        requestHeaders3.put("accept", "text/xml");
        requestHeaders3.put("apikey", apiKeyDTO.getApikey());
        // matches against permitted referer which matches urls of a specific sub domain using a wild card
        requestHeaders3.put("Referer", "sub.cds.com/path1/path2");
        HttpResponse response3 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders3);
        Assert.assertEquals(response3.getResponseCode(), HttpStatus.SC_OK);

        Map<String, String> requestHeaders4 = new HashMap<>();
        requestHeaders4.put("accept", "text/xml");
        requestHeaders4.put("apikey", apiKeyDTO.getApikey());
        // matches against permitted referer which matches urls of a specific sub domain of any domain
        // using wild cards
        requestHeaders4.put("Referer", "example.gef.com/path1");
        HttpResponse response4 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders4);
        Assert.assertEquals(response4.getResponseCode(), HttpStatus.SC_OK);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithRevokedApiKeys() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum
                .PRODUCTION.toString(), -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");

        restAPIStore.revokeAPIKey(applicationId, apiKeyDTO.getApikey());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", apiKeyDTO.getApikey());

        boolean isApiKeyValid = true;
        HttpResponse invocationResponseAfterRevoked;
        int counter = 1;
        do {
            // Wait while the JMS message is received to the related JMS topic
            Thread.sleep(1000L);
            invocationResponseAfterRevoked = HttpRequestUtil.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                    API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeader);
            int responseCodeAfterRevoked = invocationResponseAfterRevoked.getResponseCode();

            if (responseCodeAfterRevoked == HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                isApiKeyValid = false;
            } else if (responseCodeAfterRevoked == HTTP_RESPONSE_CODE_OK) {
                isApiKeyValid = true;
            } else {
                throw new APIManagerIntegrationTestException("Unexpected response received when invoking the API. " +
                        "Response received :" + invocationResponseAfterRevoked.getData() + ":" +
                        invocationResponseAfterRevoked.getResponseMessage());
            }
            counter++;
        } while (isApiKeyValid && counter < 20);
        Assert.assertFalse(isApiKeyValid, "API Key revocation failed. " +
                "API invocation response code is expected to be : " + HTTP_RESPONSE_CODE_UNAUTHORIZED +
                ", but got " + invocationResponseAfterRevoked.getResponseCode());
    }

    @Test(description = "Validating the security of API resources")
    public void testValidateSecurityOfResources() throws Exception {

        // Validate for security disabled API
        HttpResponse response = restAPIPublisher.getAPI(apiId4);
        String retrievedSwagger;

        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        List<APIOperationsDTO> operationsList = apidto.getOperations();
        // Validate the security of resources in API object
        for (APIOperationsDTO apiOperation : operationsList) {
            Assert.assertEquals(apiOperation.getAuthType(), "None", "Incorrect auth type");
        }

        // Verify the security of API in Swagger
        retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId4);
        List<Object> authTypes = validateResourceSecurity(retrievedSwagger);
        for (Object authType : authTypes) {
            Assert.assertEquals(authType, "None", "Incorrect auth type");
        }

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(OauthDisabledAPIContext, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HttpStatus.SC_OK);

        // Validate for security enabled API
        HttpResponse response2 = restAPIPublisher.getAPI(apiId5);
        apidto = new Gson().fromJson(response2.getData(), APIDTO.class);
        operationsList = apidto.getOperations();
        for (APIOperationsDTO apiOperation : operationsList) {
            Assert.assertEquals(apiOperation.getAuthType(), "Application & Application User", "Incorrect auth type");
        }

        retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId5);
        authTypes = validateResourceSecurity(retrievedSwagger);
        for (Object authType : authTypes) {
            Assert.assertEquals(authType, "Application & Application User", "Incorrect auth type");
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws IOException, AutomationUtilException, ApiException {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
        restAPIPublisher.deleteAPI(apiId4);
        restAPIPublisher.deleteAPI(apiId5);
    }

    public String generateBase64EncodedCertificate() throws IOException {

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        String base64EncodedString = IOUtils.toString(new FileInputStream(certOne));
        base64EncodedString = Base64.encodeBase64URLSafeString(base64EncodedString.getBytes());
        return base64EncodedString;
    }

    private List<Object> validateResourceSecurity(String swaggerContent) throws APIManagementException {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(swaggerContent, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Paths paths = openAPI.getPaths();
        List<Object> authType = new ArrayList<>();
        for (String pathKey : paths.keySet()) {
            Map<PathItem.HttpMethod, Operation> operationsMap = paths.get(pathKey).readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : operationsMap.entrySet()) {
                Operation operation = entry.getValue();
                Map<String, Object> extensions = operation.getExtensions();
                Assert.assertNotNull(extensions.get("x-auth-type"));
                authType.add(extensions.get("x-auth-type"));
            }
        }
        return authType;
    }
}
