package de.dkfz.roddy.tools;

import de.dkfz.roddy.Constants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

/**
 * This class wraps around javas logger class and implements a verbosity level structure.
 * Created by michael on 28.05.14.
 */
public class LoggerWrapper {
    public static final int VERBOSITY_INFO = 5;
    public static final int VERBOSITY_WARNING = 3;
    public static final int VERBOSITY_SEVERE = 1;
    public static final int VERBOSITY_HIGH = 5;
    public static final int VERBOSITY_MEDIUM = 3;
    public static final int VERBOSITY_LOW = 1;
    public static final int VERBOSITY_RARE = 5;
    public static final int VERBOSITY_SOMETIMES = 3;
    public static final int VERBOSITY_ALWAYS = 1;
    private static int verbosityLevel = VERBOSITY_LOW;
    private Logger logger;

    public LoggerWrapper(String name) {
        logger = Logger.getLogger(name);
    }

    public static LoggerWrapper getLogger(String name) {
        return new LoggerWrapper(name);
    }

    public static LoggerWrapper getLogger(Class cls) {
        return new LoggerWrapper(cls.getName());
    }

    public static void setVerbosityLevel(int level) {
        verbosityLevel = level;
    }

    public static int getVerbosityLevel() {
        return verbosityLevel;
    }

    public static boolean isVerbosityLow() {
        return verbosityLevel >= VERBOSITY_SEVERE;
    }

    public static boolean isVerbosityMedium() {
        return verbosityLevel >= VERBOSITY_WARNING;
    }

    public static boolean isVerbosityHigh() {
        return verbosityLevel >= VERBOSITY_INFO;
    }

    private static int getVerbosityLevelFor(Level lvl) {
        if (lvl == Level.INFO) {
            return VERBOSITY_INFO;
        } else if (lvl == Level.WARNING) {
            return VERBOSITY_WARNING;
        } else if (lvl == Level.SEVERE) {
            return VERBOSITY_SEVERE;
        }
        return 0;
    }

    private static Level getVerbosityLevelObject(int lvl) {
        if (lvl == VERBOSITY_INFO) {
            return Level.INFO;
        } else if (lvl == VERBOSITY_WARNING) {
            return Level.WARNING;
        } else if (lvl == VERBOSITY_SEVERE) {
            return Level.SEVERE;
        }
        return Level.SEVERE;
    }

    public void log(int lvl, String text) {
        if(verbosityLevel >= lvl) {
            logger.log(getVerbosityLevelObject(lvl), text);
        }
    }

    public void log(Level lvl, String text) {
        if (getVerbosityLevelFor(lvl) <= verbosityLevel)
            logger.log(lvl, text);
    }

    public void log(Level lvl, String text, Throwable ex) {
        if (getVerbosityLevelFor(lvl) <= verbosityLevel)
            logger.log(lvl, text, ex);
    }

    public void severe(String text) {
        log(Level.SEVERE, text);
    }

    public void severe(String text, Exception ex) {
        log(Level.SEVERE, text);
        log(Level.INFO, RoddyIOHelperMethods.getStackTraceAsString(ex));
    }

    public void warning(String text) {
        log(Level.WARNING, text);
    }

    public void info(String text) {
        log(Level.INFO, text);
    }

    public void postRareInfo(String text) {
        if(verbosityLevel >= VERBOSITY_RARE)
            logger.log(Level.INFO, text);
    }

    public void postAlwaysInfo(String text) {
        if(verbosityLevel >= VERBOSITY_ALWAYS)
            logger.log(Level.INFO, text);
    }

    public void postSometimesInfo(String text) {
        if(verbosityLevel >= VERBOSITY_SOMETIMES)
            logger.log(Level.INFO, text);
    }

    /**
     * Set up the applications logger mechanisms
     * This sets i.e. how messages are printed.
     */
    public static void setup() {

        Logger global = Logger.getLogger("");
        Handler[] handlers = global.getHandlers();
        for (Handler iHandler : handlers) {
            global.removeHandler(iHandler);
        }

        global.setUseParentHandlers(false);
        ConsoleHandler cHandler = new ConsoleHandler();
        cHandler.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord r) {
                if (r.getLoggerName().startsWith("net.schmizz.sshj"))
                    return false;
                return true;
            }
        });
        cHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord r) {
                if (LoggerWrapper.getVerbosityLevel() == 1)
                    return r.getMessage() + Constants.ENV_LINESEPARATOR;
                StringBuilder sb = new StringBuilder();
                sb.append(r.getLevel()).append(" ").append(r.getSourceMethodName()).append(" ").append(r.getLoggerName()).append(":\t").append(formatMessage(r)).append(System.getProperty("line.separator"));
                if (null != r.getThrown()) {
                    sb.append("Throwable occurred: "); //$NON-NLS-1$
                    Throwable t = r.getThrown();
                    PrintWriter pw = null;
                    try {
                        StringWriter sw = new StringWriter();
                        pw = new PrintWriter(sw);
                        t.printStackTrace(pw);
                        sb.append(sw.toString());
                    } finally {
                        if (pw != null) {
                            try {
                                pw.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }
                return sb.toString();
            }
        });
        global.addHandler(cHandler);
    }
}
