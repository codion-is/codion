/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

public class DateMaskFormat extends SimpleDateFormat {

  public DateMaskFormat(final String pattern) {
    super(pattern);
  }

  /**
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public String getDateMask() {
    return parseDateMask(toPattern());
  }

  private static String parseDateMask(final String datePattern) {
    final StringBuilder ret = new StringBuilder(datePattern.length());
    for (final Character character : datePattern.toCharArray())
      ret.append(Character.isLetter(character) ? "#" : character);

    return ret.toString();
  }
}
