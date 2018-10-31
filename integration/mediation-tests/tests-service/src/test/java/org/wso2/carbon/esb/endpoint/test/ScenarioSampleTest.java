/*
 *Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.endpoint.test;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.common.admin.client.ApplicationAdminClient;
import org.wso2.carbon.integration.common.admin.client.CarbonAppUploaderClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class ScenarioSampleTest {

    protected Log log = LogFactory.getLog(getClass());
    private CarbonAppUploaderClient carbonAppUploaderClient;
    private ApplicationAdminClient applicationAdminClient;
    private final int MAX_TIME = 120000;
    private final String carFileName = "SOAPToJSONCarbonApplication_1.0.0";
    String resourceLocation = System.getProperty("framework.resource.location");
    int timeout = 5;
    RequestConfig config = RequestConfig.custom()
                                        .setConnectTimeout(timeout * 100)
                                        .setConnectionRequestTimeout(timeout * 1000)
                                        .setSocketTimeout(timeout * 1000).build();

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        String backendURL = "https://" + getBackEndEP() + ":9443/services/";

        setKeyStoreProperties();
        AuthenticatorClient authenticatorClient = new AuthenticatorClient(backendURL);
        String sessionCookie = authenticatorClient.login("admin", "admin", getBackEndEP());
        log.info("The Backend service URL : " + backendURL);
        carbonAppUploaderClient = new CarbonAppUploaderClient(backendURL, sessionCookie);
        DataHandler dh = new DataHandler(new FileDataSource(new File(resourceLocation + File.separator + "artifacts" +
                                                                     File.separator + carFileName + ".car")));
        carbonAppUploaderClient.uploadCarbonAppArtifact(carFileName + ".car", dh);
        applicationAdminClient = new ApplicationAdminClient(backendURL, sessionCookie);
        Assert.assertTrue(isCarFileDeployed(carFileName), "Car file deployment failed");
    }

    @Test(description = "Test HTTP the transformation", enabled = true)
    public void testMessageTransformation() throws Exception {
        // Invoke the service and invoke
        String restURL = "http://" + getBackEndEP() + ":8280/city/lookup/60601";
        log.info("The API Endpoint : " + restURL);
        HttpGet httpGet = new HttpGet(restURL);
        Thread.sleep(1000);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                log.info("The response status : " + response.getStatusLine());
                String responseString = "{\n" +
                                        "  \"LookupCityResult\": {\n" +
                                        "    \"City\": \"Chicago\",\n" +
                                        "    \"State\": \"IL\",\n" +
                                        "    \"Zip\": 60601\n" +
                                        "  }\n" +
                                        "}";
                Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
                Assert.assertEquals(IOUtils.toString(response.getEntity().getContent()), responseString);
            }
        } catch (IOException e) {
            //throw e;
        }
    }

    @Test(description = "Test HTTP the transformation when a invalid status code is given")
    public void testMessageTransformationForInvalidCode() throws Exception {
        // Invoke the service and invoke
        String restURL = "http://" + getBackEndEP() + ":8280/city/lookup/606010000";
        HttpGet httpHead = new HttpGet(restURL);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
                log.info(response.getStatusLine());
                String responseString = "{\n" +
                                        "  \"Error\": {\n" +
                                        "    \"message\": \"Error while processing the request\",\n" +
                                        "    \"code\": \"0\",\n" +
                                        "    \"description\": \"Error while building message. Error while building Passthrough stream\"\n" +
                                        "  }\n" +
                                        "}";
                Assert.assertEquals(IOUtils.toString(response.getEntity().getContent()), responseString);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    @Test(description = "Test HTTP the transformation when a invalid status code is given")
    public void testMessageTransformationFailure() throws Exception {
        Assert.assertTrue(false, "This test is intentionally failed!");
    }

    @AfterClass(description = "Test HTTP the transformation")
    public void close() throws Exception {
        // Clean up if required
    }

    private boolean isCarFileDeployed(String carFileName) throws Exception {

        log.info("waiting " + MAX_TIME + " millis for car deployment " + carFileName);
        boolean isCarFileDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < MAX_TIME) {
            String[] applicationList = applicationAdminClient.listAllApplications();
            if (applicationList != null) {
                if (ArrayUtils.contains(applicationList, carFileName)) {
                    isCarFileDeployed = true;
                    log.info("car file deployed in " + time + " mills");
                    return isCarFileDeployed;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isCarFileDeployed;
    }

    private String getBackEndEP() {

        String bucketLocation = System.getenv("DATA_BUCKET_LOCATION");
        String url = null;

        Properties prop = new Properties();
        //InputStream input = null;
        try (InputStream input = new FileInputStream(bucketLocation + "/infrastructure.properties")) {
            prop.load(input);
            url = prop.getProperty("WSO2PublicIP");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Construct the proper URL if required
        return url == null ? "localhost" : url;
    }

    private void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation + "/keystores/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }
}
