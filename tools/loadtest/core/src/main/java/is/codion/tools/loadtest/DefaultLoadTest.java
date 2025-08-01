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
package is.codion.tools.loadtest;

import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.tools.loadtest.LoadTest.Scenario.Result;
import is.codion.tools.loadtest.randomizer.ItemRandomizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem.randomItem;
import static is.codion.tools.loadtest.randomizer.ItemRandomizer.itemRandomizer;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;

final class DefaultLoadTest<T> implements LoadTest<T> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultLoadTest.class);

	private static final Random RANDOM = new Random();
	private static final int MINIMUM_NUMBER_OF_THREADS = 12;

	private final Function<User, T> createApplication;
	private final Consumer<T> closeApplication;
	private final State paused = State.state();

	private final Value<Integer> loginDelayFactor;
	private final Value<Integer> applicationBatchSize;
	private final Value<Integer> maximumThinkTime;
	private final Value<Integer> minimumThinkTime;
	private final Value<Integer> applicationCount = Value.nullable(0);
	private final Event<?> shutdownEvent = Event.event();
	private final Event<Result> resultEvent = Event.event();

	private final String name;
	private final Value<User> user;

	private final Map<ApplicationRunner, T> applications = new HashMap<>();
	private final Map<String, Scenario<T>> scenarios;
	private final ItemRandomizer<Scenario<T>> scenarioChooser;
	private final ScheduledExecutorService scheduledExecutor =
					newScheduledThreadPool(Math.max(MINIMUM_NUMBER_OF_THREADS, Runtime.getRuntime().availableProcessors() * 2));

	DefaultLoadTest(DefaultBuilder<T> builder) {
		this.createApplication = builder.createApplication;
		this.closeApplication = builder.closeApplication;
		this.name = builder.name;
		this.user = Value.nonNull(builder.user);
		this.loginDelayFactor = Value.builder()
						.nonNull(builder.loginDelayFactor)
						.validator(new MinimumValidator(1))
						.build();
		this.applicationBatchSize = Value.builder()
						.nonNull(builder.applicationBatchSize)
						.validator(new MinimumValidator(1))
						.build();
		this.minimumThinkTime = Value.nonNull(builder.minimumThinkTime);
		this.maximumThinkTime = Value.nonNull(builder.maximumThinkTime);
		this.minimumThinkTime.addValidator(new MinimumThinkTimeValidator());
		this.maximumThinkTime.addValidator(new MaximumThinkTimeValidator());
		this.scenarios = unmodifiableMap(builder.scenarios.stream()
						.collect(Collectors.toMap(Scenario::name, Function.identity())));
		this.scenarioChooser = createScenarioChooser();
	}

	@Override
	public Value<User> user() {
		return user;
	}

	@Override
	public Optional<String> name() {
		return Optional.ofNullable(name);
	}

	@Override
	public Scenario<T> scenario(String scenarioName) {
		Scenario<T> scenario = scenarios.get(requireNonNull(scenarioName));
		if (scenario == null) {
			throw new IllegalArgumentException("Scenario not found: " + scenarioName);
		}

		return scenario;
	}

	@Override
	public Collection<Scenario<T>> scenarios() {
		return scenarios.values();
	}

	@Override
	public void setWeight(String scenarioName, int weight) {
		scenarioChooser.setWeight(scenario(scenarioName), weight);
	}

	@Override
	public boolean isScenarioEnabled(String scenarioName) {
		return scenarioChooser.isItemEnabled(scenario(scenarioName));
	}

	@Override
	public void setScenarioEnabled(String scenarioName, boolean enabled) {
		scenarioChooser.setItemEnabled(scenario(scenarioName), enabled);
	}

	@Override
	public ItemRandomizer<Scenario<T>> scenarioChooser() {
		return scenarioChooser;
	}

	@Override
	public Map<ApplicationRunner, T> applications() {
		synchronized (applications) {
			return new HashMap<>(applications);
		}
	}

	@Override
	public Value<Integer> applicationBatchSize() {
		return applicationBatchSize;
	}

	@Override
	public void addApplicationBatch() {
		synchronized (applications) {
			int batchSize = applicationBatchSize.getOrThrow();
			for (int i = 0; i < batchSize; i++) {
				DefaultApplicationRunner applicationRunner = new DefaultApplicationRunner(user.get(), createApplication);
				applications.put(applicationRunner, applicationRunner.application);
				applicationCount.set(applications.size());
				scheduledExecutor.schedule(applicationRunner, initialDelay(), TimeUnit.MILLISECONDS);
			}
		}
	}

	@Override
	public void removeApplicationBatch() {
		synchronized (applications) {
			if (!applications.isEmpty()) {
				applications.keySet().stream()
								.filter(applicationRunner -> !applicationRunner.stopped())
								.limit(applicationBatchSize.getOrThrow())
								.collect(toList())
								.forEach(this::stop);
			}
		}
	}

	@Override
	public State paused() {
		return paused;
	}

	@Override
	public void shutdown() {
		synchronized (applications) {
			new ArrayList<>(applications.keySet()).forEach(this::stop);
		}
		scheduledExecutor.shutdown();
		try {
			scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES);
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
	public Observable<Integer> applicationCount() {
		return applicationCount.observable();
	}

	@Override
	public void addShutdownListener(Runnable listener) {
		shutdownEvent.addListener(listener);
	}

	@Override
	public Observer<Result> result() {
		return resultEvent.observer();
	}

	private int initialDelay() {
		int time = maximumThinkTime.getOrThrow() - minimumThinkTime.getOrThrow();
		return time > 0 ? RANDOM.nextInt(time * loginDelayFactor.getOrThrow()) + minimumThinkTime.getOrThrow() : minimumThinkTime.getOrThrow();
	}

	private ItemRandomizer<Scenario<T>> createScenarioChooser() {
		return itemRandomizer(scenarios.values().stream()
						.map(scenario -> randomItem(scenario, scenario.defaultWeight()))
						.collect(toList()));
	}

	@Override
	public void stop(ApplicationRunner applicationRunner) {
		requireNonNull(applicationRunner).stop();
		synchronized (applications) {
			applications.remove(applicationRunner);
			applicationCount.set(applications.size());
		}
	}

	private final class DefaultApplicationRunner implements ApplicationRunner {

		private static final int MAX_RESULTS = 20;

		private final User user;
		private final Function<User, T> applicationFactory;
		private final List<Result> results = new ArrayList<>();
		private final AtomicBoolean stopped = new AtomicBoolean();
		private final LocalDateTime created = LocalDateTime.now();

		private T application;

		private DefaultApplicationRunner(User user, Function<User, T> applicationFactory) {
			this.user = user;
			this.applicationFactory = applicationFactory;
		}

		@Override
		public String name() {
			return application == null ? "Not initialized" : application.toString();
		}

		@Override
		public User user() {
			return user;
		}

		@Override
		public LocalDateTime created() {
			return created;
		}

		@Override
		public List<Result> results() {
			synchronized (results) {
				return unmodifiableList(new ArrayList<>(results));
			}
		}

		@Override
		public boolean stopped() {
			return stopped.get();
		}

		@Override
		public void stop() {
			stopped.set(true);
		}

		@Override
		public void run() {
			if (stopped.get()) {
				cleanupOnStop();
				return;
			}
			try {
				if (!paused.is()) {
					if (application == null && !stopped.get()) {
						application = initializeApplication();
					}
					else if (!stopped.get()) {
						runScenario(application, scenarioChooser.randomItem());
					}
				}
				if (stopped.get()) {
					cleanupOnStop();
					return;
				}
				scheduledExecutor.schedule(this, thinkTime(), TimeUnit.MILLISECONDS);
			}
			catch (Exception e) {
				LOG.debug("Exception during run {}", application, e);
			}
		}

		private void cleanupOnStop() {
			if (application != null) {
				closeApplication.accept(application);
				LOG.debug("LoadTestModel disconnected application: {}", application);
				application = null;
			}
		}

		private T initializeApplication() {
			try {
				long startTime = System.nanoTime();
				T app = applicationFactory.apply(user);
				int duration = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime);
				addResult(Result.success("Initialization", duration));
				LOG.debug("LoadTestModel initialized application: {}", app);

				return app;
			}
			catch (Exception e) {
				addResult(Result.failure("Initialization", e));
				return null;
			}
		}

		private void runScenario(T application, Scenario<T> scenario) {
			Result result = scenario.run(application);
			addResult(result);
			resultEvent.accept(result);
		}

		private void addResult(Result result) {
			synchronized (results) {
				results.add(result);
				if (results.size() > MAX_RESULTS) {
					results.remove(0);
				}
			}
		}

		private int thinkTime() {
			int time = maximumThinkTime.getOrThrow() - minimumThinkTime.getOrThrow();
			return time > 0 ? RANDOM.nextInt(time) + minimumThinkTime.getOrThrow() : minimumThinkTime.getOrThrow();
		}
	}

	private static final class DefaultCreateApplicationStep implements Builder.CreateApplicationStep {

		@Override
		public <T> Builder.CloseApplicationStep<T> createApplication(Function<User, T> createApplication) {
			return new DefaultCloseApplicationStep<>(requireNonNull(createApplication));
		}
	}

	private static final class DefaultCloseApplicationStep<T>  implements Builder.CloseApplicationStep<T> {

		private final Function<User, T> createApplication;

		private DefaultCloseApplicationStep(Function<User, T> createApplication) {
			this.createApplication = createApplication;
		}

		@Override
		public Builder<T> closeApplication(Consumer<T> closeApplication) {
			return new DefaultBuilder<>(createApplication, requireNonNull(closeApplication));
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final CreateApplicationStep CREATE_APPLICATION = new DefaultCreateApplicationStep();

		private final Function<User, T> createApplication;
		private final List<Scenario<T>> scenarios = new ArrayList<>();
		private final Consumer<T> closeApplication;

		private String name;
		private User user;
		private int minimumThinkTime = DEFAULT_MINIMUM_THINKTIME;
		private int maximumThinkTime = DEFAULT_MAXIMUM_THINKTIME;
		private int loginDelayFactor = DEFAULT_LOGIN_DELAY_FACTOR;
		private int applicationBatchSize = DEFAULT_APPLICATION_BATCH_SIZE;

		private DefaultBuilder(Function<User, T> createApplication, Consumer<T> closeApplication) {
			this.createApplication = createApplication;
			this.closeApplication = closeApplication;
		}

		@Override
		public Builder<T> user(User user) {
			this.user = requireNonNull(user);
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
		public Builder<T> scenarios(Collection<? extends Scenario<T>> scenarios) {
			this.scenarios.addAll(requireNonNull(scenarios));
			return this;
		}

		@Override
		public Builder<T> name(String name) {
			this.name = requireNonNull(name);
			return this;
		}

		@Override
		public LoadTest<T> build() {
			return new DefaultLoadTest<>(this);
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
			if (value > maximumThinkTime.getOrThrow()) {
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
			if (value < minimumThinkTime.getOrThrow()) {
				throw new IllegalArgumentException("Maximum think time must be equal to or exceed minimum think time");
			}
		}
	}
}
