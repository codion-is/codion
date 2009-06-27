/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class CompactDateFormat extends DateMaskFormat {

  public static final String PATTERN = "ddMMyy";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new CompactDateFormat. */
  public CompactDateFormat() {
    super(PATTERN);
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
