/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.execution.io.ExecutionService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 */
public abstract class RoddyUITask<T> extends Task<T> {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(RoddyUITask.class.getSimpleName());
    private static final ReentrantLock activeTaskCountLock = new ReentrantLock();
    private static final ReentrantLock lightWeightTaskIDCounterLock = new ReentrantLock();
    private static final String METHOD_CALL = "call";
    private static final String METHOD_SUCCEEDED = "succeeded";
    private static final String METHOD_FAILED = "failed";
    private static final String METHOD_CANCELLED = "cancelled";
    private static final String METHOD_INVOKELATER = "invokelater";
    private static int activeTaskCount = 0;
    private static int taskIDCounter;
    private static int lightWeightTaskIDCounter;
    private static IntegerProperty _activeTaskCount = new SimpleIntegerProperty();
    private static javafx.beans.property.MapProperty _activeListOfTasks = new SimpleMapProperty(FXCollections.synchronizedObservableMap(FXCollections.observableMap(new LinkedHashMap<Integer, String>())));

    /**
     * Keeps a list of measurement information objects for all called tasks.
     */
    private static Map<String, TaskMeasurementInfo> taskMeasurementsMap = new LinkedHashMap<>();
    private int taskID = -1;
    private String taskName = "";
    private double taskDuration = -1;
    private boolean printTask;
    private static Object listOfAllMeasurementObjects;

    public RoddyUITask(String taskName) {
        this(taskName, true);
    }

    public RoddyUITask(String taskName, boolean printTask) {
        this.taskName = taskName != null ? taskName : "anonymous";
        taskID = getNextTaskID();
        this.taskName += ":" + taskID;
        this.printTask = printTask;
    }

    private static synchronized int getNextTaskID() {
        taskIDCounter++;
        return taskIDCounter;
    }

    private static void incrementActiveTaskCount() {
        activeTaskCountLock.lock();
        activeTaskCount++;
        activeTaskCountLock.unlock();
        _activeTaskCount.setValue(activeTaskCount);
    }

    private static void decrementActiveTaskCount() {
        activeTaskCountLock.lock();
        activeTaskCount--;
        activeTaskCountLock.unlock();
        _activeTaskCount.setValue(activeTaskCount);
    }

    public static int getActiveTaskCount() {
        return activeTaskCount;
    }

    public static IntegerProperty activeTaskCountProperty() {
        return _activeTaskCount;
    }

    private static void addActiveTaskInfo(String taskName, int id) {
        if (taskName.startsWith("donttrack::")) return;
        logger.postRareInfo("Add " + id + ":" + taskName);
        _activeListOfTasks.put(id, taskName);
    }

    private static void removeActiveTaskInfo(String taskName, int id) {
        if (taskName.startsWith("donttrack::")) return;
        if (_activeListOfTasks.containsKey(id)) {
            logger.postRareInfo("Remove " + id + ":" + taskName);
            _activeListOfTasks.remove(id);
        } else {
            logger.postRareInfo("Could not remove " + id + ":" + taskName);
        }
    }

    public static MapProperty activeListOfTasksProperty() {
        return _activeListOfTasks;
    }

    public static void runTask(RoddyUITask task) {
        task.runTask();
    }

    /**
     * Runs a task immediately, if possible.
     *
     * @param r
     * @param taskNameIn
     */
    public static void invokeASAP(final Runnable r, String taskNameIn) {
        invokeASAP(r, taskNameIn, true);
    }

    /**
     * Runs a task immediately, if possible.
     *
     * @param r
     * @param taskNameIn
     */
    public static void invokeASAP(final Runnable r, String taskNameIn, boolean printMessages) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            invokeLater(r, taskNameIn, printMessages);
        }
    }

    /**
     * Invoke Platform.runLater on a runnable and measure it.
     *
     * @param r
     */
    public static void invokeLater(final Runnable r, String taskNameIn) {
        invokeLater(r, taskNameIn, true);
    }

    public static void invokeLater(final Runnable r, String taskNameIn, final boolean printMessages) {
        lightWeightTaskIDCounterLock.lock();
        final long lightWeightTaskID = lightWeightTaskIDCounter++;
        lightWeightTaskIDCounterLock.unlock();
        final String taskName = taskNameIn + ":" + lightWeightTaskID;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (printMessages && !taskName.startsWith("null"))
                    logger.info(String.format("UI lightweight update task %s started.", taskName));
                long t = startMeasurement(taskName, METHOD_INVOKELATER);
                int id = getNextTaskID();
                try {
                    RoddyUIController.getMainUIController().executionStarted(t, taskName);
                    incrementActiveTaskCount();
                    addActiveTaskInfo(taskName, id);
                    try {
                        r.run();
                    } catch (Exception ex) {
                        logger.severe("Error in Platform.invokeLater() of task " + taskName + "\n" + ex.toString());
                    }
                    double duration = stopMeasurement(t, taskName, METHOD_INVOKELATER);
                    if (printMessages && !taskName.startsWith("null"))
                        logger.info(String.format("UI lightweight update task %s finished after %8.2f ms, with currently %d active tasks.", taskName, duration, activeTaskCount));
                } finally {
                    RoddyUIController.getMainUIController().executionFinished(t, taskName);
                    decrementActiveTaskCount();
                    removeActiveTaskInfo(taskName, id);
                }
            }
        });
    }

    private static long startMeasurement(String taskName, String method) {
        String key = extractKeyString(taskName, method);
        synchronized (taskMeasurementsMap) {
            if (!taskMeasurementsMap.containsKey(key))
                taskMeasurementsMap.put(key, new TaskMeasurementInfo(key));
        }
        return ExecutionService.measureStart();
    }

    private static String extractKeyString(String taskName, String method) {
        return taskName.split(StringConstants.SPLIT_COLON)[0] + "_" + method;
    }

    public static List<TaskMeasurementInfo> getListOfAllMeasurementObjects() {
        LinkedList<TaskMeasurementInfo> taskMeasurementInfos;
        synchronized (taskMeasurementsMap) {
            taskMeasurementInfos = new LinkedList<>(taskMeasurementsMap.values());
        }
        return taskMeasurementInfos;
    }

    protected abstract T _call() throws Exception;

    protected void _succeeded() {
    }

    protected void _failed() {
    }

    protected void _cancelled() {
    }

    @Override
    protected final T call() throws Exception {
        if (printTask)
            logger.info(String.format("Task %s started.", taskName));
        long t = startMeasurement(taskName, METHOD_CALL);
        T result = null;
        try {
            RoddyUIController.getMainUIController().executionStarted(t, taskName);
            incrementActiveTaskCount();
            addActiveTaskInfo(taskName, taskID);
            result = _call();
        } catch (Exception ex) {
            System.out.println(ex);
            logger.severe("Error in _call() of task " + taskName + "\n" + ex.toString());
            throw ex;
        } finally {
            stopMeasurement(t, taskName, METHOD_CALL);
            RoddyUIController.getMainUIController().executionFinished(t, taskName);
            decrementActiveTaskCount();
            removeActiveTaskInfo(taskName, taskID);
        }
        return result;
    }

    private static double stopMeasurement(long id, String taskName, String method) {
        double duration = ExecutionService.measureStop(id, null);
        String key = extractKeyString(taskName, method);
        TaskMeasurementInfo tmi;
        synchronized (taskMeasurementsMap) {
            tmi = taskMeasurementsMap.get(key);
        }
        tmi.addDuration(duration);
        return duration;
    }

    @Override
    protected final void succeeded() {
        long t = startMeasurement(taskName, METHOD_SUCCEEDED);
        try {
            _succeeded();
        } catch (Exception ex) {
            logger.severe("Error in _succeeded() of task " + taskName + "\n" + ex.toString());
            logger.severe(RoddyIOHelperMethods.getStackTraceAsString(ex));
        } finally {
            double tf = stopMeasurement(t, taskName, METHOD_SUCCEEDED);
            if (printTask)
                logger.info(String.format("Task %s finished after %8.2f ms, _succeeded took %8.2f ms with currently %d active tasks.", taskName, taskDuration, tf, activeTaskCount));
        }
    }

    @Override
    protected final void failed() {
        long t = startMeasurement(taskName, METHOD_FAILED);
        try {
            _failed();
            System.out.println(getException());
            System.out.println(getMessage());
        } catch (Exception ex) {
            logger.severe("Error in _failed() of task " + taskName + "\n" + ex.toString());
        } finally {
            double tf = stopMeasurement(t, taskName, METHOD_FAILED);
            if (printTask)
                logger.warning(String.format("Task %s failed after %8.2f ms, _failed took %8.2f ms with currently %d active tasks.", taskName, taskDuration, tf, activeTaskCount));
        }
    }

    @Override
    protected final void cancelled() {
        long t = startMeasurement(taskName, METHOD_CANCELLED);
        try {
            _cancelled();
        } catch (Exception ex) {
            logger.severe("Error in _cancelled() of task " + taskName + "\n" + ex.toString());
        } finally {
            double tf = stopMeasurement(t, taskName, METHOD_CANCELLED);
            if (printTask)
                logger.info(String.format("Task %s cancelled after %8.2f ms, _cancelled took %8.2f ms with currently %d active tasks.", taskName, taskDuration, tf, activeTaskCount));
        }
    }

    /**
     * Run this task in a thread.
     */
    public void runTask() {
        Thread t = new Thread(this);
        t.setName(this.taskName);
        t.start();
    }

    public static class TaskMeasurementInfo {
        private List<Double> listOfDurations = new LinkedList<>();
        private double cumulatedTaskDuration;
        private String id;

        public TaskMeasurementInfo(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public synchronized void addDuration(double value) {
            cumulatedTaskDuration += value;
            listOfDurations.add(value);
        }

        public int getNumberOfCalls() {
            return listOfDurations.size();
        }

        public double getMeanValueInMicros() {
            return cumulatedTaskDuration / listOfDurations.size();
        }
    }
}
