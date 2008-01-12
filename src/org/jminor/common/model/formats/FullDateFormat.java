/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 *
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class FullDateFormat extends AbstractDateMaskFormat {

  public static final String MASK_STRING = "##-##-#### ##:##:##";
  public static final String PATTERN = "dd-MM-yyyy HH:mm:ss";

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new SimpleDateFormat(PATTERN);
    }
  };

  /** Constructs a new FullDateFormat. */
  public FullDateFormat() {
    super(PATTERN);
  }

  public String getDateMask() {
    return MASK_STRING;
  }

  public static SimpleDateFormat get() {
    return (SimpleDateFormat)(dateFormat.get());
  }
}
