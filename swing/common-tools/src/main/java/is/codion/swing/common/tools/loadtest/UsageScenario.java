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
  String getName();

  /**
   * @return the default weight for this scenario, 1 by default
   */
  int getDefaultWeight();

  /**
   * The maximum time in milliseconds this scenario can run before issuing a warning.
   * @return the warning time
   */
  int getMaximumTime();

  /**
   * Runs this scenario with the given application
   * @param application the application to use
   */
  void run(T application);

  /**
   * @return the total number of times this scenario has been run
   */
  int getTotalRunCount();

  /**
   * @return any exceptions that have occurred during a run
   */
  List<Exception> getExceptions();

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
  int getSuccessfulRunCount();

  /**
   * @return the number of times this scenario has been unsuccessfully run
   */
  int getUnsuccessfulRunCount();
}
