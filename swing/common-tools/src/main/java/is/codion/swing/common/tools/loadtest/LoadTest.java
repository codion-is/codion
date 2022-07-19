/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

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
  User getUser();

  /**
   * @param user the user to use when initializing new application instances
   */
  void setUser(User user);

  /**
   * The title of this LoadTest
   * @return the title
   */
  String getTitle();

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
  Collection<String> getUsageScenarios();

  /**
   * @param usageScenarioName the scenario name
   * @return the usage scenario
   */
  UsageScenario<T> getUsageScenario(String usageScenarioName);

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
  int getApplicationCount();

  /**
   * @return the Value controlling the number of applications to initialize per batch
   */
  Value<Integer> getApplicationBatchSizeValue();

  /**
   * @return the state controlling the paused state of this load test
   */
  State getPausedState();

  /**
   * @return the Value controlling the maximum number of milliseconds that should pass between work requests
   */
  Value<Integer> getMaximumThinkTimeValue();

  /**
   * @return the Value controlling the minimum number of milliseconds that should pass between work requests
   */
  Value<Integer> getMinimumThinkTimeValue();

  /**
   * This value controls the factor with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the Value controlling the factor with which to multiply the think time when logging in
   */
  Value<Integer> getLoginDelayFactorValue();

  /**
   * @return the state controlling whether this load test collects chart data
   */
  State getCollectChartDataState();

  /**
   * @return an observer notified each time the application count changes
   */
  ValueObserver<Integer> applicationCountObserver();

  /**
   * Adds a batch of applications.
   * @see #getApplicationBatchSizeValue()
   */
  void addApplicationBatch();

  /**
   * Removes one batch of applications.
   * @see #getApplicationBatchSizeValue()
   */
  void removeApplicationBatch();

  /**
   * Resets the accumulated chart data
   */
  void resetChartData();

  /**
   * @return a dataset plotting the average scenario duration
   * @param name the scenario name
   */
  IntervalXYDataset getScenarioDurationDataset(String name);

  /**
   * @return a dataset plotting the think time
   */
  XYDataset getThinkTimeDataset();

  /**
   * @return a dataset plotting the number of active applications
   */
  XYDataset getNumberOfApplicationsDataset();

  /**
   * @return a dataset plotting the number of runs each usage scenario is being run per second
   */
  XYDataset getUsageScenarioDataset();

  /**
   * @return a dataset plotting the memory usage of this load test model
   */
  XYDataset getMemoryUsageDataset();

  /**
   * @return a dataset plotting the system load of this load test model
   */
  XYDataset getSystemLoadDataset();

  /**
   * @return a dataset plotting the failure rate of each usage scenario
   */
  XYDataset getUsageScenarioFailureDataset();

  /**
   * @return the randomizer used to select scenarios
   */
  ItemRandomizer<UsageScenario<T>> getScenarioChooser();
}
