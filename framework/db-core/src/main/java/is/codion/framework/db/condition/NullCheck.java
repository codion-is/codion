/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

/**
 * Specifies whether a condition should check if the value is null or not null.
 */
public enum NullCheck {
  /**
   * The value should be null.
   */
  IS_NULL,
  /**
   * The value should not be null.
   */
  IS_NOT_NULL
}
