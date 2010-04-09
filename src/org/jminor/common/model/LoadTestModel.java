/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.db.User;

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
  private final Event evtMemoryUsageUpdated = new Event();
  private final Event evtDoneExiting = new Event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int applicationBatchSize;

  private boolean paused = false;
  private boolean stopped = false;
  private boolean collectChartData = false;

  private final Stack<Object> applications = new Stack<Object>();
  private final Collection<UsageScenario> usageScenarios;
  private final RandomItemModel scenarioRandomModel;
  private User user;

  private final XYSeries workRequestsSeries = new XYSeries("Work requests per second");
  private final XYSeries delayedWorkRequestsSeries = new XYSeries("Delayed requests per second");
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

  private final Counter counter;

  private int warningTime;

  /**
   * Constructs a new LoadTest.
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
    this.scenarioRandomModel = initializeScenarioRandomModel();
    this.counter = new Counter(this.usageScenarios);
    initializeChartData();
    initializeContext();
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        counter.updateRequestsPerSecond();
        if (collectChartData && !paused)
          updateChartData();
        evtMemoryUsageUpdated.fire();
        if (stopped && applications.size() == 0)
          evtDoneExiting.fire();
      }
    }, new Date(), 2000);
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

  public RandomItemModel getRandomModel() {
    return scenarioRandomModel;
  }

  public Collection<UsageScenario> getUsageScenarios() {
    return usageScenarios;
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

  public String getMemoryUsage() {
    return Util.getMemoryUsageString();
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

  public Event eventMemoryUsageUpdated() {
    return evtMemoryUsageUpdated;
  }

  protected void performWork(final Object application) {
    try {
      final UsageScenario scenario = (UsageScenario) scenarioRandomModel.getRandomItem();
      runScenario(scenario.getName(), application);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void runScenario(final String usageScenarioName, final Object application) throws Exception {
    getUsageScenario(usageScenarioName).run(application);
  }

  public UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios)
      if (scenario.getName().equals(usageScenarioName))
        return scenario;

    throw new RuntimeException("UsageScenario not found: " + usageScenarioName);
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
    final int time = maximumThinkTime - minimumThinkTime;
    return time > 0 ? random.nextInt(time) + minimumThinkTime : minimumThinkTime;
  }

  private synchronized void addApplication() {
    final Runnable applicationRunner = new Runnable() {
      public void run() {
        try {
          delayLogin();
          System.out.println("Initializing an application...");
          final Object application = initializeApplication();
          applications.push(application);
          evtApplicationtCountChanged.fire();
          while (applications.contains(application)) {
            try {
              think();
              if (!paused && (applications.contains(application))) {
                final long currentTime = System.currentTimeMillis();
                try {
                  counter.incrementWorkRequestsPerSecond();
                  performWork(application);
                }
                finally {
                  final long workTime = System.currentTimeMillis() - currentTime;
                  if (workTime > warningTime)
                    counter.incrementDelayedWorkRequestsPerSecond();
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

  private RandomItemModel initializeScenarioRandomModel() {
    final RandomItemModel model = new RandomItemModel();
    for (final UsageScenario scenario : this.usageScenarios)
      model.addItem(scenario, scenario.getDefaultWeight());

    return model;
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
    private int workRequestsPerSecondCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestsPerSecondCounter = 0;
    private long workRequestsPerSecondTime = System.currentTimeMillis();

    public Counter(final Collection<UsageScenario> usageScenarios) {
      this.usageScenarios = usageScenarios;
    }

    public int getWorkRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    public int getDelayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    public Collection<String> getScenarioNames() {
      return usageScenarioRates.keySet();
    }

    public int getScenarioRate(final String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName))
        return 0;

      return usageScenarioRates.get(scenarioName);
    }

    public void incrementWorkRequestsPerSecond() {
      workRequestsPerSecondCounter++;
    }

    public void incrementDelayedWorkRequestsPerSecond() {
      delayedWorkRequestsPerSecondCounter++;
    }

    public void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - workRequestsPerSecondTime)/1000;
      if (seconds > 5) {
        workRequestsPerSecond = (int) ((double) workRequestsPerSecondCounter/seconds);
        delayedWorkRequestsPerSecond = (int) ((double) delayedWorkRequestsPerSecondCounter/seconds);
        for (final UsageScenario scenario : usageScenarios)
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / seconds));

        resetCounters();
        workRequestsPerSecondTime = current;
      }
    }

    private void resetCounters() {
      for (final UsageScenario scenario : usageScenarios)
        scenario.resetRunCount();
      workRequestsPerSecondCounter = 0;
      delayedWorkRequestsPerSecondCounter = 0;
    }
  }
}
