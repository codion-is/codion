/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Handles i18n for Swing components
 */
public final class SwingMessages {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingMessages.class.getName(), Locale.getDefault());

  private SwingMessages() {}

  /**
   * Retrieves the locale string associated with the given key
   * @param key the key
   * @return the string associated with the key
   */
  public static String get(final String key) {
    return MESSAGES.getString(key);
  }
}
