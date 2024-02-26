/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.Memory;
import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario.Result;
import is.codion.common.model.randomizer.ItemRandomizer;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultLoadTestModel<T> implements LoadTestModel<T> {

  public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;
  private static final double HUNDRED = 100d;
  private static final double THOUSAND = 1000d;

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
  public void shutdown() {
    loadTest.shutdown();
  }

  @Override
  public void stop(ApplicationRunner applicationRunner) {
    loadTest.stop(applicationRunner);
  }

  @Override
  public Value<User> user() {
    return loadTest.user();
  }

  @Override
  public String title() {
    return loadTest.title();
  }

  @Override
  public void setWeight(String scenarioName, int weight) {
    loadTest.setWeight(scenarioName, weight);
  }

  @Override
  public boolean isScenarioEnabled(String scenarioName) {
    return loadTest.isScenarioEnabled(scenarioName);
  }

  @Override
  public void setScenarioEnabled(String scenarioName, boolean enabled) {
    loadTest.setScenarioEnabled(scenarioName, enabled);
  }

  @Override
  public Collection<Scenario<T>> scenarios() {
    return loadTest.scenarios();
  }

  @Override
  public Scenario<T> scenario(String scenarioName) {
    return loadTest.scenario(scenarioName);
  }

  @Override
  public void addResultListener(Consumer<Result> listener) {
    loadTest.addResultListener(listener);
  }

  @Override
  public void addShutdownListener(Runnable listener) {
    loadTest.addShutdownListener(listener);
  }

  @Override
  public Value<Integer> applicationBatchSize() {
    return loadTest.applicationBatchSize();
  }

  @Override
  public State paused() {
    return loadTest.paused();
  }

  @Override
  public Value<Integer> maximumThinkTime() {
    return loadTest.maximumThinkTime();
  }

  @Override
  public Value<Integer> minimumThinkTime() {
    return loadTest.minimumThinkTime();
  }

  @Override
  public Value<Integer> loginDelayFactor() {
    return loadTest.loginDelayFactor();
  }

  @Override
  public ValueObserver<Integer> applicationCount() {
    return loadTest.applicationCount();
  }

  @Override
  public void addApplicationBatch() {
    loadTest.addApplicationBatch();
  }

  @Override
  public void removeApplicationBatch() {
    loadTest.removeApplicationBatch();
  }

  @Override
  public void removeSelectedApplications() {
    applicationTableModel.selectionModel().getSelectedItems().stream()
            .map(DefaultApplicationRow.class::cast)
            .forEach(application -> loadTest.stop(application.applicationRunner));
  }

  @Override
  public ItemRandomizer<Scenario<T>> scenarioChooser() {
    return loadTest.scenarioChooser();
  }

  @Override
  public Map<ApplicationRunner, T> applications() {
    return loadTest.applications();
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
    loadTest.addResultListener(counter::addScenarioDuration);
    addShutdownListener(() -> {
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

    private void addScenarioDuration(Result result) {
      Scenario<T> scenario = loadTest.scenario(result.scenario());
      synchronized (scenarioDurations) {
        scenarioDurations.computeIfAbsent(scenario.name(), scenarioName -> new ArrayList<>()).add(result.duration());
        workRequestCounter++;
        if (scenario.maximumTime() > 0 && result.duration() > scenario.maximumTime()) {
          delayedWorkRequestCounter++;
        }
      }
    }

    private void updateRequestsPerSecond() {
      long current = System.currentTimeMillis();
      double elapsedSeconds = (current - time) / THOUSAND;
      if (elapsedSeconds > UPDATE_INTERVAL) {
        scenarioAvgDurations.clear();
        scenarioMinDurations.clear();
        scenarioMaxDurations.clear();
        workRequestsPerSecond = workRequestCounter / elapsedSeconds;
        delayedWorkRequestsPerSecond = (int) (delayedWorkRequestCounter / elapsedSeconds);
        for (Scenario<T> scenario : loadTest.scenarios()) {
          scenarioRates.put(scenario.name(), (int) (scenario.totalRunCount() / elapsedSeconds));
          scenarioFailures.put(scenario.name(), scenario.unsuccessfulRunCount());
          calculateScenarioDuration(scenario);
        }
        resetCounters();
        time = current;
      }
    }

    private void calculateScenarioDuration(Scenario<T> scenario) {
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
          scenarioAvgDurations.put(scenario.name(), totalDuration / durations.size());
          scenarioMinDurations.put(scenario.name(), minDuration);
          scenarioMaxDurations.put(scenario.name(), maxDuration);
        }
      }
    }

    private void resetCounters() {
      for (Scenario<T> scenario : scenarios()) {
        scenario.resetRunCount();
      }
      workRequestCounter = 0;
      delayedWorkRequestCounter = 0;
      synchronized (scenarioDurations) {
        scenarioDurations.clear();
      }
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
