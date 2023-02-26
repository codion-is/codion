/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.Memory;
import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;

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
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Collections.unmodifiableMap;
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

  private final State pausedState = State.state();
  private final State collectChartDataState = State.state();
  private final StateObserver chartUpdateSchedulerEnabledState =
          State.and(pausedState.reversedObserver(), collectChartDataState);

  private final Value<Integer> loginDelayFactorValue;
  private final Value<Integer> applicationBatchSizeValue;
  private final Value<Integer> maximumThinkTimeValue;
  private final Value<Integer> minimumThinkTimeValue;
  private final Value<Integer> applicationCountValue = Value.value(0);
  private final Event<?> shutdownEvent = Event.event();

  private User user;

  private volatile boolean shuttingDown = false;

  private final Deque<ApplicationRunner> applications = new ConcurrentLinkedDeque<>();
  private final Map<String, UsageScenario<T>> usageScenarios;
  private final ItemRandomizer<UsageScenario<T>> scenarioChooser;
  private final ScheduledExecutorService scheduledExecutor =
          newScheduledThreadPool(Math.max(MINIMUM_NUMBER_OF_THREADS, Runtime.getRuntime().availableProcessors() * 2));
  private final Counter counter = new Counter();
  private final TaskScheduler chartUpdateScheduler;

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
  protected LoadTestModel(User user, Collection<? extends UsageScenario<T>> usageScenarios,
                          int maximumThinkTime, int loginDelayFactor, int applicationBatchSize) {
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
    this.loginDelayFactorValue = Value.value(loginDelayFactor);
    this.applicationBatchSizeValue = Value.value(applicationBatchSize);
    this.minimumThinkTimeValue = Value.value(maximumThinkTime / 2);
    this.maximumThinkTimeValue = Value.value(maximumThinkTime);
    this.loginDelayFactorValue.addValidator(new MinimumValidator(1));
    this.applicationBatchSizeValue.addValidator(new MinimumValidator(1));
    this.minimumThinkTimeValue.addValidator(new MinimumThinkTimeValidator());
    this.maximumThinkTimeValue.addValidator(new MaximumThinkTimeValidator());
    this.usageScenarios = unmodifiableMap(usageScenarios.stream()
            .collect(Collectors.toMap(UsageScenario::name, Function.identity())));
    this.scenarioChooser = createScenarioChooser();
    initializeChartModels();
    this.chartUpdateScheduler = TaskScheduler.builder(new ChartUpdateTask())
            .interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS)
            .timeUnit(TimeUnit.MILLISECONDS)
            .build();
    bindEvents();
  }

  @Override
  public final User getUser() {
    return user;
  }

  @Override
  public final void setUser(User user) {
    this.user = user;
  }

  @Override
  public String title() {
    return getClass().getSimpleName();
  }

  @Override
  public final UsageScenario<T> usageScenario(String usageScenarioName) {
    UsageScenario<T> scenario = usageScenarios.get(usageScenarioName);
    if (scenario == null) {
      throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
    }

    return scenario;
  }

  @Override
  public final Collection<String> usageScenarios() {
    return usageScenarios.keySet();
  }

  @Override
  public final void setWeight(String scenarioName, int weight) {
    scenarioChooser.setWeight(usageScenario(scenarioName), weight);
  }

  @Override
  public final boolean isScenarioEnabled(String scenarioName) {
    return scenarioChooser.isItemEnabled(usageScenario(scenarioName));
  }

  @Override
  public final void setScenarioEnabled(String scenarioName, boolean enabled) {
    scenarioChooser.setItemEnabled(usageScenario(scenarioName), enabled);
  }

  @Override
  public final ItemRandomizer<UsageScenario<T>> scenarioChooser() {
    return scenarioChooser;
  }

  @Override
  public final IntervalXYDataset scenarioDurationDataset(String name) {
    YIntervalSeriesCollection scenarioDurationCollection = new YIntervalSeriesCollection();
    scenarioDurationCollection.addSeries(durationSeries.get(name));

    return scenarioDurationCollection;
  }

  @Override
  public final XYDataset thinkTimeDataset() {
    return thinkTimeCollection;
  }

  @Override
  public final XYDataset numberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  @Override
  public final XYDataset usageScenarioDataset() {
    return usageScenarioCollection;
  }

  @Override
  public final XYDataset usageScenarioFailureDataset() {
    return scenarioFailureCollection;
  }

  @Override
  public final XYDataset memoryUsageDataset() {
    return memoryUsageCollection;
  }

  @Override
  public final XYDataset systemLoadDataset() {
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
    for (XYSeries series : usageSeries) {
      series.clear();
    }
    for (XYSeries series : failureSeries) {
      series.clear();
    }
    for (YIntervalSeries series : durationSeries.values()) {
      series.clear();
    }
  }

  @Override
  public final int getUpdateInterval() {
    return chartUpdateScheduler.getInterval();
  }

  @Override
  public final void setUpdateInterval(int updateInterval) {
    chartUpdateScheduler.setInterval(updateInterval);
  }

  @Override
  public final int applicationCount() {
    return applications.size();
  }

  @Override
  public final Value<Integer> applicationBatchSizeValue() {
    return applicationBatchSizeValue;
  }

  @Override
  public final void addApplicationBatch() {
    for (int i = 0; i < applicationBatchSizeValue.get(); i++) {
      ApplicationRunner runner = new ApplicationRunner();
      applications.push(runner);
      int initialDelay = thinkTime();
      if (loginDelayFactorValue.get() > 0) {
        initialDelay *= loginDelayFactorValue.get();
      }
      scheduledExecutor.schedule(runner, initialDelay, TimeUnit.MILLISECONDS);
    }
    applicationCountValue.set(applications.size());
  }

  @Override
  public final void removeApplicationBatch() {
    for (int i = 0; i < applicationBatchSizeValue.get() && !applications.isEmpty(); i++) {
      applications.pop().stop();
    }
    applicationCountValue.set(applications.size());
  }

  @Override
  public final State pausedState() {
    return pausedState;
  }

  @Override
  public final State collectChartDataState() {
    return collectChartDataState;
  }

  @Override
  public final void shutdown() {
    shuttingDown = true;
    chartUpdateScheduler.stop();
    scheduledExecutor.shutdown();
    synchronized (applications) {
      while (!applications.isEmpty()) {
        applications.pop().stop();
      }
    }
    try {
      scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    shutdownEvent.onEvent();
  }

  @Override
  public final Value<Integer> maximumThinkTimeValue() {
    return maximumThinkTimeValue;
  }

  @Override
  public final Value<Integer> minimumThinkTimeValue() {
    return minimumThinkTimeValue;
  }

  @Override
  public final Value<Integer> loginDelayFactorValue() {
    return loginDelayFactorValue;
  }

  @Override
  public final ValueObserver<Integer> applicationCountObserver() {
    return applicationCountValue.observer();
  }

  /**
   * Runs the scenario with the given name on the given application
   * @param usageScenario the scenario to run
   * @param application the application to use
   */
  protected final void runScenario(UsageScenario<T> usageScenario, T application) {
    usageScenario.run(application);
  }

  /**
   * @param listener a listener notified when this load test model has been shutdown.
   */
  protected final void addShutdownListener(EventListener listener) {
    shutdownEvent.addListener(listener);
  }

  /**
   * @return an initialized application.
   * @throws is.codion.common.model.CancelException in case the initialization was cancelled
   */
  protected abstract T createApplication();

  /**
   * @param application the application to disconnect
   */
  protected abstract void disconnectApplication(T application);

  /**
   * @return a random think time in milliseconds based on the values of minimumThinkTime and maximumThinkTime
   * @see #minimumThinkTimeValue()
   * @see #maximumThinkTimeValue()
   */
  protected final int thinkTime() {
    int time = minimumThinkTimeValue.get() - maximumThinkTimeValue.get();
    return time > 0 ? RANDOM.nextInt(time) + minimumThinkTimeValue.get() : minimumThinkTimeValue.get();
  }

  private ItemRandomizer<UsageScenario<T>> createScenarioChooser() {
    return ItemRandomizer.itemRandomizer(usageScenarios.values().stream()
            .map(scenario -> ItemRandomizer.RandomItem.randomItem(scenario, scenario.defaultWeight()))
            .collect(Collectors.toList()));
  }

  private void initializeChartModels() {
    thinkTimeCollection.addSeries(minimumThinkTimeSeries);
    thinkTimeCollection.addSeries(maximumThinkTimeSeries);
    numberOfApplicationsCollection.addSeries(numberOfApplicationsSeries);
    memoryUsageCollection.addSeries(maxMemoryCollection);
    memoryUsageCollection.addSeries(allocatedMemoryCollection);
    memoryUsageCollection.addSeries(usedMemoryCollection);
    systemLoadCollection.addSeries(systemLoadSeries);
    systemLoadCollection.addSeries(processLoadSeries);
    usageScenarioCollection.addSeries(scenariosRunSeries);
    for (UsageScenario<T> usageScenario : usageScenarios.values()) {
      XYSeries series = new XYSeries(usageScenario.name());
      usageScenarioCollection.addSeries(series);
      usageSeries.add(series);
      YIntervalSeries avgDurSeries = new YIntervalSeries(usageScenario.name());
      durationSeries.put(usageScenario.name(), avgDurSeries);
      XYSeries failSeries = new XYSeries(usageScenario.name());
      scenarioFailureCollection.addSeries(failSeries);
      failureSeries.add(failSeries);
    }
    usageScenarioCollection.addSeries(delayedScenarioRunsSeries);
  }

  private void bindEvents() {
    chartUpdateSchedulerEnabledState.addDataListener(active -> {
      if (active) {
        chartUpdateScheduler.start();
      }
      else {
        chartUpdateScheduler.stop();
      }
    });
  }

  private static double systemCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  private static double processCpuLoad() {
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
          application = createApplication();
          LOG.debug("LoadTestModel initialized application: {}", application);
        }
        if (!stopped && !pausedState.get()) {
          runRandomScenario(application);
        }
        if (stopped) {
          disconnectApplication(application);
          LOG.debug("LoadTestModel disconnected application: {}", application);
        }
        else {
          scheduledExecutor.schedule(this, thinkTime(), TimeUnit.MILLISECONDS);
        }
      }
      catch (Exception e) {
        LOG.debug("Exception during " + (application == null ? "application initialization" : ("run " + application)), e);
      }
    }

    private void runRandomScenario(T application) {
      long currentTimeNano = System.nanoTime();
      UsageScenario<T> scenario = null;
      try {
        scenario = scenarioChooser.randomItem();
        runScenario(scenario, application);
      }
      finally {
        if (scenario != null) {
          counter.addScenarioDuration(scenario, (int) ((System.nanoTime() - currentTimeNano) / NANO_IN_MILLI));
        }
      }
    }
  }

  private final class ChartUpdateTask implements Runnable {

    @Override
    public void run() {
      if (shuttingDown) {
        return;
      }
      counter.updateRequestsPerSecond();
      updateChartData();
    }

    private void updateChartData() {
      long time = System.currentTimeMillis();
      delayedScenarioRunsSeries.add(time, counter.delayedWorkRequestsPerSecond());
      minimumThinkTimeSeries.add(time, minimumThinkTimeValue.get());
      maximumThinkTimeSeries.add(time, maximumThinkTimeValue.get());
      numberOfApplicationsSeries.add(time, applications.size());
      allocatedMemoryCollection.add(time, Memory.allocatedMemory() / THOUSAND);
      usedMemoryCollection.add(time, Memory.usedMemory() / THOUSAND);
      maxMemoryCollection.add(time, Memory.maxMemory() / THOUSAND);
      systemLoadSeries.add(time, systemCpuLoad() * HUNDRED);
      processLoadSeries.add(time, processCpuLoad() * HUNDRED);
      scenariosRunSeries.add(time, counter.workRequestsPerSecond());
      for (XYSeries series : usageSeries) {
        series.add(time, counter.scenarioRate((String) series.getKey()));
      }
      for (YIntervalSeries series : durationSeries.values()) {
        String scenario = (String) series.getKey();
        series.add(time, counter.averageScenarioDuration(scenario),
                counter.minimumScenarioDuration(scenario), counter.maximumScenarioDuration(scenario));
      }
      for (XYSeries series : failureSeries) {
        series.add(time, counter.scenarioFailureRate((String) series.getKey()));
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

    private double workRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    private int delayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond;
    }

    private int minimumScenarioDuration(String scenarioName) {
      if (!usageScenarioMinDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioMinDurations.get(scenarioName);
    }

    private int maximumScenarioDuration(String scenarioName) {
      if (!usageScenarioMaxDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioMaxDurations.get(scenarioName);
    }

    private int averageScenarioDuration(String scenarioName) {
      if (!usageScenarioAvgDurations.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioAvgDurations.get(scenarioName);
    }

    private double scenarioFailureRate(String scenarioName) {
      if (!usageScenarioFailures.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioFailures.get(scenarioName);
    }

    private int scenarioRate(String scenarioName) {
      if (!usageScenarioRates.containsKey(scenarioName)) {
        return 0;
      }

      return usageScenarioRates.get(scenarioName);
    }

    private void addScenarioDuration(UsageScenario<T> scenario, int duration) {
      synchronized (scenarioDurations) {
        scenarioDurations.computeIfAbsent(scenario.name(), scenarioName -> new LinkedList<>()).add(duration);
        workRequestCounter++;
        if (scenario.maximumTime() > 0 && duration > scenario.maximumTime()) {
          delayedWorkRequestCounter++;
        }
      }
    }

    private void updateRequestsPerSecond() {
      long current = System.currentTimeMillis();
      double elapsedSeconds = (current - time) / THOUSAND;
      if (elapsedSeconds > UPDATE_INTERVAL) {
        usageScenarioAvgDurations.clear();
        usageScenarioMinDurations.clear();
        usageScenarioMaxDurations.clear();
        workRequestsPerSecond = workRequestCounter / elapsedSeconds;
        delayedWorkRequestsPerSecond = (int) (delayedWorkRequestCounter / elapsedSeconds);
        for (UsageScenario<T> scenario : usageScenarios.values()) {
          usageScenarioRates.put(scenario.name(), (int) (scenario.totalRunCount() / elapsedSeconds));
          usageScenarioFailures.put(scenario.name(), scenario.unsuccessfulRunCount());
          calculateScenarioDuration(scenario);
        }
        resetCounters();
        time = current;
      }
    }

    private void calculateScenarioDuration(UsageScenario<T> scenario) {
      synchronized (scenarioDurations) {
        Collection<Integer> durations = scenarioDurations.get(scenario.name());
        if (!nullOrEmpty(durations)) {
          int totalDuration = 0;
          int minDuration = -1;
          int maxDuration = -1;
          for (Integer duration : durations) {
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
          usageScenarioAvgDurations.put(scenario.name(), totalDuration / durations.size());
          usageScenarioMinDurations.put(scenario.name(), minDuration);
          usageScenarioMaxDurations.put(scenario.name(), maxDuration);
        }
      }
    }

    private void resetCounters() {
      for (UsageScenario<T> scenario : usageScenarios.values()) {
        scenario.resetRunCount();
      }
      workRequestCounter = 0;
      delayedWorkRequestCounter = 0;
      synchronized (scenarioDurations) {
        scenarioDurations.clear();
      }
    }
  }

  private static class MinimumValidator implements Value.Validator<Integer> {

    private final int minimumValue;

    private MinimumValidator(int minimumValue) {
      this.minimumValue = minimumValue;
    }

    @Override
    public void validate(Integer value) {
      if (value == null || value < minimumValue) {
        throw new IllegalArgumentException("Value must be larger than: " + minimumValue);
      }
    }
  }

  private final class MinimumThinkTimeValidator extends MinimumValidator {

    private MinimumThinkTimeValidator() {
      super(0);
    }

    @Override
    public void validate(Integer value) {
      super.validate(value);
      if (value > maximumThinkTimeValue.get()) {
        throw new IllegalArgumentException("Minimum think time must be equal to or below maximum think time");
      }
    }
  }

  private final class MaximumThinkTimeValidator extends MinimumValidator {

    private MaximumThinkTimeValidator() {
      super(0);
    }

    @Override
    public void validate(Integer value) {
      super.validate(value);
      if (value < minimumThinkTimeValue.get()) {
        throw new IllegalArgumentException("Maximum think time must be equal to or exceed minimum think time");
      }
    }
  }
}
