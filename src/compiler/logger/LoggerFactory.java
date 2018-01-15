package compiler.logger;

/**
 * Created by Toni Kocjan on 16/09/2017.
 * Triglav Partner BE
 */

public class LoggerFactory {

    private static LoggerInterface logger;
    private static Class<? extends LoggerInterface> loggerImpl = Logger.class;

    public static LoggerInterface logger() {
        if (logger == null) initLogger();
        return logger;
    }

    private static void initLogger() {
        try {
            logger = (loggerImpl.newInstance());
        }
        catch (Exception e) {}
    }

    public static void setLoggerImpl(Class<? extends LoggerInterface> loggerImpl) {
        LoggerFactory.loggerImpl = loggerImpl;
    }
}
