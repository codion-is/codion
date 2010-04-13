/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

  protected static final Random random = new Random();

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
  private final RandomItemModel scenarioChooser;
  private User user;

  private final Counter counter;
  private Timer updateTimer;
  private int warningTime;

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
  public LoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                       final int warningTime) {
    if (maximumThinkTime <= 0)
      throw new IllegalArgumentException("Maximum think time must be a positive integer");

    this.user = user;
    this.maximumThinkTime = maximumThinkTime;
    this.minimumThinkTime = maximumThinkTime / 2;
    this.loginDelayFactor = loginDelayFactor;
    this.applicationBatchSize = applicationBatchSize;
    this.warningTime = warningTime;
    this.usageScenarios = initializeUsageScenarios();
    this.scenarioChooser = initializeScenarioChooser();
    this.counter = new Counter(this.usageScenarios);
    initializeChartData();
    initializeContext();
    setUpdateInterval(2000);
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public void performGC() {
    System.gc();
  }

  public RandomItemModel getScenarioChooser() {
    return scenarioChooser;
  }

  public Collection<UsageScenario> getUsageScenarios() {
    return usageScenarios;
  }

  public UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios)
      if (scenario.getName().equals(usageScenarioName))
        return scenario;

    throw new RuntimeException("UsageScenario not found: " + usageScenarioName);
  }

  public XYSeriesCollection getWorkRequestsDataset() {
    return workRequestsCollection;
  }

  public XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  public XYSeriesCollection getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  public XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  public XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /**
   * Resets the accumulated chart data
   */
  public void resetChartData() {
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

  public int getWarningTime() {
    return warningTime;
  }

  public void setWarningTime(int warningTime) {
    if (warningTime <= 0)
      throw new IllegalArgumentException("Warning time must be a positive integer");

    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  public int getUpdateInterval() {
    return updateInterval;
  }

  public void setUpdateInterval(final int updateInterval) {
    if (updateInterval < 0)
      throw new IllegalArgumentException("Update interval must be a positive integer");

    if (this.updateInterval != updateInterval) {
      this.updateInterval = updateInterval;
      scheduleUpdateTime(updateInterval);
      evtUpdateIntervalChanged.fire();
    }
  }

  /**
   * @return the number of active applications
   */
  public int getApplicationCount() {
    return applications.size();
  }

  public int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  public void setApplicationBatchSize(int applicationBatchSize) {
    if (applicationBatchSize <= 0)
      throw new IllegalArgumentException("Application batch size must be a positive integer");

    this.applicationBatchSize = applicationBatchSize;
    evtApplicationBatchSizeChanged.fire();
  }

  public void addApplications() throws Exception {
    for (int i = 0; i < applicationBatchSize; i++)
      addApplication();
  }

  public void removeApplications() throws Exception {
    for (int i = 0; i < applicationBatchSize && applications.size() > 0; i++)
      removeApplication();
  }

  /**
   * @return true if the load testing is paused
   */
  public boolean isPaused() {
    return this.paused;
  }

  /**
   * @param value true if load testing should be paused
   */
  public void setPaused(final boolean value) {
    this.paused = value;
    evtPausedChanged.fire();
  }

  public boolean isCollectChartData() {
    return collectChartData;
  }

  public void setCollectChartData(final boolean value) {
    this.collectChartData = value;
    evtCollectChartDataChanged.fire();
  }

  public void exit() {
    paused = false;
    stopped = true;
    synchronized (applications) {
      while (applications.size() > 0)
        removeApplication();
    }
  }

  /**
   * @return the maximum number of milliseconds that should pass between work requests
   */
  public int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   */
  public void setMaximumThinkTime(int maximumThinkTime) {
    if (maximumThinkTime <= 0)
      throw new IllegalArgumentException("Maximum think time must be a positive integer");

    this.maximumThinkTime = maximumThinkTime;
    evtMaximumThinkTimeChanged.fire();
  }

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   */
  public int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   */
  public void setMinimumThinkTime(int minimumThinkTime) {
    if (minimumThinkTime < 0)
      throw new IllegalArgumentException("Minimum think time must be a positive integer");

    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  public int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  public void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0)
      throw new IllegalArgumentException("Login delay factor must be a positive integer");

    this.loginDelayFactor = loginDelayFactor;
    evtLoginDelayFactorChanged.fire();
  }

  public Event eventApplicationBatchSizeChanged() {
    return evtApplicationBatchSizeChanged;
  }

  public Event eventApplicationCountChanged() {
    return evtApplicationtCountChanged;
  }

  public Event eventDoneExiting() {
    return evtDoneExiting;
  }

  public Event eventMaximumThinkTimeChanged() {
    return evtMaximumThinkTimeChanged;
  }

  public Event eventMinimumThinkTimeChanged() {
    return evtMinimumThinkTimeChanged;
  }

  public Event eventPausedChanged() {
    return evtPausedChanged;
  }

  public Event eventCollectChartDataChanged() {
    return evtCollectChartDataChanged;
  }

  public Event eventWarningTimeChanged() {
    return evtWarningTimeChanged;
  }

  protected void performWork(final Object application) {
    try {
      final UsageScenario scenario = (UsageScenario) scenarioChooser.getRandomItem();
      runScenario(scenario.getName(), application);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void runScenario(final String usageScenarioName, final Object application) throws Exception {
    getUsageScenario(usageScenarioName).run(application);
  }

  protected Collection<UsageScenario> initializeUsageScenarios() {
    return new ArrayList<UsageScenario>();
  }

  protected abstract Object initializeApplication() throws CancelException;

  protected abstract void disconnectApplication(final Object application);

  protected void initializeContext() {/**/}

  /**
   * Simulates a user think pause by sleeping for a little while
   * @throws InterruptedException in case the sleep is interrupted
   * @see #getThinkTime()
   */
  protected void think() throws InterruptedException {
    Thread.sleep(getThinkTime());
  }

  protected int getThinkTime() {
    final int time = getMaximumThinkTime() - getMinimumThinkTime();
    return time > 0 ? random.nextInt(time) + getMinimumThinkTime() : getMinimumThinkTime();
  }

  private synchronized void addApplication() {
    final Runnable applicationRunner = new Runnable() {
      public void run() {
        try {
          delayLogin();
          final Object application = initializeApplication();
          System.out.println("Initialized application: " + application);
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
                  if (workTime > warningTime)
                    counter.incrementDelayedWorkRequests();
                }
              }
            }
            catch (Exception e) {
              e.printStackTrace();
            }
          }
          disconnectApplication(application);
        }
        catch (Exception e) {
          System.out.println("Exception " + e.getMessage());
          e.printStackTrace();
        }
      }
    };
    new Thread(applicationRunner).start();
  }

  private synchronized void removeApplication() {
    Object application = null;
    try {
      application = applications.pop();
      evtApplicationtCountChanged.fire();
    }
    catch (Exception e) {
      System.out.println(application + " exception while logging out");
      e.printStackTrace();
    }
  }

  private void delayLogin() {
    try {
      final int sleepyTime = random.nextInt(maximumThinkTime * (loginDelayFactor <= 0 ? 1 : loginDelayFactor));
      System.out.println("AppModel delaying login for " + sleepyTime + " ms");
      Thread.sleep(sleepyTime);// delay login a bit so all do not try to login at the same time
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private RandomItemModel initializeScenarioChooser() {
    final RandomItemModel model = new RandomItemModel();
    for (final UsageScenario scenario : this.usageScenarios)
      model.addItem(scenario, scenario.getDefaultWeight());

    return model;
  }

  private void scheduleUpdateTime(final int intervalMs) {
    if (updateTimer != null)
      updateTimer.cancel();

    updateTimer = new Timer(true);
    updateTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        counter.updateRequestsPerSecond();
        if (collectChartData && !paused)
          updateChartData();
        if (stopped && applications.size() == 0)
          evtDoneExiting.fire();
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
    for (final UsageScenario usageScenario : this.usageScenarios)
      usageScenarioCollection.addSeries(new XYSeries(usageScenario.getName()));
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
  public static abstract class UsageScenario {

    private final String name;
    private int successfulRunCount = 0;
    private int unsuccessfulRunCount = 0;

    public UsageScenario(final String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public int getSuccessfulRunCount() {
      return successfulRunCount;
    }

    public int getUnsuccessfulRunCount() {
      return unsuccessfulRunCount;
    }

    public int getTotalRunCount() {
      return getSuccessfulRunCount() + getUnsuccessfulRunCount();
    }

    public void resetRunCount() {
      successfulRunCount = unsuccessfulRunCount = 0;
    }

    @Override
    public String toString() {
      return getName();
    }

    public void run(final Object application) throws Exception {
      if (application == null)
        throw new RuntimeException("Can not run without an application model");
      try {
        prepare(application);
        performScenario(application);
        successfulRunCount++;
      }
      catch (Exception e) {
        unsuccessfulRunCount++;
        e.printStackTrace();
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

  private static class Counter {

    private final Collection<UsageScenario> usageScenarios;
    private final Map<String, Integer> usageScenarioRates = new HashMap<String, Integer>();

    private int workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

    public Counter(final Collection<UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    public int getWorkRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    public int getDelayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    public int getScenarioRate(final String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName))
        return 0;

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
      final double seconds = (current - time)/1000;
      if (seconds > 5) {
        workRequestsPerSecond = (int) ((double) workRequestCounter /seconds);
        delayedWorkRequestsPerSecond = (int) ((double) delayedWorkRequestCounter /seconds);
        for (final UsageScenario scenario : usageScenarios)
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / seconds));

        resetCounters();
        time = current;
      }
    }

    private void resetCounters() {
      for (final UsageScenario scenario : usageScenarios)
        scenario.resetRunCount();
      workRequestCounter = 0;
      delayedWorkRequestCounter = 0;
    }
  }
}
