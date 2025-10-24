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

import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.tools.loadtest.Scenario.Result;
import is.codion.tools.loadtest.randomizer.ItemRandomizer;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Specifies a class for running multiple application instances for load testing purposes.
 * @param <T> the type of application used by this load test.
 */
public interface LoadTest<T> {

	int DEFAULT_MINIMUM_THINKTIME = 2500;
	int DEFAULT_MAXIMUM_THINKTIME = 5000;
	int DEFAULT_LOGIN_DELAY_FACTOR = 2;
	int DEFAULT_APPLICATION_BATCH_SIZE = 10;

	/**
	 * Shuts down and removes all applications
	 */
	void shutdown();

	/**
	 * @param applicationRunner the application runner to stop
	 */
	void stop(ApplicationRunner applicationRunner);

	/**
	 * @return the user to use when initializing new application instances
	 */
	Value<User> user();

	/**
	 * @return the load test name, or an empty Optional if none is available
	 */
	Optional<String> name();

	/**
	 * Sets the random chooser weight for the given scenario
	 * @param scenarioName the name of the scenario
	 * @param weight the new weight to assign to the scenario
	 */
	void setWeight(String scenarioName, int weight);

	/**
	 * @param scenarioName the scenario name
	 * @return true if the scenario is enabled
	 */
	boolean isScenarioEnabled(String scenarioName);

	/**
	 * @param scenarioName the scenario name
	 * @param enabled true if the scenario should be enabled
	 */
	void setScenarioEnabled(String scenarioName, boolean enabled);

	/**
	 * @return the usage scenarios used by this load test.
	 */
	Collection<Scenario<T>> scenarios();

	/**
	 * @param scenarioName the scenario name
	 * @return the usage scenario
	 */
	Scenario<T> scenario(String scenarioName);

	/**
	 * @return an observer notified each time a run result is produced
	 */
	Observer<Result> result();

	/**
	 * @param listener a listener notified when this load test model has been shutdown.
	 */
	void addShutdownListener(Runnable listener);

	/**
	 * @return the {@link Value} controlling the number of applications to initialize per batch
	 */
	Value<Integer> applicationBatchSize();

	/**
	 * @return the {@link State} controlling the paused state of this load test
	 */
	State paused();

	/**
	 * @return the {@link State} controlling if the load test is automatically paused when an exception occures in a scenario run
	 * @see Scenario#pause(Exception)
	 * @see Scenario.Builder#pause(Predicate)
	 */
	State pauseOnException();

	/**
	 * @return the {@link Value} controlling the maximum number of milliseconds that should pass between work requests
	 */
	Value<Integer> maximumThinkTime();

	/**
	 * @return the {@link Value} controlling the minimum number of milliseconds that should pass between work requests
	 */
	Value<Integer> minimumThinkTime();

	/**
	 * This value controls the factor with which to multiply the think time when logging in, this helps
	 * spread the application logins when creating a batch of application.
	 * @return the {@link Value} controlling the factor with which to multiply the think time when logging in
	 */
	Value<Integer> loginDelayFactor();

	/**
	 * @return the applications
	 */
	Map<ApplicationRunner, T> applications();

	/**
	 * @return an observable notified each time the application count changes
	 */
	Observable<Integer> applicationCount();

	/**
	 * Adds a batch of applications.
	 * @see #applicationBatchSize()
	 */
	void addApplicationBatch();

	/**
	 * Removes one batch of applications.
	 * @see #applicationBatchSize()
	 */
	void removeApplicationBatch();

	/**
	 * @return the randomizer used to select scenarios
	 */
	ItemRandomizer<Scenario<T>> scenarioChooser();

	/**
	 * @return a {@link Builder.CreateApplicationStep} instance
	 */
	static Builder.CreateApplicationStep builder() {
		return DefaultLoadTest.DefaultBuilder.CREATE_APPLICATION;
	}

	/**
	 * Builds a {@link LoadTest}.
	 * @param <T> the load test application type
	 */
	interface Builder<T> {

		/**
		 * Provides an {@link CloseApplicationStep}
		 */
		interface CreateApplicationStep {

			/**
			 * @param createApplication creates the application
			 * @return a {@link CloseApplicationStep}
			 * @param <T> the application type
			 */
			<T> CloseApplicationStep<T> createApplication(Function<User, T> createApplication);
		}

		/**
		 * @param <T> the application type
		 */
		interface CloseApplicationStep<T> {

			/**
			 * @param closeApplication closes the application
			 * @return a builder instance
			 */
			Builder<T> closeApplication(Consumer<T> closeApplication);
		}

		/**
		 * @param user the initial application user
		 * @return this builder
		 */
		Builder<T> user(User user);

		/**
		 * @param minimumThinkTime the initial minimum think time
		 * @return this builder
		 */
		Builder<T> minimumThinkTime(int minimumThinkTime);

		/**
		 * @param maximumThinkTime the initial maximum think time
		 * @return this builder
		 */
		Builder<T> maximumThinkTime(int maximumThinkTime);

		/**
		 * @param loginDelayFactor the login delay factor
		 * @return this builder
		 */
		Builder<T> loginDelayFactor(int loginDelayFactor);

		/**
		 * @param applicationBatchSize the initial application batch size
		 * @return this builder
		 */
		Builder<T> applicationBatchSize(int applicationBatchSize);

		/**
		 * @param scenarios the usage scenarios
		 * @return this builder
		 */
		Builder<T> scenarios(Collection<? extends Scenario<T>> scenarios);

		/**
		 * @param name the load test name
		 * @return this builder
		 */
		Builder<T> name(String name);

		/**
		 * @return a new load test instance
		 */
		LoadTest<T> build();
	}

	/**
	 * Handles running a load test application
	 */
	interface ApplicationRunner extends Runnable {

		/**
		 * @return the name
		 */
		String name();

		/**
		 * @return the user
		 */
		User user();

		/**
		 * @return the creation time
		 */
		LocalDateTime created();

		/**
		 * @return the available run results
		 */
		List<Result> results();

		/**
		 * @return true if this runner has been stopped
		 */
		boolean stopped();

		/**
		 * Stops this application runner
		 */
		void stop();
	}
}
