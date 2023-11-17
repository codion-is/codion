/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario.DefaultRunResult;

import java.util.List;
import java.util.Optional;

/**
 * Specifies a load test usage scenario.
 * @param <T> the type used to run the scenario
 */
public interface UsageScenario<T> {

  /**
   * @return the name of this scenario
   */
  String name();

  /**
   * @return the default weight for this scenario, 1 by default
   */
  int defaultWeight();

  /**
   * The maximum time in milliseconds this scenario can run before issuing a warning.
   * @return the warning time
   */
  int maximumTime();

  /**
   * Runs this scenario with the given application
   * @param application the application to use
   * @return the run result
   */
  RunResult run(T application);

  /**
   * @return the total number of times this scenario has been run
   */
  int totalRunCount();

  /**
   * @return any exceptions that have occurred during a run
   */
  List<Throwable> exceptions();

  /**
   * Resets the run counters
   */
  void resetRunCount();

  /**
   * Clears the exceptions that have been collected so far
   */
  void clearExceptions();

  /**
   * @return the number of times this scenario has been successfully run
   */
  int successfulRunCount();

  /**
   * @return the number of times this scenario has been unsuccessfully run
   */
  int unsuccessfulRunCount();

  /**
   * Describes the results of a load test scenario run
   */
  interface RunResult {

    /**
     * @return the usage scenario name
     */
    String scenario();

    /**
     * @return the duration in microseconds
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
     * @return a new {@link RunResult} instance
     */
    static RunResult success(String scenarioName, int duration) {
      return new DefaultRunResult(scenarioName, duration, null);
    }

    /**
     * @param scenarioName the name of the usage scenario
     * @param exception the exception
     * @return a new {@link RunResult} instance
     */
    static RunResult failure(String scenarioName, Throwable exception) {
      return new DefaultRunResult(scenarioName, -1, exception);
    }
  }
}
