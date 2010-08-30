/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class for running multiple application instances for load testing purposes.
 */
public abstract class LoadTestModel {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;

  protected static final Logger LOG = LoggerFactory.getLogger(LoadTestModel.class);

  protected static final Random RANDOM = new Random();

  private final Event evtPausedChanged = Events.event();
  private final Event evtCollectChartDataChanged = Events.event();
  private final Event evtMaximumThinkTimeChanged = Events.event();
  private final Event evtMinimumThinkTimeChanged = Events.event();
  private final Event evtWarningTimeChanged = Events.event();
  private final Event evtLoginDelayFactorChanged = Events.event();
  private final Event evtApplicationtCountChanged = Events.event();
  private final Event evtApplicationBatchSizeChanged = Events.event();
  private final Event evtDoneExiting = Events.event();
  private final Event evtUpdateIntervalChanged = Events.event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int applicationBatchSize;
  private int updateInterval;

  private boolean paused = false;
  private boolean collectChartData = false;

  private final Stack<ApplicationRunner> applications = new Stack<ApplicationRunner>();
  private final Collection<UsageScenario> usageScenarios;
  private final RandomItemModel<UsageScenario> scenarioChooser;
  private User user;

  private final Counter counter;
  private Timer updateTimer;
  private volatile int warningTime;

  private final XYSeries workRequestsSeries = new XYSeries("Scenarios run per second");
  private final XYSeries delayedWorkRequestsSeries = new XYSeries("Duration exceeds warning time");
  private final XYSeriesCollection workRequestsCollection = new XYSeriesCollection();

  private final XYSeriesCollection scenarioDurationCollection = new XYSeriesCollection();

  private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
  private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
  private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

  private final XYSeries numberOfApplicationsSeries = new XYSeries("Application count");
  private final XYSeriesCollection numberOfApplicationsCollection = new XYSeriesCollection();

  private final XYSeriesCollection usageScenarioCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemoryCollection = new XYSeries("Allocated memory");
  private final XYSeries usedMemoryCollection = new XYSeries("Used memory");
  private final XYSeries maxMemoryCollection = new XYSeries("Maximum memory");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();

  /**
   * Constructs a new LoadTestModel.
   * @param user the default user to use when initializing applications
   * @param maximumThinkTime the maximum think time, by default the minimum think time is max / 2
   * @param loginDelayFactor the value with which to multiply the think time when delaying login
   * @param applicationBatchSize the number of applications to add in a batch
   * @param warningTime a work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)
   */
  public LoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor,
                       final int applicationBatchSize, final int warningTime) {
    this(user, new ArrayList<UsageScenario>(), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
  }

  /**
   * Constructs a new LoadTestModel.
   * @param user the default user to use when initializing applications
   * @param usageScenarios the usage scenarios to use
   * @param maximumThinkTime the maximum think time, by default the minimum think time is max / 2
   * @param loginDelayFactor the value with which to multiply the think time when delaying login
   * @param applicationBatchSize the number of applications to add in a batch
   * @param warningTime a work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)
   */
  public LoadTestModel(final User user, final Collection<UsageScenario> usageScenarios, final int maximumThinkTime,
                       final int loginDelayFactor, final int applicationBatchSize, final int warningTime) {
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }
    if (loginDelayFactor <= 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }
    if (warningTime <= 0) {
      throw new IllegalArgumentException("Warning time must be a positive integer");
    }

    this.user = user;
    this.maximumThinkTime = maximumThinkTime;
    this.minimumThinkTime = maximumThinkTime / 2;
    this.loginDelayFactor = loginDelayFactor;
    this.applicationBatchSize = applicationBatchSize;
    this.warningTime = warningTime;
    this.usageScenarios = usageScenarios;
    this.scenarioChooser = initializeScenarioChooser();
    this.counter = new Counter(this.usageScenarios);
    initializeChartData();
    setUpdateInterval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS);
  }

  /**
   * @return the user to use when initializing new application instances
   */
  public final User getUser() {
    return user;
  }

  /**
   * @param user the user to use when initializing new application instances
   */
  public final void setUser(final User user) {
    this.user = user;
  }

  /**
   * @return the RandomItemModel used to select the next scenario to run
   */
  public final RandomItemModel<UsageScenario> getScenarioChooser() {
    return scenarioChooser;
  }

  /**
   * @return the usage scenarios used by this load test model
   */
  public final Collection<UsageScenario> getUsageScenarios() {
    return usageScenarios;
  }

  /**
   * @param usageScenarioName the name of the usage scenario to fetch
   * @return the usage scenario with the given name
   * @throws RuntimeException if no such scenario exists
   */
  public final UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios) {
      if (scenario.getName().equals(usageScenarioName)) {
        return scenario;
      }
    }

    throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
  }

  /**
   * @return a dataset plotting the number of work requests per second
   */
  public final XYSeriesCollection getWorkRequestsDataset() {
    return workRequestsCollection;
  }

  /**
   * @return a dataset plotting the average scenario duration
   */
  public final XYSeriesCollection getScenarioDurationDataset() {
    return scenarioDurationCollection;
  }

  /**
   * @return a dataset plotting the think time
   */
  public final XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  /**
   * @return a dataset plotting the number of active applications
   */
  public final XYSeriesCollection getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  /**
   * @return a dataset plotting the number of runs each usage scenario is being run per second
   */
  public final XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  /**
   * @return a dataset plotting the memory usage of this load test model
   */
  public final XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /**
   * Resets the accumulated chart data
   */
  public final void resetChartData() {
    workRequestsSeries.clear();
    delayedWorkRequestsSeries.clear();
    minimumThinkTimeSeries.clear();
    maximumThinkTimeSeries.clear();
    numberOfApplicationsSeries.clear();
    allocatedMemoryCollection.clear();
    usedMemoryCollection.clear();
    maxMemoryCollection.clear();
    for (final Object series : usageScenarioCollection.getSeries()) {
      ((XYSeries) series).clear();
    }
    for (final Object series : scenarioDurationCollection.getSeries()) {
      ((XYSeries) series).clear();
    }
  }

  /**
   * @return the the maximum time in milliseconds a work request has to finish
   */
  public final int getWarningTime() {
    return warningTime;
  }

  /**
   * @param warningTime the the maximum time in milliseconds a work request has to finish
   */
  public final void setWarningTime(final int warningTime) {
    if (warningTime <= 0) {
      throw new IllegalArgumentException("Warning time must be a positive integer");
    }

    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  /**
   * @return the chart data update interval
   */
  public final int getUpdateInterval() {
    return updateInterval;
  }

  /**
   * @param updateInterval the chart data update interval
   */
  public final void setUpdateInterval(final int updateInterval) {
    if (updateInterval < 0) {
      throw new IllegalArgumentException("Update interval must be a positive integer");
    }

    if (this.updateInterval != updateInterval) {
      this.updateInterval = updateInterval;
      scheduleUpdateTime(updateInterval);
      evtUpdateIntervalChanged.fire();
    }
  }

  /**
   * @return the number of active applications
   */
  public final int getApplicationCount() {
    return applications.size();
  }

  /**
   * @return the number of applications to initialize per batch
   */
  public final int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  /**
   * @param applicationBatchSize the number of applications to initialize per batch
   */
  public final void setApplicationBatchSize(final int applicationBatchSize) {
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }

    this.applicationBatchSize = applicationBatchSize;
    evtApplicationBatchSizeChanged.fire();
  }

  /**
   * Adds a batch of applications.
   * @see #setApplicationBatchSize(int)
   */
  public final void addApplicationBatch() {
    for (int i = 0; i < applicationBatchSize; i++) {
      final ApplicationRunner runner = new ApplicationRunner(this);
      synchronized (applications) {
        applications.push(runner);
      }
      evtApplicationtCountChanged.fire();

      new Thread(runner).start();
    }
  }

  /**
   * Removes one batch of applications.
   * @see #setApplicationBatchSize(int)
   */
  public final void removeApplicationBatch() {
    for (int i = 0; i < applicationBatchSize && !applications.isEmpty(); i++) {
      removeApplication();
    }
  }

  /**
   * @return true if the load testing is paused
   */
  public final boolean isPaused() {
    return this.paused;
  }

  /**
   * @param value true if load testing should be paused
   */
  public final void setPaused(final boolean value) {
    this.paused = value;
    evtPausedChanged.fire();
  }

  /**
   * @return true if chart data is being collected
   */
  public final boolean isCollectChartData() {
    return collectChartData;
  }

  /**
   * @param value true if chart data should be collected
   */
  public final void setCollectChartData(final boolean value) {
    this.collectChartData = value;
    evtCollectChartDataChanged.fire();
  }

  /**
   * Removes all applications
   */
  public final void exit() {
    paused = false;
    synchronized (applications) {
      while (!applications.isEmpty()) {
        removeApplication();
      }
    }
    evtDoneExiting.fire();
  }

  /**
   * @return the maximum number of milliseconds that should pass between work requests
   */
  public final int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   */
  public final void setMaximumThinkTime(final int maximumThinkTime) {
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }

    this.maximumThinkTime = maximumThinkTime;
    evtMaximumThinkTimeChanged.fire();
  }

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   */
  public final int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   */
  public final void setMinimumThinkTime(final int minimumThinkTime) {
    if (minimumThinkTime < 0) {
      throw new IllegalArgumentException("Minimum think time must be a positive integer");
    }

    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the number with which to multiply the think time when logging in
   */
  public final int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @param loginDelayFactor the number with which to multiply the think time when logging in
   */
  public final void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }

    this.loginDelayFactor = loginDelayFactor;
    evtLoginDelayFactorChanged.fire();
  }

  /**
   * @param listener a listener notified when this load test model has finished removing all applications
   */
  public final void addExitListener(final ActionListener listener) {
    evtDoneExiting.addListener(listener);
  }

  /**
   * @return an observer notified each time the application batch size changes
   */
  public final EventObserver applicationBatchSizeObserver() {
    return evtApplicationBatchSizeChanged.getObserver();
  }

  /**
   * @return an observer notified each time the application count changes
   */
  public final EventObserver applicationCountObserver() {
    return evtApplicationtCountChanged.getObserver();
  }

  /**
   * @return an observer notified each time the maximum think time changes
   */
  public final EventObserver maximumThinkTimeObserver() {
    return evtMaximumThinkTimeChanged.getObserver();
  }

  /**
   * @return an observer notified each time the minimum think time changes
   */
  public final EventObserver minimumThinkTimeObserver() {
    return evtMinimumThinkTimeChanged.getObserver();
  }

  /**
   * @return an observer notified each time the paused state changes
   */
  public final EventObserver pauseObserver() {
    return evtPausedChanged.getObserver();
  }

  /**
   * @return an observer notified each time the collect chart data state changes
   */
  public final EventObserver collectChartDataObserver() {
    return evtCollectChartDataChanged.getObserver();
  }

  /**
   * @return an observer notified each time the warning time changes
   */
  public final EventObserver warningTimeObserver() {
    return evtWarningTimeChanged.getObserver();
  }

  /**
   * Selects a random scenario and runs it with the given application
   * @param application the application for running the next scenario
   * @return the name of the scenario that was run
   */
  protected String performWork(final Object application) {
    Util.rejectNullValue(application, "application");
    final String scenarioName = scenarioChooser.getRandomItem().getName();
    try {
      runScenario(scenarioName, application);
    }
    catch (Exception e) {
      LOG.debug("Exception while running scenario: " + scenarioName + " with application: " + application, e);
    }

    return scenarioName;
  }

  /**
   * Runs the scenario with the given name on the given application
   * @param usageScenarioName the name of the scenario to run
   * @param application the application to use
   * @throws ScenarioException in case of an exception
   */
  protected final void runScenario(final String usageScenarioName, final Object application) throws ScenarioException {
    getUsageScenario(usageScenarioName).run(application);
  }

  /**
   * @return an initialized application.
   * @throws CancelException in case the initialization was cancelled
   */
  protected abstract Object initializeApplication() throws CancelException;

  /**
   * @param application the application to disconnect
   */
  protected abstract void disconnectApplication(final Object application);

  /**
   * @return a random think time in milliseconds based on the values of minimumThinkTime and maximumThinkTime
   * @see #setMinimumThinkTime(int)
   * @see #setMaximumThinkTime(int)
   */
  protected final int getThinkTime() {
    final int time = minimumThinkTime - maximumThinkTime;
    return time > 0 ? RANDOM.nextInt(time) + minimumThinkTime : minimumThinkTime;
  }

  /**
   * Removes a single application
   */
  private void removeApplication() {
    synchronized (applications) {
      applications.pop().stop();
    }
    evtApplicationtCountChanged.fire();
  }

  private RandomItemModel<UsageScenario> initializeScenarioChooser() {
    final RandomItemModel<UsageScenario> model = new RandomItemModel<UsageScenario>();
    for (final UsageScenario scenario : this.usageScenarios) {
      model.addItem(scenario, scenario.getDefaultWeight());
    }

    return model;
  }

  private void scheduleUpdateTime(final int intervalMs) {
    if (updateTimer != null) {
      updateTimer.cancel();
    }

    updateTimer = new Timer(true);
    updateTimer.schedule(new TimerTask() {
      /** {@inheritDoc} */
      @Override
      public void run() {
        counter.updateRequestsPerSecond();
        if (collectChartData && !paused) {
          updateChartData();
        }
      }
    }, new Date(), intervalMs);
  }

  private void initializeChartData() {
    workRequestsCollection.addSeries(workRequestsSeries);
    workRequestsCollection.addSeries(delayedWorkRequestsSeries);
    thinkTimeCollection.addSeries(minimumThinkTimeSeries);
    thinkTimeCollection.addSeries(maximumThinkTimeSeries);
    numberOfApplicationsCollection.addSeries(numberOfApplicationsSeries);
    memoryUsageCollection.addSeries(maxMemoryCollection);
    memoryUsageCollection.addSeries(allocatedMemoryCollection);
    memoryUsageCollection.addSeries(usedMemoryCollection);
    for (final UsageScenario usageScenario : this.usageScenarios) {
      usageScenarioCollection.addSeries(new XYSeries(usageScenario.getName()));
      scenarioDurationCollection.addSeries(new XYSeries(usageScenario.getName()));
    }
  }

  private void updateChartData() {
    final long time = System.currentTimeMillis();
    workRequestsSeries.add(time, counter.getWorkRequestsPerSecond());
    delayedWorkRequestsSeries.add(time, counter.getDelayedWorkRequestsPerSecond());
    minimumThinkTimeSeries.add(time, minimumThinkTime);
    maximumThinkTimeSeries.add(time, maximumThinkTime);
    numberOfApplicationsSeries.add(time, applications.size());
    allocatedMemoryCollection.add(time, Util.getAllocatedMemory() / 1000);
    usedMemoryCollection.add(time, Util.getUsedMemory() / 1000);
    maxMemoryCollection.add(time, Util.getMaxMemory() / 1000);
    for (final Object object : usageScenarioCollection.getSeries()) {
      final XYSeries series = (XYSeries) object;
      series.add(time, counter.getScenarioRate((String) series.getKey()));
    }
    for (final Object object : scenarioDurationCollection.getSeries()) {
      final XYSeries series = (XYSeries) object;
      series.add(time, counter.getAverageScenarioDuration((String) series.getKey()));
    }
  }

  private static final class ApplicationRunner implements Runnable {

    private final LoadTestModel loadTestModel;
    private boolean stopped = false;

    private ApplicationRunner(final LoadTestModel loadTestModel) {
      this.loadTestModel = loadTestModel;
    }

    private void stop() {
      stopped = true;
    }

    /** {@inheritDoc} */
    public void run() {
      try {
        delayLogin();
        final Object application = loadTestModel.initializeApplication();
        LOG.debug("LoadTestModel initialized application: " + application);
        while (!stopped) {
          try {
            think();
            if (!loadTestModel.isPaused()) {
              final long currentTime = System.currentTimeMillis();
              String scenarioName = null;
              try {
                loadTestModel.counter.incrementWorkRequests();
                scenarioName = loadTestModel.performWork(application);
              }
              finally {
                final long workTime = System.currentTimeMillis() - currentTime;
                loadTestModel.counter.addScenarioDuration(scenarioName, (int) workTime);
                if (workTime > loadTestModel.getWarningTime()) {
                  loadTestModel.counter.incrementDelayedWorkRequests();
                }
              }
            }
          }
          catch (Exception e) {
            e.printStackTrace();
//            LOG.debug("Exception during during LoadTestModel.run() with application: " + application, e);
          }
        }
        loadTestModel.disconnectApplication(application);
      }
      catch (Exception e) {
        LOG.error("Exception while initializing application", e);
      }
    }

    /**
     * Simulates a user think pause by sleeping for a little while
     * @throws InterruptedException in case the sleep is interrupted
     */
    private void think() throws InterruptedException {
      Thread.sleep(loadTestModel.getThinkTime());
    }

    private void delayLogin() {
      try {
        final int sleepyTime = RANDOM.nextInt(loadTestModel.getMaximumThinkTime() *
                (loadTestModel.getLoginDelayFactor() <= 0 ? 1 : loadTestModel.getLoginDelayFactor()));
        Thread.sleep(sleepyTime);// delay login a bit so all do not try to login at the same time
      }
      catch (InterruptedException e) {
        LOG.error("Delay login sleep interrupted", e);
      }
    }
  }

  /**
   * Encapsulates a load test usage scenario.
   */
  public abstract static class UsageScenario {

    private final String name;
    private int successfulRunCount = 0;
    private int unsuccessfulRunCount = 0;

    /**
     * Instantiates a new UsageScenario using the simple class name as scenario name
     */
    public UsageScenario() {
      this.name = getClass().getSimpleName();
    }

    /**
     * Instantiates a new UsageScenario with the given name
     * @param name the scenario name
     */
    public UsageScenario(final String name) {
      this.name = name;
    }

    /**
     * @return the name of this scenario
     */
    public final String getName() {
      return this.name;
    }

    /**
     * @return the number of times this scenario has been successfully run
     */
    public final int getSuccessfulRunCount() {
      return successfulRunCount;
    }

    /**
     * @return the number of times this scenario has been unsuccessfully run
     */
    public final int getUnsuccessfulRunCount() {
      return unsuccessfulRunCount;
    }

    /**
     * @return the total number of times this scenario has been run
     */
    public final int getTotalRunCount() {
      return successfulRunCount + unsuccessfulRunCount;
    }

    /**
     * Resets the run counters
     */
    public final void resetRunCount() {
      successfulRunCount = 0;
      unsuccessfulRunCount = 0;
    }

    /**
     * @return the name of this scenario
     */
    @Override
    public final String toString() {
      return name;
    }

    /**
     * Runs this scenario with the given application
     * @param application the application to use
     * @throws ScenarioException in case of an exception
     */
    public final void run(final Object application) throws ScenarioException {
      if (application == null) {
        throw new IllegalArgumentException("Can not run without an application");
      }
      try {
        prepare(application);
        performScenario(application);
        successfulRunCount++;
      }
      catch (ScenarioException e) {
        unsuccessfulRunCount++;
        throw e;
      }
      finally {
        cleanup(application);
      }
    }

    /**
     * @return the default weight for this scenario, 1 by default
     */
    protected int getDefaultWeight() {
      return 1;
    }

    /**
     * Runs a set of actions on the given application.
     * @param application the application
     * @throws ScenarioException in case of an exception
     */
    protected abstract void performScenario(final Object application) throws ScenarioException;

    /**
     * Called before this scenario is run, override to prepare the application for each run
     * @param application the application
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void prepare(final Object application) {}

    /**
     * Called after this scenario has been run, override to cleanup the application after each run
     * @param application the application
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void cleanup(final Object application) {}
  }

  /**
   * An exception originating from a scenario run
   */
  public static final class ScenarioException extends Exception {

    /**
     * Instantiates a new ScenarioException.
     */
    public ScenarioException() {
      super();
    }

    /**
     * Instantiates a new ScenarioException.
     * @param cause the root cause
     */
    public ScenarioException(final Throwable cause) {
      super(cause);
    }
  }

  private static final class Counter {

    private final Collection<UsageScenario> usageScenarios;
    private final Map<String, Integer> usageScenarioRates = new HashMap<String, Integer>();
    private final Map<String, Integer> usageScenarioDurations = new HashMap<String, Integer>();
    private final Map<String, Collection<Integer>> scenarioDurations = new HashMap<String, Collection<Integer>>();

    private int workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

    private Counter(final Collection<UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    private int getWorkRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    private int getDelayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    private int getAverageScenarioDuration(final String scenarioName) {
      if (!usageScenarioDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioDurations.get(scenarioName);
    }

    private int getScenarioRate(final String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioRates.get(scenarioName);
    }

    private void addScenarioDuration(final String scenarioName, final int duration) {
      synchronized (scenarioDurations) {
        if (!scenarioDurations.containsKey(scenarioName)) {
          scenarioDurations.put(scenarioName, new ArrayList<Integer>());
        }
        scenarioDurations.get(scenarioName).add(duration);
      }
    }

    private void incrementWorkRequests() {
      workRequestCounter++;
    }

    private void incrementDelayedWorkRequests() {
      delayedWorkRequestCounter++;
    }

    private void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double elapsedSeconds = (current - time) / 1000d;
      if (elapsedSeconds > 5) {
        workRequestsPerSecond = (int) (workRequestCounter / (double) elapsedSeconds);
        delayedWorkRequestsPerSecond = (int) (delayedWorkRequestCounter / (double) elapsedSeconds);
        for (final UsageScenario scenario : usageScenarios) {
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / elapsedSeconds));
          int totalDuration = 0;
          synchronized (scenarioDurations) {
            final Collection<Integer> durations = scenarioDurations.get(scenario.getName());
            if (durations != null && durations.size() > 0) {
              for (final Integer duration : durations) {
                totalDuration += duration;
              }
              usageScenarioDurations.put(scenario.getName(), totalDuration / durations.size());
            }
          }
        }

        resetCounters();
        time = current;
      }
    }

    private void resetCounters() {
      for (final UsageScenario scenario : usageScenarios) {
        scenario.resetRunCount();
      }
      workRequestCounter = 0;
      delayedWorkRequestCounter = 0;
      synchronized (scenarioDurations) {
        scenarioDurations.clear();
      }
    }
  }
}
