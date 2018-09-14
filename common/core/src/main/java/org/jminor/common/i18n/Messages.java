/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A class containing i18n message keys used in the common package.
 */
public final class Messages {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Messages.class.getName(), Locale.getDefault());

  public static final String CANCEL = "cancel";
  public static final String CANCEL_MNEMONIC = "cancel_mnemonic";
  public static final String CLOSE = "close";
  public static final String PRINT = "print";
  public static final String EXCEPTION = "exception";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String OK = "ok";
  public static final String OK_MNEMONIC = "ok_mnemonic";
  public static final String COPY = "copy";
  public static final String LOGIN = "login";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String FILE_NOT_FOUND = "file_not_found";
  public static final String SELECT_INPUT_FIELD = "select_input_field";
  public static final String SEARCH_FIELD_HINT = "search_field_hint";
  public static final String SETTINGS = "settings";
  public static final String NOT_CONNECTED = "not_connected";

  /**
   * Retrieves the locale string associated with the given key
   * @param key the key
   * @return the string associated with the key
   */
  public static String get(final String key) {
    return BUNDLE.getString(key);
  }
}