/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

/**
 * Specifies a class for running multiple application instances for load testing purposes.
 * @param <T> the type of application used by this load test.
 */
public interface LoadTest<T> {

  /**
   * Removes all applications and exits
   */
  void shutdown();

  /**
   * @return the user to use when initializing new application instances
   */
  Value<User> userValue();

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
   * @return the number of active applications
   */
  int applicationCount();

  /**
   * @return the Value controlling the number of applications to initialize per batch
   */
  Value<Integer> applicationBatchSizeValue();

  /**
   * @return the state controlling the paused state of this load test
   */
  State pausedState();

  /**
   * @return the Value controlling the maximum number of milliseconds that should pass between work requests
   */
  Value<Integer> maximumThinkTimeValue();

  /**
   * @return the Value controlling the minimum number of milliseconds that should pass between work requests
   */
  Value<Integer> minimumThinkTimeValue();

  /**
   * This value controls the factor with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the Value controlling the factor with which to multiply the think time when logging in
   */
  Value<Integer> loginDelayFactorValue();

  /**
   * @return the state controlling whether this load test collects chart data
   */
  State collectChartDataState();

  /**
   * @return an observer notified each time the application count changes
   */
  ValueObserver<Integer> applicationCountObserver();

  /**
   * Adds a batch of applications.
   * @see #applicationBatchSizeValue()
   */
  void addApplicationBatch();

  /**
   * Removes one batch of applications.
   * @see #applicationBatchSizeValue()
   */
  void removeApplicationBatch();

  /**
   * Clears the accumulated chart data
   */
  void clearChartData();

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
