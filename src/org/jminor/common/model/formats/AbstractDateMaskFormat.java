/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public abstract class AbstractDateMaskFormat extends SimpleDateFormat {

  public AbstractDateMaskFormat(final String pattern) {
    super(pattern);
  }

  /**
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public abstract String getDateMask();
}
