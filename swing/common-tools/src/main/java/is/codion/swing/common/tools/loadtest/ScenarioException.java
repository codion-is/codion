/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

/**
 * An exception originating from a scenario run
 */
public final class ScenarioException extends Exception {

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
