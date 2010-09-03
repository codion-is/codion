/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Collection;

/**
 * Specifies a class for running multiple application instances for load testing purposes.
 */
public interface LoadTest {

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
   * Sets the random chooser weight for the given scenario
   * @param scenarioName the name of the scenario
   * @param weight the new weight to assign to the scenario
   */
  void setWeight(final String scenarioName, final int weight);

  /**
   * @return the usage scenarios used by this load test;
   */
  Collection<String> getUsageScenarios();

  /**
   * @param usageScenarioName the scenario name
   * @return the usage scenario
   */
  UsageScenario getUsageScenario(final String usageScenarioName);

  /**
   * @return the the maximum time in milliseconds a work request has to finish
   */
  int getWarningTime();

  /**
   * @param warningTime the the maximum time in milliseconds a work request has to finish
   */
  void setWarningTime(final int warningTime);

  /**
   * @return the chart data update interval
   */
  int getUpdateInterval();

  /**
   * @param updateInterval the chart data update interval
   */
  void setUpdateInterval(final int updateInterval);

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
  void setApplicationBatchSize(final int applicationBatchSize);

  /**
   * @return true if the load testing is paused
   */
  boolean isPaused();

  /**
   * @param value true if load testing should be paused
   */
  void setPaused(final boolean value);

  /**
   * @return the maximum number of milliseconds that should pass between work requests
   */
  int getMaximumThinkTime();

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   */
  void setMaximumThinkTime(final int maximumThinkTime);

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   */
  int getMinimumThinkTime();

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   */
  void setMinimumThinkTime(final int minimumThinkTime);

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
  void setLoginDelayFactor(final int loginDelayFactor);

  /**
   * @return true if chart data is being collected
   */
  boolean isCollectChartData();

  /**
   * @param value true if chart data should be collected
   */
  void setCollectChartData(final boolean value);

  /**
   * @return an observer notified each time the application count changes
   */
  EventObserver applicationCountObserver();

  /**
   * @return an observer notified each time the application batch size changes
   */
  EventObserver applicationBatchSizeObserver();

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
   * @return an observer notified each time the collect chart data state changes
   */
  EventObserver collectChartDataObserver();

  /**
   * @return a dataset plotting the average scenario duration
   * @param name the scenario name
   */
  YIntervalSeriesCollection getScenarioDurationDataset(final String name);

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
   * @return a dataset plotting the failure rate of each usage scenario
   */
  XYDataset getUsageScenarioFailureDataset();

  /**
   * @return an observer notified each time the maximum think time changes
   */
  EventObserver maximumThinkTimeObserver();/**

   * @param listener a listener notified when this load test model has finished removing all applications
   */
  void addExitListener(ActionListener listener);

  /**
   * @return an observer notified each time the minimum think time changes
   */
  EventObserver getMinimumThinkTimeObserver();

  /**
   * @return an observer notified each time the warning time changes
   */
  EventObserver getWarningTimeObserver();

  /**
   * @return an observer notified each time the paused state changes
   */
  EventObserver getPauseObserver();

  /**
   * @return the randomizer used to select scenarios
   */
  ItemRandomizer<UsageScenario> getScenarioChooser();

  /**
   * Specifies a load test usage scenario.
   */
  interface UsageScenario<T> extends Serializable {

    /**
     * @return the name of this scenario
     */
    String getName();

    /**
     * @return the default weight for this scenario, 1 by default
     */
    int getDefaultWeight();

    /**
     * Runs this scenario with the given application
     * @param application the application to use
     * @throws ScenarioException in case of an exception
     */
    void run(final T application) throws ScenarioException;

    /**
     * @return the total number of times this scenario has been run
     */
    int getTotalRunCount();

    /**
     * Resets the run counters
     */
    void resetRunCount();

    /**
     * @return the number of times this scenario has been successfully run
     */
    int getSuccessfulRunCount();

    /**
     * @return the number of times this scenario has been unsuccessfully run
     */
    int getUnsuccessfulRunCount();
  }

  /**
   * An exception originating from a scenario run
   */
  class ScenarioException extends Exception {

    /**
     * Instantiates a new ScenarioException.
     */
    public ScenarioException() {
      super();
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
