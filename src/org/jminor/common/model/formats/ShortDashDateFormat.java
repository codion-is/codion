/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class ShortDashDateFormat extends AbstractDateMaskFormat {

  public static final String MASK_STRING = "##-##-####";
  public static final String PATTERN = "dd-MM-yyyy";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new ShortDashDateFormat. */
  public ShortDashDateFormat() {
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
