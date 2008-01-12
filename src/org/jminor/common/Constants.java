/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common;

import java.sql.Timestamp;

/**
 * Some constants
 */
public class Constants {

  /**
   * The String used as wildcard in String filters
   */
  public static final String WILDCARD = "%";
  public static final int INT_NULL_VALUE = -Integer.MAX_VALUE;
  public static final char CHAR_NULL_VALUE = Character.MIN_VALUE;
  public static final double DOUBLE_NULL_VALUE = Double.NEGATIVE_INFINITY;
  public static final Character CHARACTER_NULL_VALUE = CHAR_NULL_VALUE;
  public static final Integer INTEGER_NULL_VALUE = INT_NULL_VALUE;
  public static final Timestamp TIMESTAMP_NULL_VALUE = new Timestamp(Long.MIN_VALUE);
}
