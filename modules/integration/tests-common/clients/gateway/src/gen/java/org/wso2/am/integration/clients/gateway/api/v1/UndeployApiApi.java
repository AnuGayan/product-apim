/*
 * WSO2 API Manager - Gateway
 * This document specifies a **RESTful API** for WSO2 **API Manager** - Gateway. Please see [full swagger definition](https://raw.githubusercontent.com/wso2/carbon-apimgt/v6.7.206/components/apimgt/org.wso2.carbon.apimgt.rest.api.gateway.v1/src/main/resources/gateway-api.yaml) of the API which is written using [swagger 2.0](http://swagger.io/) specification. 
 *
 * OpenAPI spec version: v1
 * Contact: architecture@wso2.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.gateway.api.v1;

import org.wso2.am.integration.clients.gateway.api.ApiCallback;
import org.wso2.am.integration.clients.gateway.api.ApiClient;
import org.wso2.am.integration.clients.gateway.api.ApiException;
import org.wso2.am.integration.clients.gateway.api.ApiResponse;
import org.wso2.am.integration.clients.gateway.api.Configuration;
import org.wso2.am.integration.clients.gateway.api.Pair;
import org.wso2.am.integration.clients.gateway.api.ProgressRequestBody;
import org.wso2.am.integration.clients.gateway.api.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import org.wso2.am.integration.clients.gateway.api.v1.dto.DeployResponseDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.ErrorDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UndeployApiApi {
    private ApiClient apiClient;

    public UndeployApiApi() {
        this(Configuration.getDefaultApiClient());
    }

    public UndeployApiApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for undeployApiPost
     * @param apiName Name of the API  (required)
     * @param version version of the API  (required)
     * @param tenantDomain Tenant Domain of the API  (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call undeployApiPostCall(String apiName, String version, String tenantDomain, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/undeploy-api";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (apiName != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("apiName", apiName));
        if (version != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("version", version));
        if (tenantDomain != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("tenantDomain", tenantDomain));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call undeployApiPostValidateBeforeCall(String apiName, String version, String tenantDomain, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'apiName' is set
        if (apiName == null) {
            throw new ApiException("Missing the required parameter 'apiName' when calling undeployApiPost(Async)");
        }
        
        // verify the required parameter 'version' is set
        if (version == null) {
            throw new ApiException("Missing the required parameter 'version' when calling undeployApiPost(Async)");
        }
        

        com.squareup.okhttp.Call call = undeployApiPostCall(apiName, version, tenantDomain, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Uneploy the API from the gateway
     * This operation is used to undeploy an API from the gateway. If the Tenant domain is not provided carbon.super will be picked as the Tenant domain. 
     * @param apiName Name of the API  (required)
     * @param version version of the API  (required)
     * @param tenantDomain Tenant Domain of the API  (optional)
     * @return DeployResponseDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DeployResponseDTO undeployApiPost(String apiName, String version, String tenantDomain) throws ApiException {
        ApiResponse<DeployResponseDTO> resp = undeployApiPostWithHttpInfo(apiName, version, tenantDomain);
        return resp.getData();
    }

    /**
     * Uneploy the API from the gateway
     * This operation is used to undeploy an API from the gateway. If the Tenant domain is not provided carbon.super will be picked as the Tenant domain. 
     * @param apiName Name of the API  (required)
     * @param version version of the API  (required)
     * @param tenantDomain Tenant Domain of the API  (optional)
     * @return ApiResponse&lt;DeployResponseDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DeployResponseDTO> undeployApiPostWithHttpInfo(String apiName, String version, String tenantDomain) throws ApiException {
        com.squareup.okhttp.Call call = undeployApiPostValidateBeforeCall(apiName, version, tenantDomain, null, null);
        Type localVarReturnType = new TypeToken<DeployResponseDTO>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Uneploy the API from the gateway (asynchronously)
     * This operation is used to undeploy an API from the gateway. If the Tenant domain is not provided carbon.super will be picked as the Tenant domain. 
     * @param apiName Name of the API  (required)
     * @param version version of the API  (required)
     * @param tenantDomain Tenant Domain of the API  (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call undeployApiPostAsync(String apiName, String version, String tenantDomain, final ApiCallback<DeployResponseDTO> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = undeployApiPostValidateBeforeCall(apiName, version, tenantDomain, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DeployResponseDTO>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
