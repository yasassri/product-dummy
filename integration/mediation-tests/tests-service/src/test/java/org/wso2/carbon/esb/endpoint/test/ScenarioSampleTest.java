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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.concurrent.TimeUnit;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class ScenarioSampleTest {
    protected Log log = LogFactory.getLog(getClass());
    private CarbonAppUploaderClient carbonAppUploaderClient;
    private ApplicationAdminClient applicationAdminClient;
    private final int MAX_TIME = 120000;
    private final String carFileName = "SOAPToJSONCarbonApplication_1.0.0";
    private boolean isCarFileUploaded = false;
    String resourceLocation = System.getProperty("framework.resource.location");

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        String backendURL = "https://localhost:9443/services/";
        String sessionCookie;

        setKeyStoreProperties();
        AuthenticatorClient authenticatorClient = new AuthenticatorClient(backendURL);
        sessionCookie = authenticatorClient.login("admin", "admin", "localhost");

        carbonAppUploaderClient = new CarbonAppUploaderClient(backendURL, sessionCookie);
        carbonAppUploaderClient.uploadCarbonAppArtifact(carFileName
                , new DataHandler(new FileDataSource(new File(resourceLocation + File.separator + "artifacts" + File
                        .separator +
                                                              carFileName + ".car")
                )));
        isCarFileUploaded = true;
        applicationAdminClient = new ApplicationAdminClient(backendURL, sessionCookie);
        Assert.assertTrue(isCarFileDeployed(carFileName), "Car file deployment failed");
        TimeUnit.SECONDS.sleep(5);
    }

    @Test(description = "Test HTTP the transformation")
    public void testMessageTransformation() throws Exception {
        // Invoke the service and invoke
    }

    @AfterClass(description = "Test HTTP the transformation")
    public void close() throws Exception {

        //super.cleanup();
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

    private String getBackEndURL() {
        String bucketLocation = System.getenv("DATA_BUCKET_LOCATION");
        String url = null;

        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(bucketLocation + "/deployment.properties");
            prop.load(input);
            url = prop.getProperty("url");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Construct the proper URL if required
        return url;
    }

    private void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation  + "/keystores/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }
}
