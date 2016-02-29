/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

/**
 * An exception originating from a scenario run
 */
public final class ScenarioException extends Exception {

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
