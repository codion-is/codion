/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class LongCompactDateFormat extends AbstractDateMaskFormat {

  public static final String MASK_STRING = "###### ##:##";
  public static final String PATTERN = "ddMMyy HH:mm";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new LongCompactDateFormat. */
  public LongCompactDateFormat() {
    super(PATTERN);
  }

  /** {@inheritDoc} */
  @Override
  public String getDateMask() {
    return MASK_STRING;
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
