/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class ShortDashDateFormat extends DateMaskFormat {

  public static final String PATTERN = "dd-MM-yyyy";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new ShortDashDateFormat. */
  public ShortDashDateFormat() {
    super(PATTERN);
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
