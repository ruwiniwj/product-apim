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

package org.wso2.am.scenario.tests.register.application;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApplicationCreationNegativeTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String SUBSCRIBER_LOGIN_USERNAME_1 = "AppCreationNegSubscriberA";
    private static final String SUBSCRIBER_LOGIN_PW_1 = "AppCreationNegSubscriberA";
    private static final String SUBSCRIBER_LOGIN_USERNAME_2 = "AppCreationNegSubscriberB";
    private static final String SUBSCRIBER_LOGIN_PW_2= "AppCreationNegSubscriberB";
    private static final String UTF_8 = "UTF-8";
    private static final String ERROR_APP_CREATION_FAILED = "Application creation failed for application: ";
    private static final String ERROR_APP_CREATION_NEGATIVE_TEST = "Error in application creation" +
            " negative test cases. Application: ";
    private static final String ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS
            = "Application name longer than 70 characters. Application: ";
    private static final String ERROR_DUPLICATE_APPLICATION_EXIST = "A duplicate application already exists" +
            " by the name - ";
    private static final String ERROR_GENERATING_KEY = " key generated for unowned application:  ";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String PRODUCTION = "PRODUCTION";
    private static final String SANDBOX = "SANDBOX";
    private static final String APPLICATION_NAME_PREFIX = "Application_";
    private static final String APPLICATION_NAME_LONGER_THAN_70_CHARS =
            "ApplicationNameLongerThan70CharactersApplicationNameLongerThan70Characters";
    private static final String APPLICATION_DESCRIPTION = "Application description";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, APIManagementException {
        createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1,
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1);
    }

    @Test(description = "4.1.1.7", dataProvider = "MissingMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithMissingMandatoryValues(String applicationName,
                                                                  String url, String errorMessage) throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(url.replace("{{backendURL}}", storeURL));
        verifyApplicationNotCreated(addApplicationResponse, errorMessage, applicationName);
    }

    @Test(description = "4.1.1.8", dataProvider = "InvalidMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithInvalidMandatoryValues(String applicationName, String tier,
                                                                  String errorMessage) throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(applicationName, UTF_8), URLEncoder.encode(tier, UTF_8)
                        , "", "");
        verifyApplicationNotCreated(addApplicationResponse, errorMessage, applicationName);
    }

    @Test(description = "4.1.1.9")
    public void testDuplicateApplicationName() throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "duplicateAppCreation", UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_NAME_PREFIX + "duplicateAppCreation");
        applicationsList.add(APPLICATION_NAME_PREFIX + "duplicateAppCreation");
//        add duplicate application - case sensitive
        addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "duplicateAppCreation", UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        verifyApplicationNotCreated(addApplicationResponse,
                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_NAME_PREFIX + "duplicateAppCreation",
                APPLICATION_NAME_PREFIX + "duplicateAppCreation");
//        todo uncomment if duplicate name check should be case insensitive
////        add duplicate application - case insensitive
//        addApplicationResponse = apiStore
//                .addApplication(URLEncoder.encode((APPLICATION_NAME_PREFIX + "duplicateAppCreation").toLowerCase(), UTF_8),
//                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
//                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
//        verifyApplicationNotCreated(addApplicationResponse,
//                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_NAME_PREFIX + "duplicateAppCreation",
//                APPLICATION_NAME_PREFIX + "duplicateAppCreation");
    }

//    todo uncomment once jappery api validation is fixed
//    @Test(description = "4.1.1.10")
//    public void testApplicationNameLongerThan70CharactersDataProvider() throws Exception {
//        HttpResponse addApplicationResponse = apiStore
//                .addApplication(URLEncoder.encode(APPLICATION_NAME_LONGER_THAN_70_CHARS, UTF_8),
//                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8)
//                        , "", URLEncoder.encode("", UTF_8));
//        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
////        if application added due to test failure add it to application list so that it could be removed later
//        if (!addApplicationJsonObject.getBoolean(ERROR)
//                && addApplicationJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
//            applicationsList.add(APPLICATION_NAME_LONGER_THAN_70_CHARS);
//        }
////        validate application wasn't created
//        assertTrue(addApplicationJsonObject.getBoolean(ERROR),
//                ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS + APPLICATION_NAME_LONGER_THAN_70_CHARS);
//    }

    @Test(description = "4.1.1.11")
    public void testTokenGenerationForOthersApplications() throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                        UTF_8), URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
        applicationsList.add(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications");
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications");
        createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2,
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2);
        testTokenGenerationFailure(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                PRODUCTION);
        testTokenGenerationFailure(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                SANDBOX);
    }

    private void verifyApplicationNotCreated(HttpResponse response, String errorMessage, String applicationName) {
        JSONObject responseJsonObject = new JSONObject(response.getData());
//        if application added due to test failure add it to application list so that it could be removed later
        if (!responseJsonObject.getBoolean(ERROR)
                && responseJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
            applicationsList.add(applicationName);
        }
//        validate application wasn't created
        assertTrue(responseJsonObject.getBoolean(ERROR),
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
        assertTrue(responseJsonObject.getString(MESSAGE).trim().contains(errorMessage),
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
    }

    private void testTokenGenerationFailure(String applicationName, String keyType)
            throws APIManagerIntegrationTestException{
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(APPLICATION_NAME_PREFIX +
                "generateTokensForUnownedApplications");
        appKeyRequestGenerator.setKeyType(keyType);
        JSONObject responseStringJson = new JSONObject(apiStore.generateApplicationKey(
                appKeyRequestGenerator).getData());
        assertTrue(responseStringJson.getBoolean(ERROR), keyType + ERROR_GENERATING_KEY +applicationName);
        assertEquals(responseStringJson.getString("message"),
                "Error occurred while executing the action generateApplicationKey", keyType +
                        ERROR_GENERATING_KEY + applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1);
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        applicationsList.clear();
        deleteUser(SUBSCRIBER_LOGIN_USERNAME_1, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(SUBSCRIBER_LOGIN_USERNAME_2, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }

}
