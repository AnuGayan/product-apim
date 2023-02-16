/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.token;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class TokenResponseWithCustomObjectsTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TokenResponseWithCustomObjectsTestCase.class);
    private ServerConfigurationManager serverConfigurationManager;
    private static final String CUSTOM_GRANT_HANDLER = "CustomGrantHandler-1.0.0.jar";
    public static final String APP_NAME = "Integration_Test_Custom_Grant";
    public static final String TOKEN_SCOPE = "Production";
    public static final String APP_OWNER = "admin";
    public static final String GRANT_TYPE = "mobile";
    private static final String TLS_PROTOCOL = "TLS";
    private String keyManagerURL;

    @BeforeClass(alwaysRun = true)
    public void configureEnvironment() throws Exception {
        super.init();
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        String customHandlerSourcePath = getAMResourceLocation() + File.separator + "lifecycletest"
                + File.separator + CUSTOM_GRANT_HANDLER;
        serverConfigurationManager.copyToComponentLib(new File(customHandlerSourcePath));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation() +
                File.separator + "configFiles" + File.separator + "customGrant" +
                File.separator + "deployment.toml"));
        serverConfigurationManager.restartGracefully();
    }

    @Test(groups = "wso2.am", description = "Check support for custom JSON objects in access-token response")
    public void testTokenResponseWithCustomObjects() {
        JsonObject response = makeDCRRequest();
        String consumerKey = response.getAsJsonPrimitive("clientId").getAsString();
        String consumerSecret = response.getAsJsonPrimitive("clientSecret").getAsString();
        if (consumerKey != null || consumerSecret != null) {
            JsonObject tokenResponse = getAccessToken(consumerKey, consumerSecret);

            JsonElement user1 = tokenResponse.get("User1");
            Assert.assertNotNull(user1, "Custom JSON object \"User1\" cannot be null");

            JsonObject actualResponse = user1.getAsJsonObject();
            String expectedResponseString = "{\"FirstName\":\"John\",\"LastName\":\"Reese\"}";
            JsonParser parser = new JsonParser();
            JsonObject expectedResponse = parser.parse(expectedResponseString).getAsJsonObject();
            Assert.assertEquals(expectedResponse, actualResponse, "Expected Response mismatch");
        }
    }

    private JsonObject getAccessToken(String consumerKey, String consumerSecret) {
        HttpsURLConnection urlConn = null;
        URL url;
        String tokenURL = keyManagerURL.concat("oauth2/token");
        TrustManager trustAll = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override
            public X509Certificate[] getAcceptedIssuers() { return null; }
        };
        //calling token endpoint
        try {
            url = new URL(tokenURL);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String clientEncoded = DatatypeConverter.printBase64Binary(
                    (consumerKey + ':' + consumerSecret).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            String postParams = "grant_type=mobile&mobileNumber=0333444";
            urlConn.setHostnameVerifier(new HostnameVerifier() {
                @Override public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            urlConn.setSSLSocketFactory(sslContext.getSocketFactory());
            urlConn.getOutputStream().write((postParams).getBytes(StandardCharsets.UTF_8));
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                return getJsonResponse(urlConn);
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    private JsonObject makeDCRRequest() {
        URL url;
        HttpURLConnection urlConn = null;

        try {
            //Create json payload for DCR endpoint
            JsonObject json = new JsonObject();
            json.addProperty("clientName", APP_NAME);
            json.addProperty("tokenScope", TOKEN_SCOPE);
            json.addProperty("owner", APP_OWNER);
            json.addProperty("grantType", GRANT_TYPE);
            // Calling DCR endpoint
            keyManagerURL = getKeyManagerURLHttps();
            String dcrEndpoint = keyManagerURL.concat("client-registration/v0.17/register");
            url = new URL(dcrEndpoint);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/json");
            String clientEncoded = DatatypeConverter.printBase64Binary((System.getProperty("systemUsername",
                    "admin") + ':' + System.getProperty("systemUserPwd", "admin"))
                    .getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            urlConn.getOutputStream().write((json.toString()).getBytes(StandardCharsets.UTF_8));
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {  //If the DCR call is success
                return getJsonResponse(urlConn);
            } else { //If DCR call fails
                throw new RuntimeException("DCR call failed. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String errorMsg = "Cannot create OAuth application  : " + APP_NAME;
            throw new RuntimeException(errorMsg, e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    private static JsonObject getJsonResponse(HttpURLConnection urlConn) throws IOException {
        String responseStr = getResponseString(urlConn.getInputStream());
        JsonParser parser = new JsonParser();
        return parser.parse(responseStr).getAsJsonObject();
    }

    private static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String file = "";
            String str;
            while ((str = buffer.readLine()) != null) {
                file += str;
            }
            return file;
        }
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfigs() throws Exception {
        serverConfigurationManager.removeFromComponentLib(CUSTOM_GRANT_HANDLER);
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
