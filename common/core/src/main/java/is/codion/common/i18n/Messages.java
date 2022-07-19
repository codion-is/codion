/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.i18n;

import java.util.ResourceBundle;

/**
 * A class containing i18n keys for common messages.
 */
public final class Messages {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Messages.class.getName());

  public static final String CANCEL = "cancel";
  public static final String CANCEL_MNEMONIC = "cancel_mnemonic";
  public static final String PRINT = "print";
  public static final String ERROR = "error";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String OK = "ok";
  public static final String OK_MNEMONIC = "ok_mnemonic";
  public static final String COPY = "copy";
  public static final String LOGIN = "login";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String SEARCH_FIELD_HINT = "search_field_hint";

  private Messages() {}

  /**
   * Retrieves the locale string associated with the given key
   * @param key the key
   * @return the string associated with the key
   */
  public static String get(String key) {
    return BUNDLE.getString(key);
  }
}