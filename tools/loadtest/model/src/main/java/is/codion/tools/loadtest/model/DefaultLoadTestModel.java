/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.model;

import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.LoadTest.ApplicationRunner;
import is.codion.tools.loadtest.Scenario;
import is.codion.tools.loadtest.Scenario.Result;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultLoadTestModel<T> implements LoadTestModel<T> {

	public static final int DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS = 2000;
	private static final double HUNDRED = 100d;
	private static final double THOUSAND = 1000d;
	private static final double K = 1024;
	private static final int MAXIMUM_EXCEPTIONS = 20;
	private static final AtomicInteger ZERO = new AtomicInteger();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private final LoadTest<T> loadTest;

	private final FilterTableModel<ApplicationRow, String> applicationTableModel;
	private final Counter counter = new Counter();

	private final State collectChartData = State.state();
	private final State autoRefreshApplications = State.state(true);
	private final ObservableState chartUpdateSchedulerEnabled;
	private final ObservableState applicationsRefreshSchedulerEnabled;
	private final TaskScheduler chartUpdateScheduler;
	private final TaskScheduler applicationsRefreshScheduler;

	private final XYSeries scenariosRunSeries = new XYSeries("Total");

	private final XYSeriesCollection scenarioFailureCollection = new XYSeriesCollection();

	private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
	private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
	private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

	private final XYSeries numberOfApplicationsSeries = new XYSeries("Application count");
	private final XYSeriesCollection numberOfApplicationsCollection = new XYSeriesCollection();

	private final XYSeriesCollection scenarioCollection = new XYSeriesCollection();

	private final XYSeries totalMemoryCollection = new XYSeries("Total");
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
		applicationTableModel = FilterTableModel.builder()
						.columns(new ApplicationColumns())
						.items(new ApplicationItems())
						.build();
		chartUpdateSchedulerEnabled = State.and(loadTest.paused().not(), collectChartData);
		applicationsRefreshSchedulerEnabled = State.and(autoRefreshApplications);
		initializeChartModels();
		chartUpdateScheduler = TaskScheduler.builder()
						.task(new ChartUpdateTask())
						.interval(DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
						.build();
		applicationsRefreshScheduler = TaskScheduler.builder()
						.task(applicationTableModel.items()::refresh)
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
		applicationTableModel.selection().items().get().stream()
						.map(DefaultApplicationRow.class::cast)
						.forEach(application -> loadTest.stop(application.applicationRunner));
	}

	@Override
	public FilterTableModel<ApplicationRow, String> applicationTableModel() {
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
		minimumThinkTimeSeries.clear();
		maximumThinkTimeSeries.clear();
		numberOfApplicationsSeries.clear();
		totalMemoryCollection.clear();
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
			return counter.scenarioRunCounts.getOrDefault(scenarioName, ZERO).get() +
							counter.scenarioFailureCounts.getOrDefault(scenarioName, ZERO).get();
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
	public List<ScenarioException> exceptions(String scenarioName) {
		synchronized (counter) {
			List<ScenarioException> exceptions = counter.scenarioExceptions.get(scenarioName);
			return exceptions == null ? emptyList() : new ArrayList<>(exceptions);
		}
	}

	@Override
	public void clearExceptions(String scenarioName) {
		synchronized (counter) {
			List<ScenarioException> exceptions = counter.scenarioExceptions.get(scenarioName);
			if (exceptions != null) {
				exceptions.clear();
			}
		}
	}

	@Override
	public int getUpdateInterval() {
		return chartUpdateScheduler.interval().getOrThrow();
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
		memoryUsageCollection.addSeries(totalMemoryCollection);
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
	}

	private void bindEvents() {
		loadTest.result().addConsumer(counter::addScenarioResults);
		loadTest.addShutdownListener(() -> {
			applicationsRefreshScheduler.stop();
			chartUpdateScheduler.stop();
		});
		chartUpdateSchedulerEnabled.addConsumer(new TaskSchedulerController(chartUpdateScheduler));
		applicationsRefreshSchedulerEnabled.addConsumer(new TaskSchedulerController(applicationsRefreshScheduler));
	}

	private static double systemCpuLoad() {
		return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getCpuLoad();
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
			minimumThinkTimeSeries.add(time, loadTest.minimumThinkTime().get());
			maximumThinkTimeSeries.add(time, loadTest.maximumThinkTime().get());
			numberOfApplicationsSeries.add(time, loadTest.applicationCount().get());
			totalMemoryCollection.add(time, RUNTIME.totalMemory() / K / K);
			usedMemoryCollection.add(time, (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / K / K);
			maxMemoryCollection.add(time, RUNTIME.maxMemory() / K / K);
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

		private final Map<String, Integer> scenarioRates = new HashMap<>();
		private final Map<String, Long> scenarioAvgDurations = new HashMap<>();
		private final Map<String, Long> scenarioMaxDurations = new HashMap<>();
		private final Map<String, Long> scenarioMinDurations = new HashMap<>();
		private final Map<String, Integer> scenarioFailures = new HashMap<>();
		private final Map<String, List<Long>> scenarioDurations = new HashMap<>();
		private final Map<String, AtomicInteger> scenarioRunCounts = new HashMap<>();
		private final Map<String, AtomicInteger> scenarioFailureCounts = new HashMap<>();
		private final Map<String, List<ScenarioException>> scenarioExceptions = new HashMap<>();
		private final AtomicInteger workRequestCounter = new AtomicInteger();

		private double workRequestsPerSecond = 0;
		private long time = System.currentTimeMillis();

		private double workRequestsPerSecond() {
			return workRequestsPerSecond;
		}

		private long minimumScenarioDuration(String scenarioName) {
			if (!scenarioMinDurations.containsKey(scenarioName)) {
				return 0;
			}

			return scenarioMinDurations.get(scenarioName);
		}

		private long maximumScenarioDuration(String scenarioName) {
			if (!scenarioMaxDurations.containsKey(scenarioName)) {
				return 0;
			}

			return scenarioMaxDurations.get(scenarioName);
		}

		private long averageScenarioDuration(String scenarioName) {
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
					List<ScenarioException> exceptions = scenarioExceptions.computeIfAbsent(scenario.name(), scenarioName -> new ArrayList<>());
					exceptions.add(new DefaultScenarioException(result.started(), exception));
					if (exceptions.size() > MAXIMUM_EXCEPTIONS) {
						exceptions.remove(0);
					}
				});
			}
			workRequestCounter.incrementAndGet();
		}

		private synchronized void updateRequestsPerSecond() {
			long current = System.currentTimeMillis();
			double elapsedSeconds = (current - time) / THOUSAND;
			if (elapsedSeconds > UPDATE_INTERVAL) {
				scenarioAvgDurations.clear();
				scenarioMinDurations.clear();
				scenarioMaxDurations.clear();
				workRequestsPerSecond = workRequestCounter.get() / elapsedSeconds;
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
			Collection<Long> durations = scenarioDurations.getOrDefault(scenario.name(), emptyList());
			if (!durations.isEmpty()) {
				long totalDuration = 0;
				long minDuration = -1;
				long maxDuration = -1;
				for (Long duration : durations) {
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
			scenarioDurations.clear();
			scenarioRunCounts.clear();
			scenarioFailureCounts.clear();
			scenarioExceptions.clear();
		}
	}

	private final class ApplicationItems implements Supplier<Collection<ApplicationRow>> {

		@Override
		public Collection<ApplicationRow> get() {
			return loadTest.applications().keySet().stream()
							.map(DefaultApplicationRow::new)
							.collect(toList());
		}
	}

	private static final class DefaultScenarioException implements ScenarioException {

		private final long timestamp;
		private final Exception exception;

		private DefaultScenarioException(long timestamp, Exception exception) {
			this.timestamp = timestamp;
			this.exception = exception;
		}

		@Override
		public long timestamp() {
			return timestamp;
		}

		@Override
		public Exception exception() {
			return exception;
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

	public static final class ApplicationColumns implements TableColumns<ApplicationRow, String> {

		private static final List<String> IDENTIFIERS = unmodifiableList(Arrays.asList(
						ApplicationRow.NAME, ApplicationRow.USERNAME, ApplicationRow.SCENARIO,
						ApplicationRow.SUCCESSFUL, ApplicationRow.DURATION, ApplicationRow.EXCEPTION,
						ApplicationRow.MESSAGE, ApplicationRow.CREATED
		));

		@Override
		public List<String> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			switch (identifier) {
				case ApplicationRow.NAME:
					return String.class;
				case ApplicationRow.USERNAME:
					return String.class;
				case ApplicationRow.SCENARIO:
					return String.class;
				case ApplicationRow.SUCCESSFUL:
					return Boolean.class;
				case ApplicationRow.DURATION:
					return Long.class;
				case ApplicationRow.EXCEPTION:
					return String.class;
				case ApplicationRow.MESSAGE:
					return String.class;
				case ApplicationRow.CREATED:
					return LocalDateTime.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Object value(ApplicationRow application, String identifier) {
			List<Result> results = application.results();
			Result result = results.isEmpty() ? null : results.get(results.size() - 1);
			Throwable exception = result == null ? null : result.exception().orElse(null);
			switch (identifier) {
				case ApplicationRow.NAME:
					return application.name();
				case ApplicationRow.USERNAME:
					return application.username();
				case ApplicationRow.SCENARIO:
					return result == null ? null : result.scenario();
				case ApplicationRow.SUCCESSFUL:
					return result == null ? null : result.successful();
				case ApplicationRow.DURATION:
					return result == null || result.duration() == -1 ? null : result.duration();
				case ApplicationRow.EXCEPTION:
					return exception;
				case ApplicationRow.MESSAGE:
					return exception == null ? null : exception.getMessage();
				case ApplicationRow.CREATED:
					return application.created();
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
