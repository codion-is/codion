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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.tools.randomizer.ItemRandomizer;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Specifies a class for running multiple application instances for load testing purposes.
 * @param <T> the type of application used by this load test.
 */
public interface LoadTestModel<T> {

  /**
   * Removes all applications and exits
   */
  void shutdown();

  /**
   * @return the user to use when initializing new application instances
   */
  Value<User> user();

  /**
   * @return a table model for displaying the active application instances
   */
  FilteredTableModel<Application, Integer> applicationTableModel();

  /**
   * The title of this LoadTest
   * @return the title
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
   * @return the names of the usage scenarios used by this load test.
   */
  Collection<String> usageScenarios();

  /**
   * @param usageScenarioName the scenario name
   * @return the usage scenario
   */
  UsageScenario<T> usageScenario(String usageScenarioName);

  /**
   * @return the chart data update interval in milliseconds
   */
  int getUpdateInterval();

  /**
   * @param updateInterval the chart data update interval in milliseconds
   */
  void setUpdateInterval(int updateInterval);

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
   * @return the state controlling whether this load test collects chart data
   */
  State collectChartData();

  /**
   * @return the state controlling whether the applications table model is automatically refreshed
   */
  State autoRefreshApplications();

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
   * Removes the selected applications
   */
  void removeSelectedApplications();

  /**
   * Clears the accumulated chart data
   */
  void clearCharts();

  /**
   * @param name the scenario name
   * @return a dataset plotting the average scenario duration
   */
  IntervalXYDataset scenarioDurationDataset(String name);

  /**
   * @return a dataset plotting the think time
   */
  XYDataset thinkTimeDataset();

  /**
   * @return a dataset plotting the number of active applications
   */
  XYDataset numberOfApplicationsDataset();

  /**
   * @return a dataset plotting the number of runs each usage scenario is being run per second
   */
  XYDataset usageScenarioDataset();

  /**
   * @return a dataset plotting the memory usage of this load test model
   */
  XYDataset memoryUsageDataset();

  /**
   * @return a dataset plotting the system load of this load test model
   */
  XYDataset systemLoadDataset();

  /**
   * @return a dataset plotting the failure rate of each usage scenario
   */
  XYDataset usageScenarioFailureDataset();

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
    return new DefaultLoadTestModel.DefaultBuilder<>(applicationFactory, closeApplication);
  }

  /**
   * Builds a {@link LoadTestModel}.
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
     * @param usageScenarios the usage scenarios
     * @return this builder
     */
    Builder<T> usageScenarios(Collection<? extends UsageScenario<T>> usageScenarios);

    /**
     * @param titleFactory the title factory
     * @return this builder
     */
    Builder<T> titleFactory(Function<LoadTestModel<T>, String> titleFactory);

    /**
     * @return a new load test instance
     */
    LoadTestModel<T> build();
  }

  /**
   * Describes a load test application.
   */
  interface Application {

    /**
     * @return the name of the application
     */
    String name();

    /**
     * @return the application username
     */
    String username();

    /**
     * @return the name of the last scenario run
     */
    String scenario();

    /**
     * @return true if the last scenario run was successful
     */
    Boolean successful();

    /**
     * @return the duration of the last scenario run, in milliseconds
     */
    Integer duration();

    /**
     * @return the exception message from the last run, if any
     */
    String exception();

    /**
     * @return the application create time
     */
    LocalDateTime created();
  }
}
