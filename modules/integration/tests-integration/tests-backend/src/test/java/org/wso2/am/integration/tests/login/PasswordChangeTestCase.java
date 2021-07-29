/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class PasswordChangeTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(PasswordChangeTestCase.class);
    private final String USER_NAME = "user1";
    private final String CURRENT_USER_PASSWORD = "password123";
    private final String NEW_USER_PASSWORD = "123password";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private RestAPIStoreImpl restAPIStoreClient;

    private static final String TENANT_ADMIN = "admin";
    private static final String TENANT_ADMIN_PWD = "test1";
    private static final String TENANT_DOMAIN = "tenant1.com";
    String appId;

    @Factory(dataProvider = "userModeDataProvider")
    public PasswordChangeTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        userManagementClient.addUser(USER_NAME, CURRENT_USER_PASSWORD,
                new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, TENANT_ADMIN_PWD, TENANT_ADMIN, "demo");
    }

    @Test(groups = {"wso2.am"}, description = "Change devportal user password")
    public void testChangeSubscriberUserPassword() throws Exception {
        String tenantDomain = storeContext.getContextTenant().getDomain();
        restAPIStore = new RestAPIStoreImpl(USER_NAME, CURRENT_USER_PASSWORD, tenantDomain, "https://localhost:9943/");
        //change password
        HttpResponse changePasswordResponse = restAPIStore.changePassword(CURRENT_USER_PASSWORD, NEW_USER_PASSWORD);
        assertEquals(changePasswordResponse.getResponseCode(), 200, "Error in making the subscriber user " +
                "password change request:" + changePasswordResponse.getResponseMessage());

        // test whether the password change has been effected correctly
        try {
            new RestAPIStoreImpl(USER_NAME, NEW_USER_PASSWORD, tenantDomain, "https://localhost:9943/");
        } catch (RuntimeException e) {
            Assert.fail("Password change has not been executed correctly. New password is not honored. Error "
                                            + "occurred: " + e.getMessage());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Change Tenant Admin Password and verify whether admin can update keys")
    public void testChangeTenantAdminPassword() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            ArrayList grantTypes = new ArrayList();
            grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
            restAPIStoreClient = new RestAPIStoreImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, storeURLHttps);
            //Create application
            ApplicationDTO appOfTenantAdminDTO = restAPIStoreClient.addApplication("Tenant1App",
                    APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant admin");
            appId = appOfTenantAdminDTO.getApplicationId();
            ApplicationKeyDTO appDTO = restAPIStoreClient.generateKeys(appId,
                    APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

            //Update tenant admin password
            TenantInfoBean tenantInfoBean = new TenantInfoBean();
            tenantInfoBean = tenantManagementServiceClient.getTenant(TENANT_DOMAIN);
            tenantInfoBean.setAdminPassword("newPassword");
            tenantManagementServiceClient.updateTenant(tenantInfoBean);
            // test whether the password change has been effected correctly
            try {
                restAPIStoreClient = new RestAPIStoreImpl(TENANT_ADMIN, "newPassword", TENANT_DOMAIN,
                        "https://localhost:9943/");
            } catch (RuntimeException e) {
                Assert.fail("Password change has not been executed correctly. New password is not honored. Error "
                        + "occurred: " + e.getMessage());
            }
            // Generate keys for application
            ApiResponse<ApplicationKeyDTO> newDTO = restAPIStoreClient.
                    updateKeys(appId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.toString(), appDTO);
            Assert.assertNotNull(newDTO);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        userManagementClient.deleteUser(USER_NAME);
    }
}
