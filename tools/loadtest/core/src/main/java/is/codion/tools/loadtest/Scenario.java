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

import is.codion.tools.loadtest.DefaultScenario.DefaultPerformerStep;
import is.codion.tools.loadtest.DefaultScenario.DefaultRunResult;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Specifies a load test usage scenario.
 * @param <T> the type used to run the scenario
 * @see #scenario(Performer)
 * @see #scenario(Performer, int)
 * @see #builder()
 */
public interface Scenario<T> {

	/**
	 * @return the name of this scenario
	 */
	String name();

	/**
	 * @return the default weight for this scenario, 1 by default
	 */
	int defaultWeight();

	/**
	 * @param exception the exception
	 * @return true if the load test should be paused when the given exception occurs in this scenario
	 */
	boolean pause(Exception exception);

	/**
	 * Runs this scenario with the given application
	 * @param application the application to use
	 * @return the run result
	 */
	Result run(T application);

	/**
	 * Performs a load test scenario.
	 * @param <T> the load test application type
	 */
	interface Performer<T> {

		/**
		 * Performs the scenario using the given application
		 * @param application the application
		 * @throws Exception in case of an exception
		 */
		void perform(T application) throws Exception;
	}

	/**
	 * A {@link Scenario} builder.
	 * @param <T> the load test application type
	 */
	interface Builder<T> {

		/**
		 * Specifies the scenario performer
		 */
		interface PerformerStep {

			/**
			 * The default scenario name is {@code performer.getClass().getSimpleName()}.
			 * @param performer the scenario performer
			 * @return the builder
			 */
			<T> Builder<T> performer(Performer<T> performer);
		}

		/**
		 * @param name the scenario name
		 * @return this builder
		 */
		Builder<T> name(String name);

		/**
		 * @param defaultWeight the default weight
		 * @return this builder
		 */
		Builder<T> defaultWeight(int defaultWeight);

		/**
		 * @param beforeRun called before each run
		 * @return this builder
		 */
		Builder<T> beforeRun(Consumer<T> beforeRun);

		/**
		 * @param afterRun called after each run
		 * @return this builder
		 */
		Builder<T> afterRun(Consumer<T> afterRun);

		/**
		 * By default, all exceptions cause a test to be paused.
		 * @param pause the {@link Predicate} controlling whether the load test should be paused when a given exception occurs in this scenario
		 * @return this builder
		 * @see LoadTest#pauseOnException()
		 */
		Builder<T> pause(Predicate<Exception> pause);

		/**
		 * @return a new {@link Scenario} instance
		 */
		Scenario<T> build();
	}

	/**
	 * @return a new {@link Builder.PerformerStep} instance
	 */
	static Builder.PerformerStep builder() {
		return new DefaultPerformerStep();
	}

	/**
	 * @param performer the scenario performer
	 * @param <T> the load test application type
	 * @return a new {@link Scenario} instance
	 */
	static <T> Scenario<T> scenario(Performer<T> performer) {
		return new DefaultPerformerStep()
						.performer(performer)
						.build();
	}

	/**
	 * @param performer the scenario performer
	 * @param defaultWeight the default scenario weight
	 * @param <T> the load test application type
	 * @return a new {@link Scenario} instance
	 */
	static <T> Scenario<T> scenario(Performer<T> performer, int defaultWeight) {
		return new DefaultPerformerStep()
						.performer(performer)
						.defaultWeight(defaultWeight)
						.build();
	}

	/**
	 * Describes the results of a load test scenario run
	 */
	interface Result {

		/**
		 * @return the usage scenario name
		 */
		String scenario();

		/**
		 * @return the time the scenario run started
		 */
		long started();

		/**
		 * @return the duration in microseconds, -1 in case of failure
		 */
		long duration();

		/**
		 * @return true if the run was successful
		 */
		boolean successful();

		/**
		 * @return the exception in case the run was unsuccessful, otherwise an empty optional
		 */
		Optional<Exception> exception();

		/**
		 * @param scenarioName the name of the usage scenario
		 * @param started the start time
		 * @param duration the duriation in microseconds
		 * @return a new {@link Result} instance
		 */
		static Result success(String scenarioName, long started, long duration) {
			return new DefaultRunResult(scenarioName, started, duration, null);
		}

		/**
		 * @param scenarioName the name of the usage scenario
		 * @param started the start time
		 * @param exception the exception
		 * @return a new {@link Result} instance
		 */
		static Result failure(String scenarioName, long started, Exception exception) {
			return new DefaultRunResult(scenarioName, started, -1, exception);
		}
	}
}
