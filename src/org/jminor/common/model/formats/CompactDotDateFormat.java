/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class CompactDotDateFormat extends SimpleDateFormat {

  public static final String PATTERN = "dd.MM.yy";

  public CompactDotDateFormat() {
    super(PATTERN);
  }
}
