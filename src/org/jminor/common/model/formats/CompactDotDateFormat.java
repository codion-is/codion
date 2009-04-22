/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class CompactDotDateFormat extends AbstractDateMaskFormat {

  public static final String MASK_STRING = "##.##.##";
  public static final String PATTERN = "dd.MM.yy";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new CompactDotDateFormat. */
  public CompactDotDateFormat() {
    super(PATTERN);
  }

  /** {@inheritDoc} */
  public String getDateMask() {
    return MASK_STRING;
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
