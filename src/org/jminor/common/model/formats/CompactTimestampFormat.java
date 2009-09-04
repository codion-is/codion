/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class CompactTimestampFormat extends SimpleDateFormat {

  public static final String PATTERN = "ddMMyy HH:mm";

  public CompactTimestampFormat() {
    super(PATTERN);
  }
}
