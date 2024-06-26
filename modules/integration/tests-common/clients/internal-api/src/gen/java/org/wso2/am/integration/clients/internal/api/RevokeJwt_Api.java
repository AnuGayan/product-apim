/*
 * Internal Utility API
 * This API allows you to access internal data.
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.internal.api;

import org.wso2.am.integration.clients.internal.ApiCallback;
import org.wso2.am.integration.clients.internal.ApiClient;
import org.wso2.am.integration.clients.internal.ApiException;
import org.wso2.am.integration.clients.internal.ApiResponse;
import org.wso2.am.integration.clients.internal.Configuration;
import org.wso2.am.integration.clients.internal.Pair;
import org.wso2.am.integration.clients.internal.ProgressRequestBody;
import org.wso2.am.integration.clients.internal.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import org.wso2.am.integration.clients.internal.api.dto.ErrorDTO;
import org.wso2.am.integration.clients.internal.api.dto.RevokedEventsDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RevokeJwt_Api {
    private ApiClient apiClient;

    public RevokeJwt_Api() {
        this(Configuration.getDefaultApiClient());
    }

    public RevokeJwt_Api(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for revokedjwtGet
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call revokedjwtGetCall(final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/revokedjwt";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
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
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call revokedjwtGetValidateBeforeCall(final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        

        com.squareup.okhttp.Call call = revokedjwtGetCall(progressListener, progressRequestListener);
        return call;

    }

    /**
     * JTIs of revoked jwt tokens
     * This will provide access to JTIs of revoked JWT tokens in database. 
     * @return RevokedEventsDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public RevokedEventsDTO revokedjwtGet() throws ApiException {
        ApiResponse<RevokedEventsDTO> resp = revokedjwtGetWithHttpInfo();
        return resp.getData();
    }

    /**
     * JTIs of revoked jwt tokens
     * This will provide access to JTIs of revoked JWT tokens in database. 
     * @return ApiResponse&lt;RevokedEventsDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<RevokedEventsDTO> revokedjwtGetWithHttpInfo() throws ApiException {
        com.squareup.okhttp.Call call = revokedjwtGetValidateBeforeCall(null, null);
        Type localVarReturnType = new TypeToken<RevokedEventsDTO>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * JTIs of revoked jwt tokens (asynchronously)
     * This will provide access to JTIs of revoked JWT tokens in database. 
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call revokedjwtGetAsync(final ApiCallback<RevokedEventsDTO> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = revokedjwtGetValidateBeforeCall(progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<RevokedEventsDTO>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
