/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

  public static final int DEFAULT_UPDATE_INTERVAL = 2000;

  protected static final Logger LOG = Util.getLogger(LoadTestModel.class);

  protected static final Random RANDOM = new Random();

  private final Event evtPausedChanged = new Event();
  private final Event evtCollectChartDataChanged = new Event();
  private final Event evtMaximumThinkTimeChanged = new Event();
  private final Event evtMinimumThinkTimeChanged = new Event();
  private final Event evtWarningTimeChanged = new Event();
  private final Event evtLoginDelayFactorChanged = new Event();
  private final Event evtApplicationtCountChanged = new Event();
  private final Event evtApplicationBatchSizeChanged = new Event();
  private final Event evtDoneExiting = new Event();
  private final Event evtUpdateIntervalChanged = new Event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int applicationBatchSize;
  private int updateInterval;

  private boolean paused = false;
  private boolean stopped = false;
  private boolean collectChartData = false;

  private final Stack<Object> applications = new Stack<Object>();
  private final Collection<UsageScenario> usageScenarios;
  private final RandomItemModel<UsageScenario> scenarioChooser;
  private User user;

  private final Counter counter;
  private Timer updateTimer;
  private volatile int warningTime;

  private final XYSeries workRequestsSeries = new XYSeries("Scenarios run per second");
  private final XYSeries delayedWorkRequestsSeries = new XYSeries("Delayed scenarios per second");
  private final XYSeriesCollection workRequestsCollection = new XYSeriesCollection();

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
    initializeContext();
    setUpdateInterval(DEFAULT_UPDATE_INTERVAL);
  }

  public final User getUser() {
    return user;
  }

  public final void setUser(final User user) {
    this.user = user;
  }

  public final void performGC() {
    System.gc();
  }

  public final RandomItemModel<UsageScenario> getScenarioChooser() {
    return scenarioChooser;
  }

  public final Collection<UsageScenario> getUsageScenarios() {
    return usageScenarios;
  }

  public final UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios) {
      if (scenario.getName().equals(usageScenarioName)) {
        return scenario;
      }
    }

    throw new RuntimeException("UsageScenario not found: " + usageScenarioName);
  }

  public final XYSeriesCollection getWorkRequestsDataset() {
    return workRequestsCollection;
  }

  public final XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  public final XYSeriesCollection getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  public final XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

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
  }

  public final int getWarningTime() {
    return warningTime;
  }

  public final void setWarningTime(final int warningTime) {
    if (warningTime <= 0) {
      throw new IllegalArgumentException("Warning time must be a positive integer");
    }

    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  public final int getUpdateInterval() {
    return updateInterval;
  }

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

  public final int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  public final void setApplicationBatchSize(final int applicationBatchSize) {
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }

    this.applicationBatchSize = applicationBatchSize;
    evtApplicationBatchSizeChanged.fire();
  }

  public final void addApplicationBatch() throws Exception {
    for (int i = 0; i < applicationBatchSize; i++) {
      addApplication();
    }
  }

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

  public final boolean isCollectChartData() {
    return collectChartData;
  }

  public final void setCollectChartData(final boolean value) {
    this.collectChartData = value;
    evtCollectChartDataChanged.fire();
  }

  public final void exit() {
    paused = false;
    stopped = true;
    synchronized (applications) {
      while (!applications.isEmpty()) {
        removeApplication();
      }
    }
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

  public final int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  public final void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }

    this.loginDelayFactor = loginDelayFactor;
    evtLoginDelayFactorChanged.fire();
  }

  public final void addExitListener(final ActionListener listener) {
    evtDoneExiting.addListener(listener);
  }

  public final EventObserver applicationBatchSizeObserver() {
    return evtApplicationBatchSizeChanged;
  }

  public final EventObserver applicationCountObserver() {
    return evtApplicationtCountChanged;
  }

  public final EventObserver maximumThinkTimeObserver() {
    return evtMaximumThinkTimeChanged;
  }

  public final EventObserver minimumThinkTimeObserver() {
    return evtMinimumThinkTimeChanged;
  }

  public final EventObserver pauseObserver() {
    return evtPausedChanged;
  }

  public final EventObserver collectChartDataObserver() {
    return evtCollectChartDataChanged;
  }

  public final EventObserver warningTimeObserver() {
    return evtWarningTimeChanged;
  }

  protected void performWork(final Object application) {
    Util.rejectNullValue(application, "application");
    final String scenarioName = scenarioChooser.getRandomItem().getName();
    try {
      runScenario(scenarioName, application);
    }
    catch (Exception e) {
      LOG.debug("Exception while running scenario: " + scenarioName + " with application: " + application, e);
    }
  }

  protected void runScenario(final String usageScenarioName, final Object application) throws Exception {
    getUsageScenario(usageScenarioName).run(application);
  }

  protected abstract Object initializeApplication() throws CancelException;

  protected abstract void disconnectApplication(final Object application);

  protected void initializeContext() {/**/}

  /**
   * Simulates a user think pause by sleeping for a little while
   * @throws InterruptedException in case the sleep is interrupted
   * @see #getThinkTime()
   */
  protected final void think() throws InterruptedException {
    Thread.sleep(getThinkTime());
  }

  protected final int getThinkTime() {
    final int time = minimumThinkTime - maximumThinkTime;
    return time > 0 ? RANDOM.nextInt(time) + minimumThinkTime : minimumThinkTime;
  }

  private synchronized void addApplication() {
    final Runnable applicationRunner = new Runnable() {
      public void run() {
        try {
          delayLogin();
          final Object application = initializeApplication();
          LOG.debug("LoadTestModel initialized application: " + application);
          applications.push(application);
          evtApplicationtCountChanged.fire();
          while (applications.contains(application)) {
            try {
              think();
              if (!isPaused() && (applications.contains(application))) {
                final long currentTime = System.currentTimeMillis();
                try {
                  counter.incrementWorkRequests();
                  performWork(application);
                }
                finally {
                  final long workTime = System.currentTimeMillis() - currentTime;
                  if (workTime > warningTime) {
                    counter.incrementDelayedWorkRequests();
                  }
                }
              }
            }
            catch (Exception e) {
              LOG.debug("Exception during during LoadTestModel.run() with application: " + application, e);
            }
          }
          disconnectApplication(application);
        }
        catch (Exception e) {
          LOG.error("Exception while initializing application", e);
        }
      }
    };
    new Thread(applicationRunner).start();
  }

  private synchronized void removeApplication() {
    applications.pop();
    evtApplicationtCountChanged.fire();
  }

  private void delayLogin() {
    try {
      final int sleepyTime = RANDOM.nextInt(maximumThinkTime * (loginDelayFactor <= 0 ? 1 : loginDelayFactor));
      System.out.println("AppModel delaying login for " + sleepyTime + " ms");
      Thread.sleep(sleepyTime);// delay login a bit so all do not try to login at the same time
    }
    catch (InterruptedException e) {
      LOG.error("Delay login sleep interrupted", e);
    }
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
      @Override
      public void run() {
        counter.updateRequestsPerSecond();
        if (collectChartData && !paused) {
          updateChartData();
        }
        if (stopped && applications.isEmpty()) {
          evtDoneExiting.fire();
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
    }
  }

  private void updateChartData() {
    final long time = System.currentTimeMillis();
    workRequestsSeries.add(time, counter.getWorkRequestsPerSecond());
    delayedWorkRequestsSeries.add(time, counter.getDelayedWorkRequestsPerSecond());
    minimumThinkTimeSeries.add(time, minimumThinkTime);
    maximumThinkTimeSeries.add(time, maximumThinkTime);
    numberOfApplicationsSeries.add(time, applications.size());
    allocatedMemoryCollection.add(time, Util.getAllocatedMemory());
    usedMemoryCollection.add(time, Util.getUsedMemory());
    maxMemoryCollection.add(time, Util.getMaxMemory());
    for (final Object object : usageScenarioCollection.getSeries()) {
      final XYSeries series = (XYSeries) object;
      series.add(time, counter.getScenarioRate((String) series.getKey()));
    }
  }

  /**
   * Encapsulates a load test usage scenario.
   */
  public abstract static class UsageScenario {

    private final String name;
    private int successfulRunCount = 0;
    private int unsuccessfulRunCount = 0;

    public UsageScenario() {
      this.name = getClass().getSimpleName();
    }

    public UsageScenario(final String name) {
      this.name = name;
    }

    public final String getName() {
      return this.name;
    }

    public final int getSuccessfulRunCount() {
      return successfulRunCount;
    }

    public final int getUnsuccessfulRunCount() {
      return unsuccessfulRunCount;
    }

    public final int getTotalRunCount() {
      return successfulRunCount + unsuccessfulRunCount;
    }

    public final void resetRunCount() {
      successfulRunCount = 0;
      unsuccessfulRunCount = 0;
    }

    @Override
    public final String toString() {
      return name;
    }

    public final void run(final Object application) throws Exception {
      if (application == null) {
        throw new RuntimeException("Can not run without an application");
      }
      try {
        prepare(application);
        performScenario(application);
        successfulRunCount++;
      }
      catch (Exception e) {
        unsuccessfulRunCount++;
        throw e;
      }
      finally {
        cleanup(application);
      }
    }

    protected int getDefaultWeight() {
      return 1;
    }

    /**
     * Runs a set of actions on the given application.
     * @param application the application
     * @throws Exception in case of an exception
     */
    protected abstract void performScenario(final Object application) throws Exception;

    @SuppressWarnings({"UnusedDeclaration"})
    protected void prepare(final Object application) {}

    @SuppressWarnings({"UnusedDeclaration"})
    protected void cleanup(final Object application) {}
  }

  private static final class Counter {

    private final Collection<UsageScenario> usageScenarios;
    private final Map<String, Integer> usageScenarioRates = new HashMap<String, Integer>();

    private int workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

    Counter(final Collection<UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    public int getWorkRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    public int getDelayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    public int getScenarioRate(final String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioRates.get(scenarioName);
    }

    public void incrementWorkRequests() {
      workRequestCounter++;
    }

    public void incrementDelayedWorkRequests() {
      delayedWorkRequestCounter++;
    }

    public void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - time) / 1000d;
      if (seconds > 5) {
        workRequestsPerSecond = (int) (workRequestCounter / (double) seconds);
        delayedWorkRequestsPerSecond = (int) (delayedWorkRequestCounter / (double) seconds);
        for (final UsageScenario scenario : usageScenarios) {
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / seconds));
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
    }
  }
}
