package com.jnet.image;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusManager;

import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/9/18 13:24:07
 */
public class AddStatusListenerApp {
    public static void main(String[] args) {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusManager statusManager = lc.getStatusManager();
        OnConsoleStatusListener onConsoleStatusListener = new OnConsoleStatusListener();
        statusManager.add(onConsoleStatusListener);

        Logger logger = LoggerFactory.getLogger("myApp");
        logger.info("Entering application");

        logger.info("Exiting application");

        StatusPrinter.print(statusManager);
    }
}
