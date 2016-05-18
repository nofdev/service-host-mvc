package org.nofdev.servicefacade

import groovy.transform.CompileStatic
import org.slf4j.Logger

/**
 * Created by Qiang on 5/18/16.
 */
@CompileStatic
class LoggerUtil {

    public static void logDurationWrapper(Logger logger, String desc, Runnable runnable) {
        long start = System.currentTimeMillis()
        try {
            runnable.run()
        } catch (Exception e) {
            throw e
        } finally {
            long end = System.currentTimeMillis()
            long duration = end - start
            logger.error("$desc execute $duration ms")
        }
    }

}
