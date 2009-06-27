/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class ShortTimeFormat extends DateMaskFormat {

  public static final String PATTERN = "HH:mm";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new ShortTimeFormat. */
  public ShortTimeFormat() {
    super(PATTERN);
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
