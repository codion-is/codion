/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A default LoadTest implementation.
 */
public abstract class LoadTestModel implements LoadTest {

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
  private final Collection<? extends UsageScenario> usageScenarios;
  private final ItemRandomizer<UsageScenario> scenarioChooser;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private User user;

  private final Counter counter;
  private Timer updateTimer;
  private volatile int warningTime;

  private final XYSeries scenariosRunSeries = new XYSeries("Total");
  private final XYSeries delayedScenarioRunsSeries = new XYSeries("Warn. time exceeded");

  private final XYSeriesCollection scenarioFailureCollection = new XYSeriesCollection();

  private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
  private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
  private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

  private final XYSeries numberOfApplicationsSeries = new XYSeries("Application count");
  private final XYSeriesCollection numberOfApplicationsCollection = new XYSeriesCollection();

  private final XYSeries warningTimeSeries = new XYSeries("Warn. limit");
  private final XYSeriesCollection usageScenarioCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemoryCollection = new XYSeries("Allocated memory");
  private final XYSeries usedMemoryCollection = new XYSeries("Used memory");
  private final XYSeries maxMemoryCollection = new XYSeries("Maximum memory");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();
  private final Collection<XYSeries> usageSeries = new ArrayList<XYSeries>();
  private final Map<String, YIntervalSeries> durationSeries = new HashMap<String, YIntervalSeries>();
  private final Collection<XYSeries> failureSeries = new ArrayList<XYSeries>();

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
  public LoadTestModel(final User user, final Collection<? extends UsageScenario> usageScenarios, final int maximumThinkTime,
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

  /** {@inheritDoc} */
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  public final void setUser(final User user) {
    this.user = user;
  }

  /** {@inheritDoc} */
  public UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios) {
      if (scenario.getName().equals(usageScenarioName)) {
        return scenario;
      }
    }

    throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
  }

  /** {@inheritDoc} */
  public Collection<String> getUsageScenarios() {
    final Collection<String> ret = new ArrayList<String>();
    for (final UsageScenario scenario : usageScenarios) {
      ret.add(scenario.getName());
    }

    return ret;
  }

  public void setWeight(String scenarioName, int weight) {
    scenarioChooser.setWeight(getUsageScenario(scenarioName), weight);
  }

  public ItemRandomizer<UsageScenario> getScenarioChooser() {
    return scenarioChooser;
  }

  /** {@inheritDoc}
   * @param name*/
  public final YIntervalSeriesCollection getScenarioDurationDataset(final String name) {
  final YIntervalSeriesCollection scenarioDurationCollection = new YIntervalSeriesCollection();
    scenarioDurationCollection.addSeries(durationSeries.get(name));

    return scenarioDurationCollection;
  }

  /** {@inheritDoc} */
  public final XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  /** {@inheritDoc} */
  public final XYSeriesCollection getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  /** {@inheritDoc} */
  public final XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  /** {@inheritDoc} */
  public final XYSeriesCollection getUsageScenarioFailureDataset() {
    return scenarioFailureCollection;
  }

  /** {@inheritDoc} */
  public final XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /** {@inheritDoc} */
  public final void resetChartData() {
    scenariosRunSeries.clear();
    delayedScenarioRunsSeries.clear();
    minimumThinkTimeSeries.clear();
    maximumThinkTimeSeries.clear();
    numberOfApplicationsSeries.clear();
    allocatedMemoryCollection.clear();
    usedMemoryCollection.clear();
    maxMemoryCollection.clear();
    for (final XYSeries series : usageSeries) {
      series.clear();
    }
    for (final YIntervalSeries series : durationSeries.values()) {
      series.clear();
    }
  }

  /** {@inheritDoc} */
  public final int getWarningTime() {
    return warningTime;
  }

  /** {@inheritDoc} */
  public final void setWarningTime(final int warningTime) {
    if (warningTime <= 0) {
      throw new IllegalArgumentException("Warning time must be a positive integer");
    }

    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  /** {@inheritDoc} */
  public final int getUpdateInterval() {
    return updateInterval;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public final int getApplicationCount() {
    return applications.size();
  }

  /** {@inheritDoc} */
  public final int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  /** {@inheritDoc} */
  public final void setApplicationBatchSize(final int applicationBatchSize) {
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }

    this.applicationBatchSize = applicationBatchSize;
    evtApplicationBatchSizeChanged.fire();
  }

  /** {@inheritDoc} */
  public final void addApplicationBatch() {
    for (int i = 0; i < applicationBatchSize; i++) {
      final ApplicationRunner runner = new ApplicationRunner(this);
      synchronized (applications) {
        applications.push(runner);
      }
      evtApplicationtCountChanged.fire();

      executor.execute(runner);
    }
  }

  /** {@inheritDoc} */
  public final void removeApplicationBatch() {
    for (int i = 0; i < applicationBatchSize && !applications.isEmpty(); i++) {
      removeApplication();
    }
  }

  /** {@inheritDoc} */
  public final boolean isPaused() {
    return this.paused;
  }

  /** {@inheritDoc} */
  public final void setPaused(final boolean value) {
    this.paused = value;
    evtPausedChanged.fire();
  }

  /** {@inheritDoc} */
  public final boolean isCollectChartData() {
    return collectChartData;
  }

  /** {@inheritDoc} */
  public final void setCollectChartData(final boolean value) {
    this.collectChartData = value;
    evtCollectChartDataChanged.fire();
  }

  /** {@inheritDoc} */
  public final void exit() {
    executor.shutdownNow();
    paused = false;
    synchronized (applications) {
      while (!applications.isEmpty()) {
        removeApplication();
      }
    }
    evtDoneExiting.fire();
  }

  /** {@inheritDoc} */
  public final int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /** {@inheritDoc} */
  public final void setMaximumThinkTime(final int maximumThinkTime) {
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }

    this.maximumThinkTime = maximumThinkTime;
    evtMaximumThinkTimeChanged.fire();
  }

  /** {@inheritDoc} */
  public final int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /** {@inheritDoc} */
  public final void setMinimumThinkTime(final int minimumThinkTime) {
    if (minimumThinkTime < 0) {
      throw new IllegalArgumentException("Minimum think time must be a positive integer");
    }

    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  /** {@inheritDoc} */
  public final int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  /** {@inheritDoc} */
  public final void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }

    this.loginDelayFactor = loginDelayFactor;
    evtLoginDelayFactorChanged.fire();
  }

  /** {@inheritDoc} */
  public final void addExitListener(final ActionListener listener) {
    evtDoneExiting.addListener(listener);
  }

  /** {@inheritDoc} */
  public final EventObserver applicationBatchSizeObserver() {
    return evtApplicationBatchSizeChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver applicationCountObserver() {
    return evtApplicationtCountChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver maximumThinkTimeObserver() {
    return evtMaximumThinkTimeChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver getMinimumThinkTimeObserver() {
    return evtMinimumThinkTimeChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver getPauseObserver() {
    return evtPausedChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver collectChartDataObserver() {
    return evtCollectChartDataChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver getWarningTimeObserver() {
    return evtWarningTimeChanged.getObserver();
  }

  /**
   * Selects a random scenario and runs it with the given application
   * @param application the application for running the next scenario
   * @return the name of the scenario that was run
   * @throws ScenarioException any exception thrown by the work request
   */
  protected String performWork(final Object application) throws ScenarioException {
    Util.rejectNullValue(application, "application");
    final String scenarioName = scenarioChooser.getRandomItem().getName();
    runScenario(scenarioName, application);

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

  private ItemRandomizer<UsageScenario> initializeScenarioChooser() {
    final ItemRandomizer<UsageScenario> model = new ItemRandomizerModel<UsageScenario>();
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
    thinkTimeCollection.addSeries(minimumThinkTimeSeries);
    thinkTimeCollection.addSeries(maximumThinkTimeSeries);
    numberOfApplicationsCollection.addSeries(numberOfApplicationsSeries);
    memoryUsageCollection.addSeries(maxMemoryCollection);
    memoryUsageCollection.addSeries(allocatedMemoryCollection);
    memoryUsageCollection.addSeries(usedMemoryCollection);
    usageScenarioCollection.addSeries(scenariosRunSeries);
    for (final UsageScenario usageScenario : this.usageScenarios) {
      final XYSeries series = new XYSeries(usageScenario.getName());
      usageScenarioCollection.addSeries(series);
      usageSeries.add(series);
      final YIntervalSeries avgDurSeries = new YIntervalSeries(usageScenario.getName());
      durationSeries.put(usageScenario.getName(), avgDurSeries);
      final XYSeries failSeries = new XYSeries(usageScenario.getName());
      scenarioFailureCollection.addSeries(failSeries);
      failureSeries.add(failSeries);
    }
    usageScenarioCollection.addSeries(delayedScenarioRunsSeries);
  }

  private void updateChartData() {
    final long time = System.currentTimeMillis();
    delayedScenarioRunsSeries.add(time, counter.getDelayedWorkRequestsPerSecond());
    minimumThinkTimeSeries.add(time, minimumThinkTime);
    maximumThinkTimeSeries.add(time, maximumThinkTime);
    numberOfApplicationsSeries.add(time, applications.size());
    allocatedMemoryCollection.add(time, Util.getAllocatedMemory() / 1000);
    usedMemoryCollection.add(time, Util.getUsedMemory() / 1000);
    maxMemoryCollection.add(time, Util.getMaxMemory() / 1000);
    scenariosRunSeries.add(time, counter.getWorkRequestsPerSecond());
    warningTimeSeries.add(time, getWarningTime());
    for (final XYSeries series : usageSeries) {
      series.add(time, counter.getScenarioRate((String) series.getKey()));
    }
    for (final YIntervalSeries series : durationSeries.values()) {
      final String scenario = (String) series.getKey();
      series.add(time, counter.getAverageScenarioDuration(scenario),
              counter.getMinimumScenarioDuration(scenario), counter.getMaximumScenarioDuration(scenario));
    }
    for (final XYSeries series : failureSeries) {
      series.add(time, counter.getScenarioFailureRate((String) series.getKey()));
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
                try {
                  scenarioName = loadTestModel.performWork(application);
                }
                catch (ScenarioException e) {
                  e.printStackTrace();
                  LOG.debug("ScenarioException: " + scenarioName, e.getCause());
                }
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
            LOG.debug("Exception during during LoadTestModel.run() with application: " + application, e);
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
   * An abstract usage scenario.
   */
  public abstract static class AbstractUsageScenario implements LoadTest.UsageScenario {

    private final String name;
    private int successfulRunCount = 0;
    private int unsuccessfulRunCount = 0;

    /**
     * Instantiates a new UsageScenario using the simple class name as scenario name
     */
    public AbstractUsageScenario() {
      this.name = getClass().getSimpleName();
    }

    /**
     * Instantiates a new UsageScenario with the given name
     * @param name the scenario name
     */
    public AbstractUsageScenario(final String name) {
      this.name = name;
    }

    /** {@inheritDoc} */
    public final String getName() {
      return this.name;
    }

    /** {@inheritDoc} */
    public final int getSuccessfulRunCount() {
      return successfulRunCount;
    }

    /** {@inheritDoc} */
    public final int getUnsuccessfulRunCount() {
      return unsuccessfulRunCount;
    }

    /** {@inheritDoc} */
    public final int getTotalRunCount() {
      return successfulRunCount + unsuccessfulRunCount;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    @Override
    public int hashCode() {
      return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof UsageScenario && ((UsageScenario) obj).getName().equals(getName());
    }

    /** {@inheritDoc} */
    public int getDefaultWeight() {
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

  private static final class Counter {

    private final Collection<? extends UsageScenario> usageScenarios;
    private final Map<String, Integer> usageScenarioRates = new HashMap<String, Integer>();
    private final Map<String, Integer> usageScenarioAvgDurations = new HashMap<String, Integer>();
    private final Map<String, Integer> usageScenarioMaxDurations = new HashMap<String, Integer>();
    private final Map<String, Integer> usageScenarioMinDurations = new HashMap<String, Integer>();
    private final Map<String, Double> usageScenarioFailures = new HashMap<String, Double>();
    private final Map<String, Collection<Integer>> scenarioDurations = new HashMap<String, Collection<Integer>>();

    private int workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

    private Counter(final Collection<? extends UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    private int getWorkRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    private int getDelayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    private int getMinimumScenarioDuration(final String scenarioName) {
      if (!usageScenarioMinDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioMinDurations.get(scenarioName);
    }

    private int getMaximumScenarioDuration(final String scenarioName) {
      if (!usageScenarioMaxDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioMaxDurations.get(scenarioName);
    }

    private int getAverageScenarioDuration(final String scenarioName) {
      if (!usageScenarioAvgDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioAvgDurations.get(scenarioName);
    }

    private double getScenarioFailureRate(final String scenarioName) {
      if (!usageScenarioFailures.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioFailures.get(scenarioName);
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
          final double failures = scenario.getUnsuccessfulRunCount();
          usageScenarioFailures.put(scenario.getName(), failures);
          synchronized (scenarioDurations) {
            final Collection<Integer> durations = scenarioDurations.get(scenario.getName());
            if (durations != null && durations.size() > 0) {
              int totalDuration = 0;
              int minDuration = -1;
              int maxDuration = -1;
              for (final Integer duration : durations) {
                totalDuration += duration;
                if (minDuration == -1) {
                  minDuration = duration;
                  maxDuration = duration;
                }
                else {
                  minDuration = Math.min(minDuration, duration);
                  maxDuration = Math.max(maxDuration, duration);
                }
              }
              usageScenarioAvgDurations.put(scenario.getName(), totalDuration / durations.size());
              usageScenarioMinDurations.put(scenario.getName(), minDuration);
              usageScenarioMaxDurations.put(scenario.getName(), maxDuration);
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
