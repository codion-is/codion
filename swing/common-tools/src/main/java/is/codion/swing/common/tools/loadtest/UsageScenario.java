/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import java.util.List;

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
   */
  void run(T application);

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
}
