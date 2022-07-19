/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import java.util.ResourceBundle;

/**
 * Handles i18n for Swing components
 */
public final class SwingMessages {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingMessages.class.getName());

  private SwingMessages() {}

  /**
   * Retrieves the locale string associated with the given key
   * @param key the key
   * @return the string associated with the key
   */
  public static String get(String key) {
    return MESSAGES.getString(key);
  }
}
