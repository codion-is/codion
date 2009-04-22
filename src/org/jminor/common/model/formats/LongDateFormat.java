/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class LongDateFormat extends AbstractDateMaskFormat {

  public static final String MASK_STRING = "##-##-#### ##:##";
  public static final String PATTERN = "dd-MM-yyyy HH:mm";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new LongDateFormat. */
  public LongDateFormat() {
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
