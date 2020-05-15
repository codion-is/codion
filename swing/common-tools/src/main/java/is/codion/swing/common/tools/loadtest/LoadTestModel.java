/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.Memory;
import is.codion.common.TaskScheduler;
import is.codion.common.Util;
import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;
import is.codion.common.event.Events;
import is.codion.common.user.User;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.tools.randomizer.ItemRandomizerModel;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * A default LoadTest implementation.
 * @param <T> the type of the applications this load test uses
 */
public abstract class LoadTestModel<T> implements LoadTest<T> {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;

  protected static final Logger LOG = LoggerFactory.getLogger(LoadTestModel.class);

  protected static final Random RANDOM = new Random();

  private static final long NANO_IN_MILLI = 1000000;
  private static final double THOUSAND = 1000d;
  private static final double HUNDRED = 100d;
  private static final int MINIMUM_NUMBER_OF_THREADS = 12;

  private final Event<Boolean> pausedChangedEvent = Events.event();
  private final Event<Boolean> collectChartDataChangedEvent = Events.event();
  private final Event<Integer> maximumThinkTimeChangedEvent = Events.event();
  private final Event<Integer> minimumThinkTimeChangedEvent = Events.event();
  private final Event<Integer> loginDelayFactorChangedEvent = Events.event();
  private final Event<Integer> applicationCountChangedEvent = Events.event();
  private final Event<Integer> applicationBatchSizeChangedEvent = Events.event();
  private final Event shutdownEvent = Events.event();

  private int maximumThinkTime;
  private int minimumThinkTime;
  private int loginDelayFactor;
  private int applicationBatchSize;
  private User user;

  private volatile boolean shuttingDown = false;
  private volatile boolean paused = false;
  private volatile boolean collectChartData = false;

  private final Deque<ApplicationRunner> applications = new ConcurrentLinkedDeque<>();
  private final Map<String, UsageScenario<T>> usageScenarios = new HashMap<>();
  private final ItemRandomizer<UsageScenario<T>> scenarioChooser;
  private final ScheduledExecutorService scheduledExecutor =
          newScheduledThreadPool(Math.max(MINIMUM_NUMBER_OF_THREADS, Runtime.getRuntime().availableProcessors() * 2));
  private final Counter counter = new Counter();
  private final TaskScheduler updateChartDataScheduler;

  private final XYSeries scenariosRunSeries = new XYSeries("Total");
  private final XYSeries delayedScenarioRunsSeries = new XYSeries("Warn. time exceeded");

  private final XYSeriesCollection scenarioFailureCollection = new XYSeriesCollection();

  private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
  private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
  private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

  private final XYSeries numberOfApplicationsSeries = new XYSeries("Application count");
  private final XYSeriesCollection numberOfApplicationsCollection = new XYSeriesCollection();

  private final XYSeriesCollection usageScenarioCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemoryCollection = new XYSeries("Allocated");
  private final XYSeries usedMemoryCollection = new XYSeries("Used");
  private final XYSeries maxMemoryCollection = new XYSeries("Available");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();
  private final Collection<XYSeries> usageSeries = new ArrayList<>();
  private final Map<String, YIntervalSeries> durationSeries = new HashMap<>();
  private final Collection<XYSeries> failureSeries = new ArrayList<>();
  private final XYSeries systemLoadSeries = new XYSeries("System Load");
  private final XYSeries processLoadSeries = new XYSeries("Process Load");
  private final XYSeriesCollection systemLoadCollection = new XYSeriesCollection();

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
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }
    if (loginDelayFactor <= 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }
    this.user = user;
    this.maximumThinkTime = maximumThinkTime;
    this.minimumThinkTime = maximumThinkTime / 2;
    this.loginDelayFactor = loginDelayFactor;
    this.applicationBatchSize = applicationBatchSize;
    for (final UsageScenario<T> scenario : usageScenarios) {
      this.usageScenarios.put(scenario.getName(), scenario);
    }
    this.scenarioChooser = initializeScenarioChooser();
    initializeChartData();
    this.updateChartDataScheduler = new TaskScheduler(() -> {
      if (shuttingDown || paused) {
        return;
      }
      counter.updateRequestsPerSecond();
      if (collectChartData && !paused) {
        updateChartData();
      }
    }, DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();
  }

  @Override
  public final User getUser() {
    return user;
  }

  @Override
  public final void setUser(final User user) {
    this.user = user;
  }

  @Override
  public String getTitle() {
    return getClass().getSimpleName();
  }

  @Override
  public final UsageScenario<T> getUsageScenario(final String usageScenarioName) {
    final UsageScenario<T> scenario = usageScenarios.get(usageScenarioName);
    if (scenario != null) {
      return scenario;
    }

    throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
  }

  @Override
  public final Collection<String> getUsageScenarios() {
    return unmodifiableCollection(usageScenarios.keySet());
  }

  @Override
  public final void setWeight(final String scenarioName, final int weight) {
    scenarioChooser.setWeight(getUsageScenario(scenarioName), weight);
  }

  @Override
  public final boolean isScenarioEnabled(final String scenarioName) {
    return scenarioChooser.isItemEnabled(getUsageScenario(scenarioName));
  }

  @Override
  public final void setScenarioEnabled(final String scenarioName, final boolean enabled) {
    scenarioChooser.setItemEnabled(getUsageScenario(scenarioName), enabled);
  }

  @Override
  public final ItemRandomizer<UsageScenario<T>> getScenarioChooser() {
    return scenarioChooser;
  }

  @Override
  public final IntervalXYDataset getScenarioDurationDataset(final String name) {
    final YIntervalSeriesCollection scenarioDurationCollection = new YIntervalSeriesCollection();
    scenarioDurationCollection.addSeries(durationSeries.get(name));

    return scenarioDurationCollection;
  }

  @Override
  public final XYDataset getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  @Override
  public final XYDataset getNumberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  @Override
  public final XYDataset getUsageScenarioDataset() {
    return usageScenarioCollection;
  }

  @Override
  public final XYDataset getUsageScenarioFailureDataset() {
    return scenarioFailureCollection;
  }

  @Override
  public final XYDataset getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  @Override
  public final XYDataset getSystemLoadDataset() {
    return systemLoadCollection;
  }

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
    systemLoadSeries.clear();
    processLoadSeries.clear();
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

  @Override
  public final int getUpdateInterval() {
    return updateChartDataScheduler.getInterval();
  }

  @Override
  public final void setUpdateInterval(final int updateInterval) {
    updateChartDataScheduler.setInterval(updateInterval);
  }

  @Override
  public final int getApplicationCount() {
    return applications.size();
  }

  @Override
  public final int getApplicationBatchSize() {
    return applicationBatchSize;
  }

  @Override
  public final void setApplicationBatchSize(final int applicationBatchSize) {
    if (applicationBatchSize <= 0) {
      throw new IllegalArgumentException("Application batch size must be a positive integer");
    }

    this.applicationBatchSize = applicationBatchSize;
    applicationBatchSizeChangedEvent.onEvent(this.applicationBatchSize);
  }

  @Override
  public final void addApplicationBatch() {
    for (int i = 0; i < applicationBatchSize; i++) {
      final ApplicationRunner runner = new ApplicationRunner();
      applications.push(runner);
      applicationCountChangedEvent.onEvent();
      int initialDelay = getThinkTime();
      if (loginDelayFactor > 0) {
        initialDelay *= loginDelayFactor;
      }
      scheduledExecutor.schedule(runner, initialDelay, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public final void removeApplicationBatch() {
    for (int i = 0; i < applicationBatchSize && !applications.isEmpty(); i++) {
      applications.pop().stop();
    }
    applicationCountChangedEvent.onEvent(applications.size());
  }

  @Override
  public final boolean isPaused() {
    return this.paused;
  }

  @Override
  public final void setPaused(final boolean paused) {
    this.paused = paused;
    pausedChangedEvent.onEvent(this.paused);
  }

  @Override
  public final boolean isCollectChartData() {
    return collectChartData;
  }

  @Override
  public final void setCollectChartData(final boolean collectChartData) {
    this.collectChartData = collectChartData;
    collectChartDataChangedEvent.onEvent(this.collectChartData);
  }

  @Override
  public final void shutdown() {
    shuttingDown = true;
    updateChartDataScheduler.stop();
    scheduledExecutor.shutdown();
    paused = false;
    synchronized (applications) {
      while (!applications.isEmpty()) {
        applications.pop().stop();
      }
    }
    try {
      scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    shutdownEvent.onEvent();
  }

  @Override
  public final int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  @Override
  public final void setMaximumThinkTime(final int maximumThinkTime) {
    if (maximumThinkTime <= 0) {
      throw new IllegalArgumentException("Maximum think time must be a positive integer");
    }

    this.maximumThinkTime = maximumThinkTime;
    maximumThinkTimeChangedEvent.onEvent(this.maximumThinkTime);
  }

  @Override
  public final int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  @Override
  public final void setMinimumThinkTime(final int minimumThinkTime) {
    if (minimumThinkTime < 0) {
      throw new IllegalArgumentException("Minimum think time must be a positive integer");
    }

    this.minimumThinkTime = minimumThinkTime;
    minimumThinkTimeChangedEvent.onEvent(this.minimumThinkTime);
  }

  @Override
  public final int getLoginDelayFactor() {
    return this.loginDelayFactor;
  }

  @Override
  public final void setLoginDelayFactor(final int loginDelayFactor) {
    if (loginDelayFactor < 0) {
      throw new IllegalArgumentException("Login delay factor must be a positive integer");
    }

    this.loginDelayFactor = loginDelayFactor;
    loginDelayFactorChangedEvent.onEvent(this.loginDelayFactor);
  }

  @Override
  public final EventObserver<Integer> applicationBatchSizeObserver() {
    return applicationBatchSizeChangedEvent.getObserver();
  }

  @Override
  public final EventObserver<Integer> applicationCountObserver() {
    return applicationCountChangedEvent.getObserver();
  }

  @Override
  public final EventObserver<Integer> maximumThinkTimeObserver() {
    return maximumThinkTimeChangedEvent.getObserver();
  }

  @Override
  public final EventObserver<Integer> getMinimumThinkTimeObserver() {
    return minimumThinkTimeChangedEvent.getObserver();
  }

  @Override
  public final EventObserver<Boolean> getPauseObserver() {
    return pausedChangedEvent.getObserver();
  }

  @Override
  public final EventObserver<Boolean> collectChartDataObserver() {
    return collectChartDataChangedEvent.getObserver();
  }

  /**
   * Runs the scenario with the given name on the given application
   * @param usageScenario the scenario to run
   * @param application the application to use
   */
  protected final void runScenario(final UsageScenario<T> usageScenario, final T application) {
    usageScenario.run(application);
  }

  /**
   * @param listener a listener notified when this load test model has been shutdown.
   */
  protected void addShutdownListener(final EventListener listener) {
    shutdownEvent.addListener(listener);
  }

  /**
   * @return an initialized application.
   * @throws is.codion.common.model.CancelException in case the initialization was cancelled
   */
  protected abstract T initializeApplication();

  /**
   * @param application the application to disconnect
   */
  protected abstract void disconnectApplication(T application);

  /**
   * @return a random think time in milliseconds based on the values of minimumThinkTime and maximumThinkTime
   * @see #setMinimumThinkTime(int)
   * @see #setMaximumThinkTime(int)
   */
  protected final int getThinkTime() {
    final int time = minimumThinkTime - maximumThinkTime;
    return time > 0 ? RANDOM.nextInt(time) + minimumThinkTime : minimumThinkTime;
  }

  private ItemRandomizer<UsageScenario<T>> initializeScenarioChooser() {
    final ItemRandomizer<UsageScenario<T>> model = new ItemRandomizerModel<>();
    for (final UsageScenario<T> scenario : usageScenarios.values()) {
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
    systemLoadCollection.addSeries(systemLoadSeries);
    systemLoadCollection.addSeries(processLoadSeries);
    usageScenarioCollection.addSeries(scenariosRunSeries);
    for (final UsageScenario<T> usageScenario : usageScenarios.values()) {
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
    allocatedMemoryCollection.add(time, Memory.getAllocatedMemory() / THOUSAND);
    usedMemoryCollection.add(time, Memory.getUsedMemory() / THOUSAND);
    maxMemoryCollection.add(time, Memory.getMaxMemory() / THOUSAND);
    systemLoadSeries.add(time, getSystemCpuLoad() * HUNDRED);
    processLoadSeries.add(time, getProcessCpuLoad() * HUNDRED);
    scenariosRunSeries.add(time, counter.getWorkRequestsPerSecond());
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

  private static double getSystemCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  private static double getProcessCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  private final class ApplicationRunner implements Runnable {

    private T application = null;
    private volatile boolean stopped = false;

    private void stop() {
      stopped = true;
    }

    @Override
    public void run() {
      try {
        if (application == null) {
          application = initializeApplication();
          LOG.debug("LoadTestModel initialized application: {}", application);
        }
        if (!stopped && !paused) {
          runRandomScenario(application);
        }
        if (stopped) {
          disconnectApplication(application);
          LOG.debug("LoadTestModel disconnected application: {}", application);
        }
        else {
          scheduledExecutor.schedule(this, getThinkTime(), TimeUnit.MILLISECONDS);
        }
      }
      catch (final Exception e) {
        LOG.debug("Exception during " + (application == null ? "application initialization" : ("run " + application)), e);
      }
    }

    private void runRandomScenario(final T application) {
      final long currentTimeNano = System.nanoTime();
      UsageScenario<T> scenario = null;
      try {
        scenario = scenarioChooser.getRandomItem();
        runScenario(scenario, application);
      }
      finally {
        counter.incrementWorkRequests();
        int maximumTime = 0;
        final long workTimeMillis = (System.nanoTime() - currentTimeNano) / NANO_IN_MILLI;
        if (scenario != null) {
          counter.addScenarioDuration(scenario.getName(), (int) workTimeMillis);
          maximumTime = scenario.getMaximumTime();
        }
        if (maximumTime > 0 && workTimeMillis > maximumTime) {
          counter.incrementDelayedWorkRequests();
        }
      }
    }
  }

  private final class Counter {

    private static final int UPDATE_INTERVAL = 5;

    private final Map<String, Integer> usageScenarioRates = new HashMap<>();
    private final Map<String, Integer> usageScenarioAvgDurations = new HashMap<>();
    private final Map<String, Integer> usageScenarioMaxDurations = new HashMap<>();
    private final Map<String, Integer> usageScenarioMinDurations = new HashMap<>();
    private final Map<String, Integer> usageScenarioFailures = new HashMap<>();
    private final Map<String, Collection<Integer>> scenarioDurations = new HashMap<>();

    private double workRequestsPerSecond = 0;
    private int workRequestCounter = 0;
    private int delayedWorkRequestsPerSecond = 0;
    private int delayedWorkRequestCounter = 0;
    private long time = System.currentTimeMillis();

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
          scenarioDurations.put(scenarioName, new LinkedList<>());
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
        for (final UsageScenario<T> scenario : usageScenarios.values()) {
          usageScenarioRates.put(scenario.getName(), (int) (scenario.getTotalRunCount() / elapsedSeconds));
          usageScenarioFailures.put(scenario.getName(), scenario.getUnsuccessfulRunCount());
          calculateScenarioDuration(scenario);
        }
        resetCounters();
        time = current;
      }
    }

    private void calculateScenarioDuration(final UsageScenario<T> scenario) {
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

    private void resetCounters() {
      for (final UsageScenario<T> scenario : usageScenarios.values()) {
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
