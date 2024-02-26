/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.loadtest;

import is.codion.common.model.loadtest.UsageScenario.Result;
import is.codion.common.model.randomizer.ItemRandomizer;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
   * @return the load test title
   */
  String title();

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
  Collection<UsageScenario<T>> scenarios();

  /**
   * @param scenarioName the scenario name
   * @return the usage scenario
   */
  UsageScenario<T> scenario(String scenarioName);

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
  ItemRandomizer<UsageScenario<T>> scenarioChooser();

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
    Builder<T> scenarios(Collection<? extends UsageScenario<T>> scenarios);

    /**
     * @param titleFactory the title factory
     * @return this builder
     */
    Builder<T> titleFactory(Function<LoadTest<T>, String> titleFactory);

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
