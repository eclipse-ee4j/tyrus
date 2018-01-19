/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.tyrus.tests.qa;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.safari.SafariDriver;


/**
 * @author mikc
 */
public class SeleniumToolkit {

    enum Browser {

        FIREFOX, IE, CHROME, SAFARI
    }

    ;
    private final String WIN_FIREFOX_BIN = "C:/Program Files (x86)/Mozilla Firefox/firefox.exe";
    private static final Logger logger = Logger.getLogger(SeleniumToolkit.class.getCanonicalName());
    private static List<WebDriver> webDriverInstances = new CopyOnWriteArrayList<WebDriver>();
    private WebDriver driver;
    private long sleepTime = 500;

    public SeleniumToolkit(Browser browser) {
        switch (browser) {
            case FIREFOX:
                setUpFirefox();
                break;
            case IE:
                setUpExplorer();
                break;
            case CHROME:
                setUpChrome();
                break;
            case SAFARI:
                setUpSafari();
                break;
            default:

        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    String getEnv(String key) {
        String value;

        value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }

        logger.log(Level.INFO, "Retrived {0}={1}", new Object[]{key, value});
        return value;
    }

    public static boolean onWindows() {
        return Platform.WINDOWS.is(Platform.getCurrent());
    }

    public static boolean onMac() {
        return Platform.MAC.is(Platform.getCurrent());
    }

    public static boolean safariPlatform() {
        return onMac() || onWindows();
    }

    private void commonBrowserSetup() {
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
        webDriverInstances.add(driver);
    }

    public void setUpSafari() {
        try {
            //System.setProperty("webdriver.safari.driver", getEnv("SAFARI_EXTENSION"));
            driver = new SafariDriver();
            commonBrowserSetup();
            logger.log(Level.INFO, "Safari Setup PASSED");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Safari Setup FAILED: {0}", ex.getLocalizedMessage());
            ex.printStackTrace();
        } finally {
            assert driver != null : "Driver is null";
        }
    }

    public void setUpChrome() {
        ChromeDriverService service;

        try {
            System.setProperty("webdriver.chrome.driver", getEnv("CHROME_DRIVER"));
            service = new ChromeDriverService.Builder()
                    .usingAnyFreePort()
                    .usingDriverExecutable(new File(getEnv("CHROME_DRIVER")))
                    .build();
            service.start();
            logger.log(Level.INFO, "ChromeDriverService.URL: {0}", service.getUrl());
            driver = new ChromeDriver(service);
            commonBrowserSetup();
            logger.log(Level.INFO, "Chrome Setup PASSED");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Chrome Setup FAILED: {0}", ex.getLocalizedMessage());
            ex.printStackTrace();
        } finally {
            assert driver != null : "Driver is null";
        }
    }

    public void setUpExplorer() {
        try {
            System.setProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY, getEnv("IE_DRIVER"));
            assert new File(System.getProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY)).exists() :
                    "IE_DRIVER exists";
            driver = new InternetExplorerDriver();
            commonBrowserSetup();
            logger.log(Level.INFO, "IE Setup PASSED");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "IE Setup FAILED: {0}", ex.getLocalizedMessage());
            ex.printStackTrace();
        } finally {
            assert driver != null : "Driver is null";
        }
    }

    public void setUpFirefox() {
        try {
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("network.proxy.type", 0);
            if (onWindows()) {
                assert new File(WIN_FIREFOX_BIN).exists() : WIN_FIREFOX_BIN + " exists";
                driver = new FirefoxDriver(new FirefoxBinary(new File(WIN_FIREFOX_BIN)), profile);
            } else {
                driver = new FirefoxDriver(profile);
            }
            commonBrowserSetup();
            logger.log(Level.INFO, "FF Setup PASSED");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "FF Setup FAILED: {0}", ex.getLocalizedMessage());
            ex.printStackTrace();
        } finally {
            assert driver != null : "Driver is null";
        }
    }

    public static String calcTimestap() {
        long millis = System.currentTimeMillis();
        SimpleDateFormat date_format = new SimpleDateFormat("MMM_dd_yyyy_HH.mm.ss.SSS");
        Date resultdate = new Date(millis);
        return date_format.format(resultdate);
    }

    /**
     * Takes screenshot of BUI. This is usually used if a test fails
     */
    public String takeScreenshot(WebDriver driver) {
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        //System.out.println("XXX:"+scrFile.getAbsolutePath());
        File dstFile = new File(
                "./testcase_" + calcTimestap() + ".png");
        try {
            FileUtils.copyFile(scrFile, dstFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Impossible to write into file " + dstFile.getAbsolutePath());
            ex.printStackTrace();
        }
        logger.log(Level.INFO, "Took screenshot into: {0}", dstFile.getAbsolutePath());
        return dstFile.getName();
    }

    public void get(String url) throws InterruptedException {
        logger.log(Level.INFO, "GET {0}", url);
        driver.get(url);
        Thread.sleep(sleepTime);
        takeScreenshot(driver);
        logger.log(Level.INFO, "========BODY=======\n{0}\n=======================",
                   driver.findElement(By.tagName("body")).getText());
    }

    public void click(By by) throws Exception {
        WebElement el = driver.findElement(by);
        if (el == null) {
            throw new Exception("Can't find element: " + by.toString());
        }
        Thread.sleep(sleepTime);
        takeScreenshot(driver);
        el.click();
    }

    public void sendKeys(By by, String text) throws InterruptedException, Exception {
        WebElement el = driver.findElement(by);
        if (el == null) {
            throw new Exception("Can't find element: " + by.toString());
        }
        Thread.sleep(sleepTime);
        takeScreenshot(driver);
        el.clear();
        el.sendKeys(text);
    }

    public String bodyText() {
        return getTextByTagName("body");
    }

    public static void quit() {
        for (WebDriver driver : webDriverInstances) {
            driver.quit();
        }
    }

    public String getTextById(String id) {
        return driver.findElement(By.id(id)).getText();
    }

    public String getValue(By by) {
        return driver.findElement(by).getAttribute("value");
    }

    public String getTextByTagName(String name) {
        return driver.findElement(By.tagName(name)).getText();
    }
}
