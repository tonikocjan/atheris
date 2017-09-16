package compiler.logger;

/**
 * Created by Toni Kocjan on 16/09/2017.
 * Triglav Partner BE
 */

public class LoggerFactory {

    private static LoggerInterface logger;
    private static Class loggerImpl;

    public static LoggerInterface logger() {
        if (logger == null) initLogger();
        return logger;
    }

    private static void initLogger() {
        loggerImpl = Logger.class;
        try {
            logger = ((LoggerInterface) loggerImpl.newInstance());
        }
        catch (InstantiationException e) {

        }
        catch (IllegalAccessException e) {

        }
    }

    public static<T extends LoggerInterface> void setLoggerImplementation(Class<T> impl) {
        loggerImpl = impl;
    }
}
