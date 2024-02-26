/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.Memory;
import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.ApplicationRunner;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.model.loadtest.LoadTest.Scenario.Result;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultLoadTestModel<T> implements LoadTestModel<T> {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;
  private static final double HUNDRED = 100d;
  private static final double THOUSAND = 1000d;
  private static final int MAXIMUM_EXCEPTIONS = 20;
  private static final AtomicInteger ZERO = new AtomicInteger();

  private final LoadTest<T> loadTest;

  private final FilteredTableModel<ApplicationRow, Integer> applicationTableModel;
  private final Counter counter = new Counter();

  private final State collectChartData = State.state();
  private final State autoRefreshApplications = State.state(true);
  private final StateObserver chartUpdateSchedulerEnabled;
  private final StateObserver applicationsRefreshSchedulerEnabled;
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

  private final XYSeriesCollection scenarioCollection = new XYSeriesCollection();

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

  DefaultLoadTestModel(LoadTest<T> loadTest) {
    this.loadTest = requireNonNull(loadTest);
    applicationTableModel = FilteredTableModel.builder(DefaultLoadTestModel::createApplicationTableModelColumns, new ApplicationColumnValueProvider())
            .itemSupplier(new ApplicationRowSupplier())
            .build();
    chartUpdateSchedulerEnabled = State.and(loadTest.paused().not(), collectChartData);
    applicationsRefreshSchedulerEnabled = State.and(loadTest.paused().not(), autoRefreshApplications);
    initializeChartModels();
    chartUpdateScheduler = TaskScheduler.builder(new ChartUpdateTask())
            .interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .build();
    applicationsRefreshScheduler = TaskScheduler.builder(applicationTableModel::refresh)
            .interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .start();
    bindEvents();
  }

  @Override
  public LoadTest<T> loadTest() {
    return loadTest;
  }

  @Override
  public void removeSelectedApplications() {
    applicationTableModel.selectionModel().getSelectedItems().stream()
            .map(DefaultApplicationRow.class::cast)
            .forEach(application -> loadTest.stop(application.applicationRunner));
  }

  @Override
  public FilteredTableModel<ApplicationRow, Integer> applicationTableModel() {
    return applicationTableModel;
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
  public XYDataset scenarioDataset() {
    return scenarioCollection;
  }

  @Override
  public XYDataset scenarioFailureDataset() {
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
  public void clearCharts() {
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
  public int totalRunCount(String scenarioName) {
    synchronized (counter) {
      return counter.scenarioRunCounts.getOrDefault(scenarioName, ZERO).get() + counter.scenarioFailureCounts.getOrDefault(scenarioName, ZERO).get();
    }
  }

  @Override
  public int successfulRunCount(String scenarioName) {
    synchronized (counter) {
      return counter.scenarioRunCounts.getOrDefault(scenarioName, ZERO).get();
    }
  }

  @Override
  public int unsuccessfulRunCount(String scenarioName) {
    synchronized (counter) {
      return counter.scenarioFailureCounts.getOrDefault(scenarioName, ZERO).get();
    }
  }

  @Override
  public void resetRunCounter() {
    synchronized (counter) {
      counter.resetCounters();
    }
  }

  @Override
  public List<Throwable> exceptions(String scenarioName) {
    synchronized (counter) {
      Collection<Throwable> exceptions = counter.scenarioExceptions.get(scenarioName);
      return exceptions == null ? Collections.emptyList() : new ArrayList<>(exceptions);
    }
  }

  @Override
  public void clearExceptions(String scenarioName) {
    synchronized (counter) {
      Collection<Throwable> exceptions = counter.scenarioExceptions.get(scenarioName);
      if (exceptions != null) {
        exceptions.clear();
      }
    }
  }

  @Override
  public int getUpdateInterval() {
    return chartUpdateScheduler.interval().get();
  }

  @Override
  public void setUpdateInterval(int updateInterval) {
    chartUpdateScheduler.interval().set(updateInterval);
  }

  @Override
  public State collectChartData() {
    return collectChartData;
  }

  @Override
  public State autoRefreshApplications() {
    return autoRefreshApplications;
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
    scenarioCollection.addSeries(scenariosRunSeries);
    for (Scenario<T> scenario : loadTest.scenarios()) {
      XYSeries series = new XYSeries(scenario.name());
      scenarioCollection.addSeries(series);
      usageSeries.add(series);
      YIntervalSeries avgDurSeries = new YIntervalSeries(scenario.name());
      durationSeries.put(scenario.name(), avgDurSeries);
      XYSeries failSeries = new XYSeries(scenario.name());
      scenarioFailureCollection.addSeries(failSeries);
      failureSeries.add(failSeries);
    }
    scenarioCollection.addSeries(delayedScenarioRunsSeries);
  }

  private void bindEvents() {
    loadTest.addResultListener(counter::addScenarioResults);
    loadTest.addShutdownListener(() -> {
      applicationsRefreshScheduler.stop();
      chartUpdateScheduler.stop();
    });
    chartUpdateSchedulerEnabled.addDataListener(new TaskSchedulerController(chartUpdateScheduler));
    applicationsRefreshSchedulerEnabled.addDataListener(new TaskSchedulerController(applicationsRefreshScheduler));
  }

  private static double systemCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  private static double processCpuLoad() {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  private final class ChartUpdateTask implements Runnable {

    @Override
    public void run() {
      counter.updateRequestsPerSecond();
      updateChartData();
    }

    private void updateChartData() {
      long time = System.currentTimeMillis();
      delayedScenarioRunsSeries.add(time, counter.delayedWorkRequestsPerSecond());
      minimumThinkTimeSeries.add(time, loadTest.minimumThinkTime().get());
      maximumThinkTimeSeries.add(time, loadTest.maximumThinkTime().get());
      numberOfApplicationsSeries.add(time, loadTest.applicationCount().get());
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

  private static List<FilteredTableColumn<Integer>> createApplicationTableModelColumns() {
    return Arrays.asList(
            FilteredTableColumn.builder(ApplicationRow.NAME_INDEX)
                    .headerValue("Name")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.USERNAME_INDEX)
                    .headerValue("User")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.SCENARIO_INDEX)
                    .headerValue("Scenario")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.SUCCESSFUL_INDEX)
                    .headerValue("Success")
                    .columnClass(Boolean.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.DURATION_INDEX)
                    .headerValue("Duration (μs)")
                    .columnClass(Integer.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.EXCEPTION_INDEX)
                    .headerValue("Exception")
                    .columnClass(Throwable.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.MESSAGE_INDEX)
                    .headerValue("Message")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(ApplicationRow.CREATED_INDEX)
                    .headerValue("Created")
                    .columnClass(LocalDateTime.class)
                    .build()
    );
  }

  private final class Counter {

    private static final int UPDATE_INTERVAL = 5;

    private final Map<String, Integer> scenarioRates = new HashMap<>();
    private final Map<String, Integer> scenarioAvgDurations = new HashMap<>();
    private final Map<String, Integer> scenarioMaxDurations = new HashMap<>();
    private final Map<String, Integer> scenarioMinDurations = new HashMap<>();
    private final Map<String, Integer> scenarioFailures = new HashMap<>();
    private final Map<String, List<Integer>> scenarioDurations = new HashMap<>();
    private final Map<String, AtomicInteger> scenarioRunCounts = new HashMap<>();
    private final Map<String, AtomicInteger> scenarioFailureCounts = new HashMap<>();
    private final Map<String, List<Throwable>> scenarioExceptions = new HashMap<>();

    private final AtomicInteger workRequestCounter = new AtomicInteger();
    private final AtomicInteger delayedWorkRequestsPerSecond = new AtomicInteger();
    private final AtomicInteger delayedWorkRequestCounter = new AtomicInteger();

    private double workRequestsPerSecond = 0;
    private long time = System.currentTimeMillis();

    private double workRequestsPerSecond() {
      return workRequestsPerSecond;
    }

    private int delayedWorkRequestsPerSecond() {
      return delayedWorkRequestsPerSecond.get();
    }

    private int minimumScenarioDuration(String scenarioName) {
      if (!scenarioMinDurations.containsKey(scenarioName)) {
        return 0;
      }

      return scenarioMinDurations.get(scenarioName);
    }

    private int maximumScenarioDuration(String scenarioName) {
      if (!scenarioMaxDurations.containsKey(scenarioName)) {
        return 0;
      }

      return scenarioMaxDurations.get(scenarioName);
    }

    private int averageScenarioDuration(String scenarioName) {
      if (!scenarioAvgDurations.containsKey(scenarioName)) {
        return 0;
      }

      return scenarioAvgDurations.get(scenarioName);
    }

    private double scenarioFailureRate(String scenarioName) {
      if (!scenarioFailures.containsKey(scenarioName)) {
        return 0;
      }

      return scenarioFailures.get(scenarioName);
    }

    private int scenarioRate(String scenarioName) {
      if (!scenarioRates.containsKey(scenarioName)) {
        return 0;
      }

      return scenarioRates.get(scenarioName);
    }

    private synchronized void addScenarioResults(Result result) {
      Scenario<T> scenario = loadTest.scenario(result.scenario());
      scenarioDurations.computeIfAbsent(scenario.name(), scenarioName -> new ArrayList<>()).add(result.duration());
      if (result.successful()) {
        scenarioRunCounts.computeIfAbsent(scenario.name(), scenarioName -> new AtomicInteger()).incrementAndGet();
      }
      else {
        scenarioFailureCounts.computeIfAbsent(scenario.name(), scenarioName -> new AtomicInteger()).incrementAndGet();
        result.exception().ifPresent(exception -> {
          List<Throwable> exceptions = scenarioExceptions.computeIfAbsent(scenario.name(), scenarioName -> new ArrayList<>());
          exceptions.add(exception);
          if (exceptions.size() > MAXIMUM_EXCEPTIONS) {
            exceptions.remove(0);
          }
        });
      }
      workRequestCounter.incrementAndGet();
      if (scenario.maximumTime() > 0 && result.duration() > scenario.maximumTime()) {
        delayedWorkRequestCounter.incrementAndGet();
      }
    }

    private synchronized void updateRequestsPerSecond() {
      long current = System.currentTimeMillis();
      double elapsedSeconds = (current - time) / THOUSAND;
      if (elapsedSeconds > UPDATE_INTERVAL) {
        scenarioAvgDurations.clear();
        scenarioMinDurations.clear();
        scenarioMaxDurations.clear();
        workRequestsPerSecond = workRequestCounter.get() / elapsedSeconds;
        delayedWorkRequestsPerSecond.set((int) (delayedWorkRequestCounter.get() / elapsedSeconds));
        for (Scenario<T> scenario : loadTest.scenarios()) {
          scenarioRates.put(scenario.name(), (int) (scenarioRunCounts.getOrDefault(scenario.name(), ZERO).get() / elapsedSeconds));
          scenarioFailures.put(scenario.name(), scenarioFailureCounts.getOrDefault(scenario.name(), ZERO).get());
          calculateScenarioDuration(scenario);
        }
        resetCounters();
        time = current;
      }
    }

    private void calculateScenarioDuration(Scenario<T> scenario) {
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
        scenarioAvgDurations.put(scenario.name(), totalDuration / durations.size());
        scenarioMinDurations.put(scenario.name(), minDuration);
        scenarioMaxDurations.put(scenario.name(), maxDuration);
      }
    }

    private synchronized void resetCounters() {
      workRequestCounter.set(0);
      delayedWorkRequestCounter.set(0);
      scenarioDurations.clear();
      scenarioRunCounts.clear();
      scenarioFailureCounts.clear();
      scenarioExceptions.clear();
    }
  }

  private final class ApplicationRowSupplier implements Supplier<Collection<ApplicationRow>> {

    @Override
    public Collection<ApplicationRow> get() {
      return loadTest.applications().keySet().stream()
              .map(DefaultApplicationRow::new)
              .collect(toList());
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

  private static final class DefaultApplicationRow implements ApplicationRow {

    private final ApplicationRunner applicationRunner;
    private final String user;
    private final LocalDateTime created;
    private final List<Result> results;

    private DefaultApplicationRow(ApplicationRunner applicationRunner) {
      this.applicationRunner = applicationRunner;
      this.user = applicationRunner.user().username();
      this.created = applicationRunner.created();
      this.results = applicationRunner.results();
    }

    @Override
    public String name() {
      return applicationRunner.name();
    }

    @Override
    public String username() {
      return user;
    }

    @Override
    public LocalDateTime created() {
      return created;
    }

    @Override
    public List<Result> results() {
      return results;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object instanceof DefaultLoadTestModel.DefaultApplicationRow)) {
        return false;
      }
      DefaultApplicationRow that = (DefaultApplicationRow) object;

      return applicationRunner == that.applicationRunner;
    }

    @Override
    public int hashCode() {
      return Objects.hash(applicationRunner);
    }
  }

  private static final class ApplicationColumnValueProvider implements ColumnValueProvider<ApplicationRow, Integer> {

    @Override
    public Object value(ApplicationRow application, Integer columnIdentifier) {
      List<Result> results = application.results();
      Result result = results.isEmpty() ? null : results.get(results.size() - 1);
      Throwable exception = result == null ? null : result.exception().orElse(null);
      switch (columnIdentifier) {
        case ApplicationRow.NAME_INDEX:
          return application.name();
        case ApplicationRow.USERNAME_INDEX:
          return application.username();
        case ApplicationRow.SCENARIO_INDEX:
          return result == null ? null : result.scenario();
        case ApplicationRow.SUCCESSFUL_INDEX:
          return result == null ? null : result.successful();
        case ApplicationRow.DURATION_INDEX:
          return result == null ? null : result.duration();
        case ApplicationRow.EXCEPTION_INDEX:
          return exception;
        case ApplicationRow.MESSAGE_INDEX:
          return exception == null ? null : exception.getMessage();
        case ApplicationRow.CREATED_INDEX:
          return application.created();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }
}
