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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.loadtest;

import is.codion.common.model.loadtest.DefaultScenario.DefaultRunResult;
import is.codion.common.model.loadtest.LoadTest.Scenario.Result;
import is.codion.common.model.randomizer.ItemRandomizer;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

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
   * Removes all applications and exits
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
   * @param listener a listener notified each time a run result is produced
   */
  void addResultListener(Consumer<Result> listener);

  /**
   * @param listener a listener notified when this load test model has been shutdown.
   */
  void addShutdownListener(Runnable listener);

  /**
   * @return the Value controlling the number of applications to initialize per batch
   */
  Value<Integer> applicationBatchSize();

  /**
   * @return the state controlling the paused state of this load test
   */
  State paused();

  /**
   * @return the Value controlling the maximum number of milliseconds that should pass between work requests
   */
  Value<Integer> maximumThinkTime();

  /**
   * @return the Value controlling the minimum number of milliseconds that should pass between work requests
   */
  Value<Integer> minimumThinkTime();

  /**
   * This value controls the factor with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the Value controlling the factor with which to multiply the think time when logging in
   */
  Value<Integer> loginDelayFactor();

  /**
   * @return the applications
   */
  Map<ApplicationRunner, T> applications();

  /**
   * @return an observer notified each time the application count changes
   */
  ValueObserver<Integer> applicationCount();

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
   * @param applicationFactory the application factory
   * @param closeApplication closes an application
   * @return a new builder
   * @param <T> the application type
   */
  static <T> Builder<T> builder(Function<User, T> applicationFactory, Consumer<T> closeApplication) {
    return new DefaultLoadTest.DefaultBuilder<>(applicationFactory, closeApplication);
  }

  /**
   * Builds a {@link LoadTest}.
   * @param <T> the load test application type
   */
  interface Builder<T> {

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

  /**
   * Specifies a load test usage scenario.
   * @param <T> the type used to run the scenario
   * @see #scenario(Performer)
   * @see #scenario(Performer, int)
   * @see #builder(Performer)
   */
  interface Scenario<T> {

    /**
     * @return the name of this scenario
     */
    String name();

    /**
     * @return the default weight for this scenario, 1 by default
     */
    int defaultWeight();

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
       * @return a new {@link Scenario} instance
       */
      Scenario<T> build();
    }

    /**
     * @param performer the scenario performer
     * @return a new Builder
     * @param <T> the load test application type
     */
    static <T> Builder<T> builder(Performer<T> performer) {
      return new DefaultScenario.DefaultBuilder<>(performer);
    }

    /**
     * @param performer the scenario performer
     * @return a new {@link Scenario} instance
     * @param <T> the load test application type
     */
    static <T> Scenario<T> scenario(Performer<T> performer) {
      return builder(performer).build();
    }

    /**
     * @param performer the scenario performer
     * @param defaultWeight the default scenario weight
     * @return a new {@link Scenario} instance
     * @param <T> the load test application type
     */
    static <T> Scenario<T> scenario(Performer<T> performer, int defaultWeight) {
      return builder(performer)
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
       * @return the duration in microseconds, -1 in case of failure
       */
      int duration();

      /**
       * @return true if the run was successful
       */
      boolean successful();

      /**
       * @return the exception in case the run was unsuccessful, otherwise an empty optional
       */
      Optional<Throwable> exception();

      /**
       * @param scenarioName the name of the usage scenario
       * @param duration the duriation in microseconds
       * @return a new {@link Result} instance
       */
      static Result success(String scenarioName, int duration) {
        return new DefaultRunResult(scenarioName, duration, null);
      }

      /**
       * @param scenarioName the name of the usage scenario
       * @param exception the exception
       * @return a new {@link Result} instance
       */
      static Result failure(String scenarioName, Throwable exception) {
        return new DefaultRunResult(scenarioName, -1, exception);
      }
    }
  }
}
