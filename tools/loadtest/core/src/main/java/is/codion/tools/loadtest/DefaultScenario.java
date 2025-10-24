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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

final class DefaultScenario<T> implements Scenario<T> {

	private static final Consumer<?> EMPTY_CONSUMER = (Consumer<Object>) object -> {};
	private static final Predicate<Exception> PAUSE_ON_ALL_EXCPTIONS = exception -> true;

	private final String name;
	private final int defaultWeight;
	private final Performer<T> performer;
	private final Consumer<T> beforeRun;
	private final Consumer<T> afterRun;
	private final Predicate<Exception> pause;

	private DefaultScenario(DefaultBuilder<T> builder) {
		this.performer = builder.performer;
		this.name = builder.name;
		this.defaultWeight = builder.defaultWeight;
		this.beforeRun = builder.beforeRun;
		this.afterRun = builder.afterRun;
		this.pause = builder.pause;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int defaultWeight() {
		return defaultWeight;
	}

	@Override
	public Result run(T application) {
		requireNonNull(application, "application may not be null");
		beforeRun.accept(application);
		long startTimeMillis = System.currentTimeMillis();
		try {
			long startTime = System.nanoTime();
			performer.perform(application);

			return Result.success(name, startTimeMillis, TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime));
		}
		catch (Exception e) {
			return Result.failure(name, startTimeMillis, e);
		}
		finally {
			afterRun.accept(application);
		}
	}

	@Override
	public boolean pause(Exception exception) {
		return pause.test(exception);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Scenario && ((Scenario<T>) obj).name().equals(name);
	}

	static final class DefaultPerformerStep implements Builder.PerformerStep {

		@Override
		public <T> Builder<T> performer(Performer<T> performer) {
			return new DefaultBuilder<>(requireNonNull(performer));
		}
	}

	private static final class DefaultBuilder<T> implements Scenario.Builder<T> {

		private final Performer<T> performer;

		private String name;
		private int defaultWeight = 1;
		private Consumer<T> beforeRun = (Consumer<T>) EMPTY_CONSUMER;
		private Consumer<T> afterRun = (Consumer<T>) EMPTY_CONSUMER;
		private Predicate<Exception> pause = PAUSE_ON_ALL_EXCPTIONS;

		private DefaultBuilder(Performer<T> performer) {
			this.performer = performer;
			this.name = performer.getClass().getSimpleName();
		}

		@Override
		public Builder<T> name(String name) {
			this.name = requireNonNull(name);
			return this;
		}

		@Override
		public Builder<T> defaultWeight(int defaultWeight) {
			if (defaultWeight < 0) {
				throw new IllegalArgumentException("Default weight must be a positive integer");
			}
			this.defaultWeight = defaultWeight;
			return this;
		}

		@Override
		public Builder<T> beforeRun(Consumer<T> beforeRun) {
			this.beforeRun = requireNonNull(beforeRun);
			return this;
		}

		@Override
		public Builder<T> afterRun(Consumer<T> afterRun) {
			this.afterRun = requireNonNull(afterRun);
			return this;
		}

		@Override
		public Builder<T> pause(Predicate<Exception> pause) {
			this.pause = requireNonNull(pause);
			return this;
		}

		@Override
		public Scenario<T> build() {
			return new DefaultScenario<>(this);
		}
	}

	static final class DefaultRunResult implements Result {

		private final String scenario;
		private final long started;
		private final long duration;
		private final Exception exception;

		DefaultRunResult(String scenario, long started, long duration, Exception exception) {
			this.scenario = requireNonNull(scenario);
			this.started = started;
			this.duration = duration;
			this.exception = exception;
		}

		@Override
		public long started() {
			return started;
		}

		@Override
		public long duration() {
			return duration;
		}

		@Override
		public String scenario() {
			return scenario;
		}

		@Override
		public boolean successful() {
			return exception == null;
		}

		@Override
		public Optional<Exception> exception() {
			return Optional.ofNullable(exception);
		}
	}
}
