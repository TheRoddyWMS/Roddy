package de.dkfz.roddy.core;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
*/
public abstract class AsyncDataFetcherTask<T> implements Runnable {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(AsyncDataFetcherTask.class.getSimpleName());

    private static synchronized final long getID() {
        return System.nanoTime();
    }

    private final long id = getID();

    public AsyncDataFetcherTask() {
        run();
    }

    public long getId() {
        return id;
    }

    @Override
    public void run() {
        try {
            done(fetch());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while processing async data fetching task.", ex);
            error(ex);
        }
    }

    public abstract T fetch();

    public void postIntermediate(T result) {};

    public void done(T result) {};

    public void error(Exception ex) {};
}
