/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.Memory;
import is.codion.common.event.Event;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;
import is.codion.swing.common.model.tools.loadtest.UsageScenario.RunResult;
import is.codion.swing.common.model.tools.randomizer.ItemRandomizer;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;

/**
 * A default LoadTest implementation.
 * @param <T> the type of the applications this load test uses
 */
final class DefaultLoadTestModel<T> implements LoadTestModel<T> {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLoadTestModel.class);

  private static final Random RANDOM = new Random();
  private static final double THOUSAND = 1000d;
  private static final double HUNDRED = 100d;
  private static final int MINIMUM_NUMBER_OF_THREADS = 12;

  private final Function<User, T> applicationFactory;
  private final Consumer<T> closeApplication;
  private final State paused = State.state();
  private final State collectChartData = State.state();
  private final State autoRefreshApplications = State.state(true);
  private final StateObserver chartUpdateSchedulerEnabled =
          State.and(paused.not(), collectChartData);
  private final StateObserver applicationsRefreshSchedulerEnabled =
          State.and(paused.not(), autoRefreshApplications);

  private final Value<Integer> loginDelayFactor;
  private final Value<Integer> applicationBatchSize;
  private final Value<Integer> maximumThinkTime;
  private final Value<Integer> minimumThinkTime;
  private final Value<Integer> applicationCount = Value.value(0);
  private final Event<?> shutdownEvent = Event.event();

  private final Value<User> user;

  private volatile boolean shuttingDown = false;

  private final List<ApplicationRunner> applications = new ArrayList<>();
  private final FilteredTableModel<Application, Integer> applicationTableModel;
  private final Map<String, UsageScenario<T>> usageScenarios;
  private final ItemRandomizer<UsageScenario<T>> scenarioChooser;
  private final ScheduledExecutorService scheduledExecutor =
          newScheduledThreadPool(Math.max(MINIMUM_NUMBER_OF_THREADS, Runtime.getRuntime().availableProcessors() * 2));
  private final Counter counter = new Counter();
  private final TaskScheduler chartUpdateScheduler;
  private final TaskScheduler applicationsRefreshScheduler;

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
  private final Function<LoadTestModel<T>, String> titleFactory;

  DefaultLoadTestModel(DefaultBuilder<T> builder) {
    this.applicationFactory = builder.applicationFactory;
    this.closeApplication = builder.closeApplication;
    this.titleFactory = builder.titleFactory;
    this.applicationTableModel = FilteredTableModel.builder(DefaultLoadTestModel::createApplicationTableModelColumns, new ApplicationColumnValueProvider())
            .itemSupplier(new ApplicationItemSupplier())
            .build();
    this.user = Value.value(builder.user, builder.user);
    this.loginDelayFactor = Value.value(builder.loginDelayFactor, builder.loginDelayFactor);
    this.applicationBatchSize = Value.value(builder.applicationBatchSize, builder.applicationBatchSize);
    this.minimumThinkTime = Value.value(builder.minimumThinkTime, builder.minimumThinkTime);
    this.maximumThinkTime = Value.value(builder.maximumThinkTime, builder.maximumThinkTime);
    this.loginDelayFactor.addValidator(new MinimumValidator(1));
    this.applicationBatchSize.addValidator(new MinimumValidator(1));
    this.minimumThinkTime.addValidator(new MinimumThinkTimeValidator());
    this.maximumThinkTime.addValidator(new MaximumThinkTimeValidator());
    this.usageScenarios = unmodifiableMap(builder.usageScenarios.stream()
            .collect(Collectors.toMap(UsageScenario::name, Function.identity())));
    this.scenarioChooser = createScenarioChooser();
    initializeChartModels();
    this.chartUpdateScheduler = TaskScheduler.builder(new ChartUpdateTask())
            .interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .build();
    this.applicationsRefreshScheduler = TaskScheduler.builder(new TableRefreshTask())
            .interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .start();
    bindEvents();
  }

  @Override
  public Value<User> user() {
    return user;
  }

  @Override
  public FilteredTableModel<Application, Integer> applicationTableModel() {
    return applicationTableModel;
  }

  @Override
  public String title() {
    return titleFactory.apply(this);
  }

  @Override
  public UsageScenario<T> usageScenario(String usageScenarioName) {
    UsageScenario<T> scenario = usageScenarios.get(requireNonNull(usageScenarioName));
    if (scenario == null) {
      throw new IllegalArgumentException("UsageScenario not found: " + usageScenarioName);
    }

    return scenario;
  }

  @Override
  public Collection<String> usageScenarios() {
    return usageScenarios.keySet();
  }

  @Override
  public void setWeight(String scenarioName, int weight) {
    scenarioChooser.setWeight(usageScenario(scenarioName), weight);
  }

  @Override
  public boolean isScenarioEnabled(String scenarioName) {
    return scenarioChooser.isItemEnabled(usageScenario(scenarioName));
  }

  @Override
  public void setScenarioEnabled(String scenarioName, boolean enabled) {
    scenarioChooser.setItemEnabled(usageScenario(scenarioName), enabled);
  }

  @Override
  public ItemRandomizer<UsageScenario<T>> scenarioChooser() {
    return scenarioChooser;
  }

  @Override
  public IntervalXYDataset scenarioDurationDataset(String name) {
    YIntervalSeriesCollection scenarioDurationCollection = new YIntervalSeriesCollection();
    scenarioDurationCollection.addSeries(durationSeries.get(name));

    return scenarioDurationCollection;
  }

  @Override
  public XYDataset thinkTimeDataset() {
    return thinkTimeCollection;
  }

  @Override
  public XYDataset numberOfApplicationsDataset() {
    return numberOfApplicationsCollection;
  }

  @Override
  public XYDataset usageScenarioDataset() {
    return usageScenarioCollection;
  }

  @Override
  public XYDataset usageScenarioFailureDataset() {
    return scenarioFailureCollection;
  }

  @Override
  public XYDataset memoryUsageDataset() {
    return memoryUsageCollection;
  }

  @Override
  public XYDataset systemLoadDataset() {
    return systemLoadCollection;
  }

  @Override
  public void clearChartData() {
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
  public int getUpdateInterval() {
    return chartUpdateScheduler.getInterval();
  }

  @Override
  public void setUpdateInterval(int updateInterval) {
    chartUpdateScheduler.setInterval(updateInterval);
  }

  @Override
  public Value<Integer> applicationBatchSize() {
    return applicationBatchSize;
  }

  @Override
  public void addApplicationBatch() {
    synchronized (applications) {
      int batchSize = applicationBatchSize.get();
      for (int i = 0; i < batchSize; i++) {
        int initialDelay = thinkTime();
        if (loginDelayFactor.get() > 0) {
          initialDelay *= loginDelayFactor.get();
        }
        ApplicationRunner applicationRunner = new ApplicationRunner(user.get(), applicationFactory);
        synchronized (applications) {
          applications.add(applicationRunner);
          applicationCount.set(applications.size());
        }
        scheduledExecutor.schedule(applicationRunner, initialDelay, TimeUnit.MILLISECONDS);
      }
    }
  }

  @Override
  public void removeApplicationBatch() {
    synchronized (applications) {
      if (!applications.isEmpty()) {
        int batchSize = applicationBatchSize.get();
        applicationCount.set(Math.max(0, applicationCount.get() - batchSize));
        List<ApplicationRunner> toStop = applications.stream()
                .filter(applicationRunner -> !applicationRunner.stopped())
                .limit(batchSize)
                .collect(toList());
        toStop.forEach(this::stop);
      }
    }
  }

  @Override
  public State paused() {
    return paused;
  }

  @Override
  public State collectChartData() {
    return collectChartData;
  }

  @Override
  public State autoRefreshApplications() {
    return autoRefreshApplications;
  }

  @Override
  public void shutdown() {
    shuttingDown = true;
    chartUpdateScheduler.stop();
    scheduledExecutor.shutdown();
    synchronized (applications) {
      new ArrayList<>(applications).forEach(this::stop);
    }
    try {
      scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    shutdownEvent.run();
  }

  @Override
  public Value<Integer> maximumThinkTime() {
    return maximumThinkTime;
  }

  @Override
  public Value<Integer> minimumThinkTime() {
    return minimumThinkTime;
  }

  @Override
  public Value<Integer> loginDelayFactor() {
    return loginDelayFactor;
  }

  @Override
  public ValueObserver<Integer> applicationCount() {
    return applicationCount.observer();
  }

  @Override
  public void addShutdownListener(Runnable listener) {
    shutdownEvent.addListener(listener);
  }

  /**
   * @return a random think time in milliseconds based on the values of minimumThinkTime and maximumThinkTime
   * @see #minimumThinkTime()
   * @see #maximumThinkTime()
   */
  private int thinkTime() {
    int time = minimumThinkTime.get() - maximumThinkTime.get();
    return time > 0 ? RANDOM.nextInt(time) + minimumThinkTime.get() : minimumThinkTime.get();
  }

  private ItemRandomizer<UsageScenario<T>> createScenarioChooser() {
    return ItemRandomizer.itemRandomizer(usageScenarios.values().stream()
            .map(scenario -> ItemRandomizer.RandomItem.randomItem(scenario, scenario.defaultWeight()))
            .collect(toList()));
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
    chartUpdateSchedulerEnabled.addDataListener(new TaskSchedulerController(chartUpdateScheduler));
    applicationsRefreshSchedulerEnabled.addDataListener(new TaskSchedulerController(applicationsRefreshScheduler));
  }

  private void stop(ApplicationRunner applicationRunner) {
    applicationRunner.stop();
    synchronized (applications) {
      applications.remove(applicationRunner);
      applicationCount.set(applications.size());
    }
    if (applicationRunner.application != null) {
      closeApplication.accept(applicationRunner.application);
      LOG.debug("LoadTestModel disconnected application: {}", applicationRunner.application);
    }
  }

  private static List<FilteredTableColumn<Integer>> createApplicationTableModelColumns() {
    return Arrays.asList(
            FilteredTableColumn.builder(ApplicationColumnValueProvider.APPLICATION)
                    .headerValue("Application")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.USERNAME)
                    .headerValue("User")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.SCENARIO)
                    .headerValue("Scenario")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.SUCCESSFUL)
                    .headerValue("Success")
                    .columnClass(Boolean.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.DURATION)
                    .headerValue("Duration (ms)")
                    .columnClass(Integer.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.EXCEPTION)
                    .headerValue("Exception")
                    .columnClass(Throwable.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationColumnValueProvider.CREATED)
                    .headerValue("Created")
                    .columnClass(LocalDateTime.class)
                    .build()
    );
  }

  private static double systemCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  private static double processCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  private final class ApplicationRunner implements Runnable {

    private static final int MAX_RESULTS = 20;

    private final User user;
    private final Function<User, T> applicationFactory;
    private final List<RunResult> runResults = new ArrayList<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final LocalDateTime created = LocalDateTime.now();

    private T application;

    private ApplicationRunner(User user, Function<User, T> applicationFactory) {
      this.user = user;
      this.applicationFactory = applicationFactory;
    }

    @Override
    public void run() {
      try {
        if (application == null && !stopped.get() && !paused.get()) {
          application = initializeApplication();
          if (application != null) {
            LOG.debug("LoadTestModel initialized application: {}", application);
          }
        }
        else {
          if (!stopped.get() && !paused.get()) {
            runScenario(application, scenarioChooser.randomItem());
          }
        }
        if (!stopped.get()) {
          scheduledExecutor.schedule(this, thinkTime(), TimeUnit.MILLISECONDS);
        }
      }
      catch (Exception e) {
        LOG.debug("Exception during run " + application, e);
      }
    }

    private T initializeApplication() {
      try {
        long startTime = System.currentTimeMillis();
        T application = applicationFactory.apply(user);
        int duration = (int) (System.currentTimeMillis() - startTime);
        addRunResult(new AbstractUsageScenario.DefaultRunResult("Initialization", duration, null));

        return application;
      }
      catch (Exception e) {
        addRunResult(new AbstractUsageScenario.DefaultRunResult("Initialization", -1, e));
        return null;
      }
    }

    private void runScenario(T application, UsageScenario<T> scenario) {
      RunResult runResult = scenario.run(application);
      counter.addScenarioDuration(scenario, runResult.duration());
      addRunResult(runResult);
    }

    private void addRunResult(RunResult runResult) {
      synchronized (runResults) {
        runResults.add(runResult);
        if (runResults.size() > MAX_RESULTS) {
          runResults.remove(0);
        }
      }
    }

    private void stop() {
      stopped.set(true);
    }

    private boolean stopped() {
      return stopped.get();
    }

    private LocalDateTime created() {
      return created;
    }
  }

  private final class TableRefreshTask implements Runnable {

    @Override
    public void run() {
      if (!shuttingDown) {
        SwingUtilities.invokeLater(applicationTableModel::refresh);
      }
    }
  }

  private final class ChartUpdateTask implements Runnable {

    @Override
    public void run() {
      if (!shuttingDown) {
        counter.updateRequestsPerSecond();
        updateChartData();
      }
    }

    private void updateChartData() {
      long time = System.currentTimeMillis();
      delayedScenarioRunsSeries.add(time, counter.delayedWorkRequestsPerSecond());
      minimumThinkTimeSeries.add(time, minimumThinkTime.get());
      maximumThinkTimeSeries.add(time, maximumThinkTime.get());
      synchronized (applications) {
        numberOfApplicationsSeries.add(time, applications.size());
      }
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
        scenarioDurations.computeIfAbsent(scenario.name(), scenarioName -> new ArrayList<>()).add(duration);
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

  static final class DefaultBuilder<T> implements Builder<T> {

    private final Function<User, T> applicationFactory;
    private final List<UsageScenario<T>> usageScenarios = new ArrayList<>();
    private final Consumer<T> closeApplication;

    private User user;
    private int minimumThinkTime = 2500;
    private int maximumThinkTime = 5000;
    private int loginDelayFactor = 2;
    private int applicationBatchSize = 10;
    private Function<LoadTestModel<T>, String> titleFactory = new DefaultTitleFactory<>();

    DefaultBuilder(Function<User, T> applicationFactory, Consumer<T> closeApplication) {
      this.applicationFactory = requireNonNull(applicationFactory);
      this.closeApplication = requireNonNull(closeApplication);
    }

    @Override
    public Builder<T> user(User user) {
      this.user = user;
      return this;
    }

    @Override
    public Builder<T> minimumThinkTime(int minimumThinkTime) {
      if (minimumThinkTime <= 0) {
        throw new IllegalArgumentException("Minimum think time must be a positive integer");
      }
      if (minimumThinkTime > maximumThinkTime) {
        throw new IllegalArgumentException("Minimum think time must be less than maximum think time");
      }
      this.minimumThinkTime = minimumThinkTime;
      return this;
    }

    @Override
    public Builder<T> maximumThinkTime(int maximumThinkTime) {
      if (maximumThinkTime <= 0) {
        throw new IllegalArgumentException("Maximum think time must be a positive integer");
      }
      if (maximumThinkTime < minimumThinkTime) {
        throw new IllegalArgumentException("Maximum think time must be greater than than minimum think time");
      }
      this.maximumThinkTime = maximumThinkTime;
      return this;
    }

    @Override
    public Builder<T> loginDelayFactor(int loginDelayFactor) {
      if (loginDelayFactor < 1) {
        throw new IllegalArgumentException("Login delay factor must be greatar than or equal to one");
      }
      this.loginDelayFactor = loginDelayFactor;
      return this;
    }

    @Override
    public Builder<T> applicationBatchSize(int applicationBatchSize) {
      if (loginDelayFactor < 1) {
        throw new IllegalArgumentException("Application batch size must be greatar than or equal to one");
      }
      this.applicationBatchSize = applicationBatchSize;
      return this;
    }

    @Override
    public Builder<T> usageScenarios(Collection<? extends UsageScenario<T>> usageScenarios) {
      this.usageScenarios.addAll(usageScenarios);
      return this;
    }

    @Override
    public Builder<T> titleFactory(Function<LoadTestModel<T>, String> titleFactory) {
      this.titleFactory = requireNonNull(titleFactory);
      return this;
    }

    @Override
    public LoadTestModel<T> build() {
      return new DefaultLoadTestModel<T>(this);
    }

    private static final class DefaultTitleFactory<T> implements Function<LoadTestModel<T>, String> {
      @Override
      public String apply(LoadTestModel<T> loadTest) {
        return loadTest.getClass().getSimpleName();
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
      if (value > maximumThinkTime.get()) {
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
      if (value < minimumThinkTime.get()) {
        throw new IllegalArgumentException("Maximum think time must be equal to or exceed minimum think time");
      }
    }
  }

  private static final class TaskSchedulerController implements Consumer<Boolean> {

    private final TaskScheduler taskScheduler;

    private TaskSchedulerController(TaskScheduler taskScheduler) {
      this.taskScheduler = taskScheduler;
    }

    @Override
    public void accept(Boolean enabled) {
      if (enabled) {
        taskScheduler.start();
      }
      else {
        taskScheduler.stop();
      }
    }
  }

  private static final class DefaultApplication implements Application {

    private final DefaultLoadTestModel<?>.ApplicationRunner applicationRunner;
    private final List<RunResult> runResults;
    private final String user;
    private final String scenario;
    private final Boolean successful;
    private final int duration;
    private final String exception;
    private final LocalDateTime created;

    private DefaultApplication(DefaultLoadTestModel<?>.ApplicationRunner applicationRunner) {
      this.applicationRunner = applicationRunner;
      this.runResults = applicationRunner.runResults == null ? emptyList() : applicationRunner.runResults;
      RunResult runResult = runResults.isEmpty() ? null : runResults.get(runResults.size() - 1);
      this.user = applicationRunner.user.username();
      this.scenario = runResult == null ? null : runResult.scenario();
      this.successful = runResult == null ? null : runResult.successful();
      this.duration = runResult == null ? null : runResult.duration();
      this.exception = runResult == null ? null : runResult.exception().map(Throwable::getMessage).orElse(null);
      this.created = applicationRunner.created();
    }

    @Override
    public String name() {
      return applicationRunner.application == null ? "Not initialized" : applicationRunner.application.toString();
    }

    @Override
    public String username() {
      return user;
    }

    @Override
    public String scenario() {
      return scenario;
    }

    @Override
    public Boolean successful() {
      return successful;
    }

    @Override
    public Integer duration() {
      return duration == -1 ? null : duration;
    }

    @Override
    public String exception() {
      return exception;
    }

    @Override
    public LocalDateTime created() {
      return created;
    }
  }

  private final class ApplicationItemSupplier implements Supplier<Collection<Application>> {

    @Override
    public Collection<Application> get() {
      synchronized (applications) {
        return applications.stream()
                .map(this::toApplication)
                .collect(toList());
      }
    }

    private Application toApplication(ApplicationRunner applicationRunner) {
      return new DefaultApplication(applicationRunner);
    }
  }

  private static final class ApplicationColumnValueProvider implements ColumnValueProvider<Application, Integer> {

    private static final int APPLICATION = 0;
    private static final int USERNAME = 1;
    private static final int SCENARIO = 2;
    private static final int SUCCESSFUL = 3;
    private static final int DURATION = 4;
    private static final int EXCEPTION = 5;
    private static final int CREATED = 6;

    @Override
    public Object value(Application application, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case APPLICATION:
          return application.name();
        case USERNAME:
          return application.username();
        case SCENARIO:
          return application.scenario();
        case SUCCESSFUL:
          return application.successful();
        case DURATION:
          return application.duration();
        case EXCEPTION:
          return application.exception();
        case CREATED:
          return application.created();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }
}
