/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario.Result;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provides chart data for a load test.
 * @param <T> the load test application type
 * @see #loadTestModel(LoadTest)
 */
public interface LoadTestModel<T> extends LoadTest<T> {

  /**
   * @return a table model for displaying the active application instances
   */
  FilteredTableModel<ApplicationRow, Integer> applicationTableModel();

  /**
   * @return the chart data update interval in milliseconds
   */
  int getUpdateInterval();

  /**
   * @param updateInterval the chart data update interval in milliseconds
   */
  void setUpdateInterval(int updateInterval);

  /**
   * @return the state controlling whether this load test collects chart data
   */
  State collectChartData();

  /**
   * @return the state controlling whether the applications table model is automatically refreshed
   */
  State autoRefreshApplications();

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
  XYDataset scenarioDataset();

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
  XYDataset scenarioFailureDataset();

  /**
   * @param loadTest the load test
   * @return a new {@link LoadTestModel} instance based on the given load test
   * @param <T> the application type
   */
  static <T> LoadTestModel<T> loadTestModel(LoadTest<T> loadTest) {
    return new DefaultLoadTestModel<>(loadTest);
  }

  /**
   * Table model row describing a load test application.
   */
  interface ApplicationRow {

    int NAME_INDEX = 0;
    int USERNAME_INDEX = 1;
    int SCENARIO_INDEX = 2;
    int SUCCESSFUL_INDEX = 3;
    int DURATION_INDEX = 4;
    int EXCEPTION_INDEX = 5;
    int MESSAGE_INDEX = 6;
    int CREATED_INDEX = 7;

    /**
     * @return the name of the application
     */
    String name();

    /**
     * @return the application username
     */
    String username();

    /**
     * @return the application create time
     */
    LocalDateTime created();

    /**
     * @return the available run results
     */
    List<Result> results();
  }
}
