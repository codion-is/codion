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

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.tools.loadtest.Scenario.Result;
import is.codion.tools.loadtest.randomizer.ItemRandomizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem.randomItem;
import static java.lang.Runtime.getRuntime;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;

final class DefaultLoadTest<T> implements LoadTest<T> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultLoadTest.class);

	private static final Random RANDOM = new Random();
	private static final int MINIMUM_NUMBER_OF_THREADS = 12;

	private final String name;
	private final State paused = State.state();
	private final State pauseOnException = State.state();
	private final DefaultApplications applications;
	private final ThinkTime thinkTime;
	private final Event<?> shuttingDown = Event.event();
	private final Event<Result> result = Event.event();
	private final Map<String, Scenario<T>> scenarios;
	private final ItemRandomizer<Scenario<T>> randomizer;
	private final ScheduledExecutorService scheduledExecutor =
					newScheduledThreadPool(Math.max(MINIMUM_NUMBER_OF_THREADS, getRuntime().availableProcessors() * 2));

	DefaultLoadTest(DefaultBuilder<T> builder) {
		this.name = builder.name;
		this.applications = new DefaultApplications(builder);
		this.thinkTime = new DefaultThinkTime(builder);
		this.scenarios = unmodifiableMap(builder.scenarios.stream()
						.collect(Collectors.toMap(Scenario::name, Function.identity())));
		this.randomizer = createRandomizer();
	}

	@Override
	public Optional<String> name() {
		return Optional.ofNullable(name);
	}

	@Override
	public Collection<Scenario<T>> scenarios() {
		return scenarios.values();
	}

	@Override
	public Applications applications() {
		return applications;
	}

	@Override
	public ThinkTime thinkTime() {
		return thinkTime;
	}

	@Override
	public ItemRandomizer<Scenario<T>> randomizer() {
		return randomizer;
	}

	@Override
	public State paused() {
		return paused;
	}

	@Override
	public State pauseOnException() {
		return pauseOnException;
	}

	@Override
	public void shutdown() {
		applications.stop();
		scheduledExecutor.shutdown();
		try {
			scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		shuttingDown.run();
	}

	@Override
	public Observer<?> shuttingDown() {
		return shuttingDown.observer();
	}

	@Override
	public Observer<Result> result() {
		return result.observer();
	}

	private ItemRandomizer<Scenario<T>> createRandomizer() {
		return ItemRandomizer.randomizer(scenarios.values().stream()
						.map(scenario -> randomItem(scenario, scenario.defaultWeight()))
						.collect(toList()));
	}

	private final class DefaultApplications implements Applications {

		private final Value<User> user;
		private final Function<User, T> createApplication;
		private final Consumer<T> closeApplication;
		private final Set<ApplicationRunner> runners = new HashSet<>();
		private final Value<Integer> applicationCount = Value.nonNull(0);
		private final Value<Integer> applicationBatchSize;
		private final Value<Integer> loginDelayFactor;

		private DefaultApplications(DefaultBuilder<T> builder) {
			this.user = Value.nullable(builder.user);
			this.createApplication = builder.createApplication;
			this.closeApplication = builder.closeApplication;
			this.applicationBatchSize = Value.builder()
							.nonNull(builder.applicationBatchSize)
							.validator(new MinimumValidator(1))
							.build();
			this.loginDelayFactor = Value.builder()
							.nonNull(builder.loginDelayFactor)
							.validator(new MinimumValidator(1))
							.build();
		}

		@Override
		public Value<User> user() {
			return user;
		}

		@Override
		public Collection<ApplicationRunner> runners() {
			synchronized (runners) {
				return unmodifiableSet(new HashSet<>(runners));
			}
		}

		@Override
		public Observable<Integer> count() {
			return applicationCount.observable();
		}

		@Override
		public Value<Integer> batchSize() {
			return applicationBatchSize;
		}

		@Override
		public Value<Integer> loginDelayFactor() {
			return loginDelayFactor;
		}

		@Override
		public void stop(ApplicationRunner applicationRunner) {
			requireNonNull(applicationRunner).stop();
			synchronized (runners) {
				runners.remove(applicationRunner);
				applicationCount.set(runners.size());
			}
		}

		@Override
		public void addBatch() {
			if (user.isNull()) {
				throw new IllegalStateException("User must be specified to add an application batch");
			}
			synchronized (runners) {
				int batchSize = applicationBatchSize.getOrThrow();
				for (int i = 0; i < batchSize; i++) {
					DefaultApplicationRunner applicationRunner = new DefaultApplicationRunner(user.get(), createApplication);
					runners.add(applicationRunner);
					applicationCount.set(runners.size());
					scheduledExecutor.schedule(applicationRunner, initialDelay(), TimeUnit.MILLISECONDS);
				}
			}
		}

		@Override
		public void removeBatch() {
			synchronized (runners) {
				if (!runners.isEmpty()) {
					runners.stream()
									.filter(applicationRunner -> !applicationRunner.stopped())
									.filter(applicationRunner -> user.isNull() || applicationRunner.user().equals(user.get()))
									.limit(applicationBatchSize.getOrThrow())
									.collect(toList())
									.forEach(this::stop);
				}
			}
		}

		private int initialDelay() {
			int time = thinkTime.maximum().getOrThrow() - thinkTime.minimum().getOrThrow();

			return time > 0 ? RANDOM.nextInt(time * loginDelayFactor.getOrThrow()) +
							thinkTime.minimum().getOrThrow() : thinkTime.minimum().getOrThrow();
		}

		private void stop() {
			synchronized (runners) {
				new ArrayList<>(runners).forEach(this::stop);
			}
		}
	}

	private static final class DefaultThinkTime implements ThinkTime {

		private final Value<Integer> maximum;
		private final Value<Integer> minimum;

		private DefaultThinkTime(DefaultBuilder<?> builder) {
			this.minimum = Value.builder()
							.nonNull(0)
							.value(builder.minimumThinkTime)
							.build();
			this.maximum = Value.builder()
							.nonNull(0)
							.value(builder.maximumThinkTime)
							.build();
			this.minimum.addValidator(new MinimumThinkTimeValidator());
			this.maximum.addValidator(new MaximumThinkTimeValidator());
		}

		@Override
		public Value<Integer> minimum() {
			return minimum;
		}

		@Override
		public Value<Integer> maximum() {
			return maximum;
		}

		private final class MinimumThinkTimeValidator extends MinimumValidator {

			private MinimumThinkTimeValidator() {
				super(0);
			}

			@Override
			public void validate(Integer value) {
				super.validate(value);
				if (value > maximum.getOrThrow()) {
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
				if (value < minimum.getOrThrow()) {
					throw new IllegalArgumentException("Maximum think time must be equal to or exceed minimum think time");
				}
			}
		}
	}

	private final class DefaultApplicationRunner implements ApplicationRunner {

		private static final int MAX_RESULTS = 20;
		private static final Predicate<Exception> PAUSE_ON_INIT_EXCEPTION = exception -> true;

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
			if (!paused.is()) {
				if (application == null && !stopped.get()) {
					application = initializeApplication();
				}
				else if (!stopped.get()) {
					randomizer.get().ifPresent(scenario -> runScenario(application, scenario));
				}
			}
			if (stopped.get()) {
				cleanupOnStop();
				return;
			}
			scheduledExecutor.schedule(this, thinkTime(), TimeUnit.MILLISECONDS);
		}

		private void cleanupOnStop() {
			if (application != null) {
				applications.closeApplication.accept(application);
				LOG.debug("LoadTestModel disconnected application: {}", application);
				application = null;
			}
		}

		private T initializeApplication() {
			long startTimeMillis = System.currentTimeMillis();
			try {
				long startTimeNano = System.nanoTime();
				T app = applicationFactory.apply(user);
				int duration = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTimeNano);
				addResult(Result.success("Initialization", startTimeMillis, duration), PAUSE_ON_INIT_EXCEPTION);
				LOG.debug("LoadTestModel initialized application: {}", app);

				return app;
			}
			catch (Exception e) {
				addResult(Result.failure("Initialization", startTimeMillis, e), PAUSE_ON_INIT_EXCEPTION);
				return null;
			}
		}

		private void runScenario(T application, Scenario<T> scenario) {
			Result result = scenario.run(application);
			addResult(result, scenario::pause);
			DefaultLoadTest.this.result.accept(result);
		}

		private void addResult(Result result, Predicate<Exception> pause) {
			synchronized (results) {
				results.add(result);
				if (results.size() > MAX_RESULTS) {
					results.remove(0);
				}
				result.exception().ifPresent(exception -> {
					if (pauseOnException.is() && pause.test(exception)) {
						paused.set(true);
					}
				});
			}
		}

		private int thinkTime() {
			int time = thinkTime.maximum().getOrThrow() - thinkTime.minimum().getOrThrow();

			return time > 0 ? RANDOM.nextInt(time) + thinkTime.minimum().getOrThrow() : thinkTime.minimum().getOrThrow();
		}
	}

	private static final class DefaultCreateApplicationStep implements Builder.CreateApplicationStep {

		@Override
		public <T> Builder.CloseApplicationStep<T> createApplication(Function<User, T> createApplication) {
			return new DefaultCloseApplicationStep<>(requireNonNull(createApplication));
		}
	}

	private static final class DefaultCloseApplicationStep<T> implements Builder.CloseApplicationStep<T> {

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

		private final List<Scenario<T>> scenarios = new ArrayList<>();
		private final Function<User, T> createApplication;
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
}
