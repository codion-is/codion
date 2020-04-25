/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.loadtest;

import org.jminor.common.event.EventObserver;
import org.jminor.common.user.User;

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
  void exit();

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
   * @return the number of applications to initialize per batch
   */
  int getApplicationBatchSize();

  /**
   * @param applicationBatchSize the number of applications to initialize per batch
   */
  void setApplicationBatchSize(int applicationBatchSize);

  /**
   * @return true if the load testing is paused
   */
  boolean isPaused();

  /**
   * @param paused true if load testing should be paused
   */
  void setPaused(boolean paused);

  /**
   * @return the maximum number of milliseconds that should pass between work requests
   */
  int getMaximumThinkTime();

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   */
  void setMaximumThinkTime(int maximumThinkTime);

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   */
  int getMinimumThinkTime();

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   */
  void setMinimumThinkTime(int minimumThinkTime);

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the number with which to multiply the think time when logging in
   */
  int getLoginDelayFactor();

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @param loginDelayFactor the number with which to multiply the think time when logging in
   */
  void setLoginDelayFactor(int loginDelayFactor);

  /**
   * @return true if chart data is being collected
   */
  boolean isCollectChartData();

  /**
   * @param collectChartData true if chart data should be collected
   */
  void setCollectChartData(boolean collectChartData);

  /**
   * @return an observer notified each time the application count changes
   */
  EventObserver<Integer> applicationCountObserver();

  /**
   * @return an observer notified each time the application batch size changes
   */
  EventObserver<Integer> applicationBatchSizeObserver();

  /**
   * Adds a batch of applications.
   * @see #setApplicationBatchSize(int)
   */
  void addApplicationBatch();

  /**
   * Removes one batch of applications.
   * @see #setApplicationBatchSize(int)
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
   * @return an observer notified each time the collect chart data state changes
   */
  EventObserver<Boolean> collectChartDataObserver();

  /**
   * @return an observer notified each time the maximum think time changes
   */
  EventObserver<Integer> maximumThinkTimeObserver();

  /**
   * @return an observer notified each time the minimum think time changes
   */
  EventObserver<Integer> getMinimumThinkTimeObserver();

  /**
   * @return an observer notified each time the paused state changes
   */
  EventObserver<Boolean> getPauseObserver();

  /**
   * @return the randomizer used to select scenarios
   */
  ItemRandomizer<UsageScenario<T>> getScenarioChooser();

  /**
   * An exception originating from a scenario run
   */
  final class ScenarioException extends Exception {

    /**
     * Instantiates a new ScenarioException.
     */
    public ScenarioException() {
      this(null);
    }

    /**
     * Instantiates a new ScenarioException.
     * @param cause the root cause
     */
    public ScenarioException(final Throwable cause) {
      super(cause);
    }
  }
}
