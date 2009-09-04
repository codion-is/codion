/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class FullTimestampFormat extends SimpleDateFormat {

  public static final String PATTERN = "dd-MM-yyyy HH:mm:ss";

  public FullTimestampFormat() {
    super(PATTERN);
  }
}
