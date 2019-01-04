/*
 *Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.generate.application.tokens;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.*;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApplicationTokenGenerationTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private APIPublisherRestClient apiPublisher;
    private String gatewayHttpsURL;
    private List<String> applicationsList = new ArrayList<>();
    private List<String> apiList = new ArrayList<>();
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String SUBSCRIBER_LOGIN_USERNAME = "subscriber";
    private static final String SUBSCRIBER_LOGIN_PW = "subscriber";
    private static final String CREATOR_LOGIN_USERNAME = "creator";
    private static final String CREATOR_LOGIN_PW = "creator";
    private static final String PUBLISHER_LOGIN_USERNAME = "publisher";
    private static final String PUBLISHER_LOGIN_PW = "publisher";
    private static final String DEFAULT_URL_PREFIX = "https://localhost:9443/";
    private static final String UTF_8 = "UTF-8";
    private static final String APPLICATION_NAME_PREFIX = "ApplicationApiInvocation";
    private static final String API_NAME_PREFIX = "ApiInvocation";
    private static final String API_VERSION = "1.0.0";
    private static final String API_RESOURCE = "/menu";
    private static final String ERROR_APPLICATION_KEY_GENERATION_FAILED = " key generation failed for application:  ";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String DATA = "data";
    private static final String KEY = "key";
    private static final String KEY_STATE = "keyState";
    private static final String APP_DETAILS = "appDetails";
    private static final String KEY_TYPE = "key_type";
    private static final String PRODUCTION = "PRODUCTION";
    private static final String SANDBOX = "SANDBOX";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        Properties infraProperties = getDeploymentProperties();
        gatewayHttpsURL = infraProperties.getProperty(GATEWAY_HTTPS_URL);

        if(gatewayHttpsURL == null) {
            gatewayHttpsURL = "https://localhost:8243/";
        }

        String storeURL = infraProperties.getProperty(STORE_URL);
        if (storeURL == null) {
            storeURL = DEFAULT_URL_PREFIX + "store";
        }
        setKeyStoreProperties();
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);

        String publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        if (publisherURL == null) {
            publisherURL = DEFAULT_URL_PREFIX + "publisher";
        }
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }

    @Test(description = "4.2.1.1")
    public void testGenerateTokenToInvokeAPIForOAuthApp() throws Exception {
        String applicationName = "AppOAuth";
        String tokenType = "OAuth";
        createApplication(APPLICATION_NAME_PREFIX + applicationName, tokenType);
        createAndPublishAPI(API_NAME_PREFIX + applicationName);
        subscribeToAPI(API_NAME_PREFIX + applicationName,
                APPLICATION_NAME_PREFIX + applicationName);
        generateKeyAndInvokeAPI(applicationName, PRODUCTION);
        generateKeyAndInvokeAPI(applicationName, SANDBOX);
    }

    private void createApplication(String applicationName, String tokenType) throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplicationWithTokenType(URLEncoder.encode(applicationName, UTF_8),
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "",
                        URLEncoder.encode(tokenType, UTF_8));
        applicationsList.add(applicationName);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), STATUS_APPROVED,
                "Application creation failed for application: " + applicationName);
    }

    private void createAPI(String apiName) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, "public", API_VERSION,
                API_RESOURCE, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, new URL("https://localhost:9443/am/sample/pizzashack/v1/api/"));
        HttpResponse createAPIResponse = apiPublisher.addAPI(apiRequest);
        apiList.add(apiName);
        verifyResponse(createAPIResponse);
        verifyApiCreation(apiName);
    }

    private void verifyApiCreation(String apiName) throws Exception {
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
    }

    private void createAndPublishAPI(String apiName) throws Exception {
        createAPI(apiName);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse updateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyApiStatusChange(updateResponse, "PUBLISHED");
    }

    private void verifyApiStatusChange(HttpResponse apiUpdateResponse, String status) throws Exception {
        JSONArray updateStatus= new JSONObject(apiUpdateResponse.getData()).getJSONArray("lcs");
        assertTrue(updateStatus.get(updateStatus.length() - 1).toString().contains("\"newStatus\":\"" + status +"\""),
                "API publish failed");
    }

    private void subscribeToAPI(String apiName, String applicationName) throws Exception {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName,
                ADMIN_LOGIN_USERNAME);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse subscribeAPIResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(subscribeAPIResponse);
    }

    private void generateKeyAndInvokeAPI(String applicationName, String keyType) throws Exception {
        HttpResponse keyGenerateResponse = keyGenerationForApplication(APPLICATION_NAME_PREFIX + applicationName,
                keyType);
        String accessKey = new JSONObject(keyGenerateResponse.getData()).getJSONObject("data").getJSONObject("key")
                .get("accessToken").toString();
        invokeApi( gatewayHttpsURL + API_NAME_PREFIX + applicationName + "/" + API_VERSION + API_RESOURCE,
                accessKey);
    }

    private HttpResponse keyGenerationForApplication(String applicationName, String keyType) throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        appKeyRequestGenerator.setKeyType(keyType);
        HttpResponse keyGenerateResponse = apiStore.generateApplicationKey(appKeyRequestGenerator);
        verifyResponse(keyGenerateResponse);
        JSONObject keyGenerateResponseJson = new JSONObject(keyGenerateResponse.getData());
        assertEquals(keyGenerateResponseJson.getJSONObject(DATA).getJSONObject(KEY).getString(KEY_STATE), STATUS_APPROVED,
                keyType.toLowerCase() + ERROR_APPLICATION_KEY_GENERATION_FAILED + applicationName);
        assertEquals(new JSONObject(keyGenerateResponseJson.getJSONObject(DATA).getJSONObject(KEY).getString(APP_DETAILS))
                .get(KEY_TYPE), keyType, keyType.toLowerCase() + ERROR_APPLICATION_KEY_GENERATION_FAILED
                + applicationName);
        return keyGenerateResponse;
    }

    private void invokeApi(String apiResourceURL, String accessToken) throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse invokeResponse = HttpClient.doGet(apiResourceURL,requestHeaders);
        Assert.assertNotNull(invokeResponse, "API invocation response object is null");
        Assert.assertEquals(invokeResponse.getResponseCode(), HttpStatus.SC_OK,
                "API invocation response code is not as expected");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        for (String name : apiList) {
            apiPublisher.deleteAPI(name, API_VERSION, ADMIN_LOGIN_USERNAME);
        }
        applicationsList.clear();
    }
}
