/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class ShortTimeFormat extends SimpleDateFormat {

  public static final String PATTERN = "HH:mm";

  public ShortTimeFormat() {
    super(PATTERN);
  }
}
