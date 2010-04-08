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
  private final Event evtMaximumThinkTimeChanged = new Event();
  private final Event evtMinimumThinkTimeChanged = new Event();
  private final Event evtWarningTimeChanged = new Event();
  private final Event evtClientCountChanged = new Event();
  private final Event evtBatchSizeChanged = new Event();
  private final Event evtDoneExiting = new Event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int batchSize;

  private boolean paused = false;
  private boolean stopped = false;

  private final Stack<Object> clients = new Stack<Object>();
  private final Collection<UsageScenario> usageScenarios;
  private final RandomItemModel scenarioRandomModel;
  private User user;

  private final XYSeries workRequestsSeries = new XYSeries("Work requests per second");
  private final XYSeries delayedWorkRequestsSeries = new XYSeries("Delayed requests per second");
  private final XYSeriesCollection workRequestsCollection = new XYSeriesCollection();

  private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
  private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
  private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

  private final XYSeries numberOfClientsSeries = new XYSeries("Client count");
  private final XYSeriesCollection numberOfClientsCollection = new XYSeriesCollection();

  private final XYSeriesCollection usageScenarioCollection = new XYSeriesCollection();

  private Counter counter = new Counter();

  private int warningTime;

  /**
   * Constructs a new LoadTest.
   * @param user the default user to use when initializing applications
   * @param maximumThinkTime the maximum think time, by default the minimum think time is max / 2
   * @param loginDelayFactor the value with which to multiply the think time when delaying login
   * @param batchSize the number of clients to add in a batch
   * @param warningTime a work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)
   */
  public LoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int batchSize,
                       final int warningTime) {
    if (maximumThinkTime <= 0)
      throw new IllegalArgumentException("Maximum think time must be a positive integer");

    this.user = user;
    this.maximumThinkTime = maximumThinkTime;
    this.minimumThinkTime = maximumThinkTime / 2;
    this.loginDelayFactor = loginDelayFactor;
    this.batchSize = batchSize;
    this.warningTime = warningTime;
    this.usageScenarios = initializeUsageScenarios();
    this.scenarioRandomModel = initializeScenarioRandomModel();
    initializeChartData();
    initializeContext();
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        counter.updateRequestsPerSecond();
        updateChartData();
        if (stopped && clients.size() == 0)
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

  public XYSeriesCollection getNumberOfClientsDataset() {
    return numberOfClientsCollection;
  }

  public XYSeriesCollection getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  public int getWarningTime() {
    return warningTime;
  }

  public void setWarningTime(int warningTime) {
    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  /**
   * @return the number of active clients
   */
  public int getClientCount() {
    return clients.size();
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    evtBatchSizeChanged.fire();
  }

  public void addClients() throws Exception {
    for (int i = 0; i < batchSize; i++)
      addClient();
  }

  public void removeClients() throws Exception {
    for (int i = 0; i < batchSize && clients.size() > 0; i++)
      removeClient();
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

  public void exit() {
    paused = false;
    stopped = true;
    synchronized (clients) {
      while (clients.size() > 0)
        removeClient();
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
    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  public Event eventBatchSizeChanged() {
    return evtBatchSizeChanged;
  }

  public Event eventClientCountChanged() {
    return evtClientCountChanged;
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

  public Event eventWarningTimeChanged() {
    return evtWarningTimeChanged;
  }

  protected void runScenario(final String usageScenarioName, final Object application) throws Exception {
    counter.incrementScenario(usageScenarioName);
    getUsageScenario(usageScenarioName).run(application);
  }

  protected Collection<UsageScenario> initializeUsageScenarios() {
    return new ArrayList<UsageScenario>();
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

  private synchronized void addClient() {
    final Runnable clientRunner = new Runnable() {
      public void run() {
        try {
          delayLogin();
          System.out.println("Initializing an application...");
          final Object application = initializeApplication();
          clients.push(application);
          evtClientCountChanged.fire();
          while (clients.contains(application)) {
            try {
              think();
              if (!paused && (clients.contains(application))) {
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
    new Thread(clientRunner).start();
  }

  private synchronized void removeClient() {
    Object application = null;
    try {
      application = clients.pop();
      evtClientCountChanged.fire();
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
    numberOfClientsCollection.addSeries(numberOfClientsSeries);
    for (final UsageScenario usageScenario : this.usageScenarios)
      usageScenarioCollection.addSeries(new XYSeries(usageScenario.getName()));
  }

  private void updateChartData() {
    final long time = System.currentTimeMillis();
    workRequestsSeries.add(time, counter.getWorkRequestsPerSecond());
    delayedWorkRequestsSeries.add(time, counter.getDelayedWorkRequestsPerSecond());
    minimumThinkTimeSeries.add(time, minimumThinkTime);
    maximumThinkTimeSeries.add(time, maximumThinkTime);
    numberOfClientsSeries.add(time, clients.size());
    for (final Object object : usageScenarioCollection.getSeries()) {
      final XYSeries series = (XYSeries) object;
      series.add(time, counter.getScenarioRate((String) series.getKey()));
    }
  }

  private UsageScenario getUsageScenario(final String usageScenarioName) {
    for (final UsageScenario scenario : usageScenarios)
      if (scenario.getName().equals(usageScenarioName))
        return scenario;

    throw new RuntimeException("UsageScenario not found: " + usageScenarioName);
  }

  public static abstract class UsageScenario {

    private final String name;

    public UsageScenario(final String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
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
      }
      catch (Exception e) {
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
    private final Map<String, Integer> usageScenarioCounter = new HashMap<String, Integer>();
    private final Map<String, Integer> usageScenarioRates = new HashMap<String, Integer>();

    private int workRequestsPerSecond = 0;
    private int workRequestsPerSecondCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestsPerSecondCounter = 0;
    private long workRequestsPerSecondTime = System.currentTimeMillis();

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

    public void incrementScenario(final String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName))
        usageScenarioRates.put(scenarioName, 0);
      usageScenarioCounter.put(scenarioName, usageScenarioCounter.containsKey(scenarioName) ? usageScenarioCounter.get(scenarioName) + 1 : 0);
    }

    public void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - workRequestsPerSecondTime)/1000;
      if (seconds > 5) {
        workRequestsPerSecond = (int) ((double) workRequestsPerSecondCounter/seconds);
        delayedWorkRequestsPerSecond = (int) ((double) delayedWorkRequestsPerSecondCounter/seconds);
        for (final String scenarioName : usageScenarioRates.keySet()) {
          if (usageScenarioCounter.containsKey(scenarioName))
            usageScenarioRates.put(scenarioName, (int) (usageScenarioCounter.get(scenarioName)/seconds));
          else
            usageScenarioRates.put(scenarioName, 0);
        }
        usageScenarioCounter.clear();
        workRequestsPerSecondCounter = 0;
        delayedWorkRequestsPerSecondCounter = 0;
        workRequestsPerSecondTime = current;
      }
    }
  }
}
