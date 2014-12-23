/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A default LoadTest implementation.
 * @param <T> the type of the applications this load test uses
 */
public abstract class LoadTestModel<T> implements LoadTest {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;
  public static final int DEFAULT_WARNING_TIME_MS = 2000;

  protected static final Logger LOG = LoggerFactory.getLogger(LoadTestModel.class);

  protected static final Random RANDOM = new Random();

  private static final long NANO_IN_MILLI = 1000000;
  private static final double THOUSAND = 1000d;

  private final Event<Boolean> pausedChangedEvent = Events.event();
  private final Event<Boolean> collectChartDataChangedEvent = Events.event();
  private final Event<Integer> maximumThinkTimeChangedEvent = Events.event();
  private final Event<Integer> minimumThinkTimeChangedEvent = Events.event();
  private final Event<Integer> warningTimeChangedEvent = Events.event();
  private final Event<Integer> loginDelayFactorChangedEvent = Events.event();
  private final Event<Integer> applicationCountChangedEvent = Events.event();
  private final Event<Integer> applicationBatchSizeChangedEvent = Events.event();
  private final Event exitingDoneEvent = Events.event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int applicationBatchSize;
  private User user;

  private volatile boolean shuttingDown = false;
  private volatile boolean paused = false;
  private volatile boolean collectChartData = false;

  private final Stack<ApplicationRunner<T>> applications = new Stack<>();
  private final Collection<? extends UsageScenario<T>> usageScenarios;
  private final ItemRandomizer<UsageScenario> scenarioChooser;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Counter counter;
  private final TaskScheduler updateChartDataScheduler;
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

  private final XYSeries allocatedMemoryCollection = new XYSeries("Allocated");
  private final XYSeries usedMemoryCollection = new XYSeries("Used");
  private final XYSeries maxMemoryCollection = new XYSeries("Available");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();
  private final Collection<XYSeries> usageSeries = new ArrayList<>();
  private final Map<String, YIntervalSeries> durationSeries = new HashMap<>();
  private final Collection<XYSeries> failureSeries = new ArrayList<>();

  /**
   * Constructs a new LoadTestModel.
   * @param user the default user to use when initializing applications
   * @param maximumThinkTime the maximum think time, by default the minimum think time is max / 2
   * @param loginDelayFactor the value with which to multiply the think time when delaying login
   * @param applicationBatchSize the number of applications to add in a batch
   */
  public LoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor,
                       final int applicationBatchSize) {
    this(user, maximumThinkTime, loginDelayFactor, applicationBatchSize, DEFAULT_WARNING_TIME_MS);
  }

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
    this(user, new ArrayList<UsageScenario<T>>(), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
  }

  /**
   * Constructs a new LoadTestModel.
   * @param user the default user to use when initializing applications
   * @param usageScenarios the usage scenarios to use
   * @param maximumThinkTime the maximum think time, by default the minimum think time is max / 2
   * @param loginDelayFactor the value with which to multiply the think time when delaying login
   * @param applicationBatchSize the number of applications to add in a batch
   */
  public LoadTestModel(final User user, final Collection<? extends UsageScenario<T>> usageScenarios,
                       final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize) {
    this(user, usageScenarios, maximumThinkTime, loginDelayFactor, applicationBatchSize, DEFAULT_WARNING_TIME_MS);
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
  public LoadTestModel(final User user, final Collection<? extends UsageScenario<T>> usageScenarios,
                       final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                       final int warningTime) {
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
    this.updateChartDataScheduler = new TaskScheduler(new Runnable() {
      @Override
      public void run() {
        if (shuttingDown || paused) {
          return;
        }
        counter.updateRequestsPerSecond();
        if (collectChartData && !paused) {
          updateChartData();
        }
      }
    }, DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final void setUser(final User user) {
    this.user = user;
  }

  /** {@inheritDoc} */
  @Override
  public final UsageScenario<T> getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario<T> scenario : usageScenarios) {
      if (scenario.getName().equals(usageScenarioName)) {
        return scenario;
      }
    }

    throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<String> getUsageScenarios() {
    final Collection<String> ret = new ArrayList<>();
    for (final UsageScenario scenario : usageScenarios) {
      ret.add(scenario.getName());
    }

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public final void setWeight(final String scenarioName, final int weight) {
    scenarioChooser.setWeight(getUsageScenario(scenarioName), weight);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isScenarioEnabled(final String scenarioName) {
    return scenarioChooser.isItemEnabled(getUsageScenario(scenarioName));
  }

  /** {@inheritDoc} */
  @Override
  public final void setScenarioEnabled(final String scenarioName, final boolean value) {
    scenarioChooser.setItemEnabled(getUsageScenario(scenarioName), value);
  }

  /** {@inheritDoc} */
  @Override
  public final ItemRandomizer<UsageScenario> getScenarioChooser() {
    return scenarioChooser;
  }

  /** {@inheritDoc}
   * @param name*/
  @Override
  public final YIntervalSeriesCollection getScenarioDurationDataset(final String name) {
    final YIntervalSeriesCollection scenarioDurationCollection = new YIntervalSeriesCollection();
    scenarioDurationCollection.addSeries(durationSeries.get(name));

    return scenarioDurationCollection;
  }

  /** {@inheritDoc} */
  @Override
  public final XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  /** {@inheritDoc} */
  @Override
  public final XYSeriesCollection getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  /** {@inheritDoc} */
  @Override
  public final XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  /** {@inheritDoc} */
  @Override
  public final XYSeriesCollection getUsageScenarioFailureDataset() {
    return scenarioFailureCollection;
  }

  /** {@inheritDoc} */
  @Override
  public final XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /** {@inheritDoc} */
  @Override
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
    for (final XYSeries series : failureSeries) {
      series.clear();
    }
    for (final YIntervalSeries series : durationSeries.values()) {
      series.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final int getWarningTime() {
    return warningTime;
  }

  /** {@inheritDoc} */
  @Override
  public final void setWarningTime(final int warningTime) {
    if (warningTime <= 0) {
      throw new IllegalArgumentException("Warning time must be a positive integer");
    }

    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      warningTimeChangedEvent.fire(this.warningTime);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final int getUpdateInterval() {
    return updateChartDataScheduler.getInterval();
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpdateInterval(final int updateInterval) {
    updateChartDataScheduler.setInterval(updateInterval);
  }

  /** {@inheritDoc} */
  @Override
  public final int getApplicationCount() {
    return applications.size();
  }

  /** {@inheritDoc} */
  @Override
  public final int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  /** {@inheritDoc} */
  @Override
  public final void setApplicationBatchSize(final int applicationBatchSize) {
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }

    this.applicationBatchSize = applicationBatchSize;
    applicationBatchSizeChangedEvent.fire(this.applicationBatchSize);
  }

  /** {@inheritDoc} */
  @Override
  public final void addApplicationBatch() {
    for (int i = 0; i < applicationBatchSize; i++) {
      final ApplicationRunner<T> runner = new ApplicationRunner<>(this);
      synchronized (applications) {
        applications.push(runner);
      }
      applicationCountChangedEvent.fire();

      executor.execute(runner);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeApplicationBatch() {
    for (int i = 0; i < applicationBatchSize && !applications.isEmpty(); i++) {
      removeApplication();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isPaused() {
    return this.paused;
  }

  /** {@inheritDoc} */
  @Override
  public final void setPaused(final boolean value) {
    this.paused = value;
    pausedChangedEvent.fire(this.paused);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCollectChartData() {
    return collectChartData;
  }

  /** {@inheritDoc} */
  @Override
  public final void setCollectChartData(final boolean value) {
    this.collectChartData = value;
    collectChartDataChangedEvent.fire(this.collectChartData);
  }

  /** {@inheritDoc} */
  @Override
  public final void exit() {
    shuttingDown = true;
    updateChartDataScheduler.stop();
    executor.shutdown();
    paused = false;
    synchronized (applications) {
      while (!applications.isEmpty()) {
        removeApplication();
      }
    }
    while (!executor.isTerminated()) {
      try {
        Thread.sleep(maximumThinkTime);
      }
      catch (final InterruptedException ignored) {}
    }
    exitingDoneEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMaximumThinkTime(final int maximumThinkTime) {
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }

    this.maximumThinkTime = maximumThinkTime;
    maximumThinkTimeChangedEvent.fire(this.maximumThinkTime);
  }

  /** {@inheritDoc} */
  @Override
  public final int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMinimumThinkTime(final int minimumThinkTime) {
    if (minimumThinkTime < 0) {
      throw new IllegalArgumentException("Minimum think time must be a positive integer");
    }

    this.minimumThinkTime = minimumThinkTime;
    minimumThinkTimeChangedEvent.fire(this.minimumThinkTime);
  }

  /** {@inheritDoc} */
  @Override
  public final int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }

    this.loginDelayFactor = loginDelayFactor;
    loginDelayFactorChangedEvent.fire(this.loginDelayFactor);
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> applicationBatchSizeObserver() {
    return applicationBatchSizeChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> applicationCountObserver() {
    return applicationCountChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> maximumThinkTimeObserver() {
    return maximumThinkTimeChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> getMinimumThinkTimeObserver() {
    return minimumThinkTimeChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Boolean> getPauseObserver() {
    return pausedChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Boolean> collectChartDataObserver() {
    return collectChartDataChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> getWarningTimeObserver() {
    return warningTimeChangedEvent.getObserver();
  }

  /**
   * Runs the scenario with the given name on the given application
   * @param usageScenarioName the name of the scenario to run
   * @param application the application to use
   * @throws ScenarioException in case of an exception
   */
  protected final void runScenario(final String usageScenarioName, final T application) throws ScenarioException {
    getUsageScenario(usageScenarioName).run(application);
  }

  /**
   * @param listener a listener notified when this load test model has finished removing all applications
   */
  protected void addExitListener(final EventListener listener) {
    exitingDoneEvent.addListener(listener);
  }

  /**
   * @return an initialized application.
   * @throws org.jminor.common.model.CancelException in case the initialization was cancelled
   */
  protected abstract T initializeApplication();

  /**
   * @param application the application to disconnect
   */
  protected abstract void disconnectApplication(final T application);

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
   * Selects a random scenario and runs it with the given application
   * @param application the application for running the next scenario
   * @return the name of the scenario that was run
   * @throws ScenarioException any exception thrown by the work request
   */
  private String performWork(final T application) throws ScenarioException {
    Util.rejectNullValue(application, "application");
    final String scenarioName = scenarioChooser.getRandomItem().getName();
    runScenario(scenarioName, application);

    return scenarioName;
  }

  /**
   * Removes a single application
   */
  private void removeApplication() {
    synchronized (applications) {
      applications.pop().stop();
    }
    applicationCountChangedEvent.fire(applications.size());
  }

  private ItemRandomizer<UsageScenario> initializeScenarioChooser() {
    final ItemRandomizer<UsageScenario> model = new ItemRandomizerModel<>();
    for (final UsageScenario scenario : this.usageScenarios) {
      model.addItem(scenario, scenario.getDefaultWeight());
    }

    return model;
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
    allocatedMemoryCollection.add(time, Util.getAllocatedMemory() / THOUSAND);
    usedMemoryCollection.add(time, Util.getUsedMemory() / THOUSAND);
    maxMemoryCollection.add(time, Util.getMaxMemory() / THOUSAND);
    scenariosRunSeries.add(time, counter.getWorkRequestsPerSecond());
    warningTimeSeries.add(time, warningTime);
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

  private static final class ApplicationRunner<T> implements Runnable {

    private final LoadTestModel<T> loadTestModel;
    private boolean stopped = false;

    private ApplicationRunner(final LoadTestModel<T> loadTestModel) {
      this.loadTestModel = loadTestModel;
    }

    private void stop() {
      stopped = true;
    }

    @Override
    public void run() {
      try {
        if (loadTestModel.getLoginDelayFactor() > 0) {
          delayLogin();
        }
        T application = null;
        while (!stopped) {
          try {
            if (application == null) {
              application = loadTestModel.initializeApplication();
              LOG.debug("LoadTestModel initialized application: {}", application);
            }
            think();
            if (!loadTestModel.isPaused()) {
              final long currentTimeNano = System.nanoTime();
              String scenarioName = null;
              try {
                loadTestModel.counter.incrementWorkRequests();
                try {
                  scenarioName = loadTestModel.performWork(application);
                }
                catch (final ScenarioException ignored) {}
              }
              finally {
                final long workTimeMillis = (System.nanoTime() - currentTimeNano) / NANO_IN_MILLI;
                if (scenarioName != null) {
                  loadTestModel.counter.addScenarioDuration(scenarioName, (int) workTimeMillis);
                }
                if (workTimeMillis > loadTestModel.getWarningTime()) {
                  loadTestModel.counter.incrementDelayedWorkRequests();
                }
              }
            }
          }
          catch (final Exception e) {
            final String message = "Exception during " + (application == null ? "application initialization" : ("run " + application));
            LOG.debug(message, e);
          }
        }
        if (application != null) {
          loadTestModel.disconnectApplication(application);
          LOG.debug("LoadTestModel disconnected application: {}", application);
        }
      }
      catch (final Exception e) {
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
      catch (final InterruptedException e) {
        LOG.error("Delay login sleep interrupted", e);
      }
    }
  }

  /**
   * An abstract usage scenario.
   */
  public abstract static class AbstractUsageScenario<T> implements LoadTest.UsageScenario<T> {

    private final String name;
    private volatile int successfulRunCount = 0;
    private volatile int unsuccessfulRunCount = 0;
    private final List<ScenarioException> exceptions = new ArrayList<>();

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
    @Override
    public final String getName() {
      return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public final int getSuccessfulRunCount() {
      return successfulRunCount;
    }

    /** {@inheritDoc} */
    @Override
    public final int getUnsuccessfulRunCount() {
      return unsuccessfulRunCount;
    }

    /** {@inheritDoc} */
    @Override
    public final int getTotalRunCount() {
      return successfulRunCount + unsuccessfulRunCount;
    }

    /** {@inheritDoc} */
    @Override
    public final List<ScenarioException> getExceptions() {
      synchronized (exceptions) {
        return new ArrayList<>(exceptions);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void resetRunCount() {
      successfulRunCount = 0;
      unsuccessfulRunCount = 0;
    }

    /** {@inheritDoc} */
    @Override
    public final void clearExceptions() {
      synchronized (exceptions) {
        exceptions.clear();
      }
    }

    /**
     * @return the name of this scenario
     */
    @Override
    public final String toString() {
      return name;
    }

    /** {@inheritDoc} */
    @Override
    public final void run(final T application) throws ScenarioException {
      if (application == null) {
        throw new IllegalArgumentException("Can not run without an application");
      }
      try {
        prepare(application);
        performScenario(application);
        successfulRunCount++;
      }
      catch (final ScenarioException e) {
        unsuccessfulRunCount++;
        synchronized (exceptions) {
          exceptions.add(e);
        }
        throw e;
      }
      finally {
        cleanup(application);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
      return name.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object obj) {
      return obj instanceof UsageScenario && ((UsageScenario) obj).getName().equals(name);
    }

    /** {@inheritDoc} */
    @Override
    public int getDefaultWeight() {
      return 1;
    }

    /**
     * Runs a set of actions on the given application.
     * @param application the application
     * @throws ScenarioException in case of an exception
     */
    protected abstract void performScenario(final T application) throws ScenarioException;

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

    private static final int UPDATE_INTERVAL = 5;

    private final Collection<? extends UsageScenario> usageScenarios;
    private final Map<String, Integer> usageScenarioRates = new HashMap<>();
    private final Map<String, Integer> usageScenarioAvgDurations = new HashMap<>();
    private final Map<String, Integer> usageScenarioMaxDurations = new HashMap<>();
    private final Map<String, Integer> usageScenarioMinDurations = new HashMap<>();
    private final Map<String, Double> usageScenarioFailures = new HashMap<>();
    private final Map<String, Collection<Integer>> scenarioDurations = new HashMap<>();

    private double workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

    private Counter(final Collection<? extends UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    private double getWorkRequestsPerSecond() {
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
      final double elapsedSeconds = (current - time) / THOUSAND;
      if (elapsedSeconds > UPDATE_INTERVAL) {
        usageScenarioAvgDurations.clear();
        usageScenarioMinDurations.clear();
        usageScenarioMaxDurations.clear();
        workRequestsPerSecond = workRequestCounter / elapsedSeconds;
        delayedWorkRequestsPerSecond = (int) (delayedWorkRequestCounter / elapsedSeconds);
        for (final UsageScenario scenario : usageScenarios) {
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / elapsedSeconds));
          final double failures = scenario.getUnsuccessfulRunCount();
          usageScenarioFailures.put(scenario.getName(), failures);
          synchronized (scenarioDurations) {
            final Collection<Integer> durations = scenarioDurations.get(scenario.getName());
            if (!Util.nullOrEmpty(durations)) {
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
