/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.i18n;

import java.util.ResourceBundle;

/**
 * A class providing i18n messages.
 */
public final class Messages {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Messages.class.getName());

  private static final String CANCEL = "cancel";
  private static final String CANCEL_MNEMONIC = "cancel_mnemonic";
  private static final String PRINT = "print";
  private static final String PRINT_MNEMONIC = "print_mnemonic";
  private static final String ERROR = "error";
  private static final String YES = "yes";
  private static final String NO = "no";
  private static final String OK = "ok";
  private static final String OK_MNEMONIC = "ok_mnemonic";
  private static final String COPY = "copy";
  private static final String LOGIN = "login";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String SEARCH_FIELD_HINT = "search_field_hint";

  private Messages() {}

  /**
   * @return cancel
   */
  public static String cancel() {
    return get(CANCEL);
  }

  /**
   * @return cancel mnemonic
   */
  public static char cancelMnemonic() {
    return get(CANCEL_MNEMONIC).charAt(0);
  }

  /**
   * @return print
   */
  public static String print() {
    return get(PRINT);
  }

  /**
   * @return print mnemonic
   */
  public static char printMnemonic() {
    return get(PRINT_MNEMONIC).charAt(0);
  }

  /**
   * @return error
   */
  public static String error() {
    return get(ERROR);
  }

  /**
   * @return yes
   */
  public static String yes() {
    return get(YES);
  }

  /**
   * @return no
   */
  public static String no() {
    return get(NO);
  }

  /**
   * @return ok
   */
  public static String ok() {
    return get(OK);
  }

  /**
   * @return ok mnemonic
   */
  public static char okMnemonic() {
    return get(OK_MNEMONIC).charAt(0);
  }

  /**
   * @return copy
   */
  public static String copy() {
    return get(COPY);
  }

  /**
   * @return login
   */
  public static String login() {
    return get(LOGIN);
  }

  /**
   * @return username
   */
  public static String username() {
    return get(USERNAME);
  }

  /**
   * @return password
   */
  public static String password() {
    return get(PASSWORD);
  }

  /**
   * @return the search field hint
   */
  public static String searchFieldHint() {
    return get(SEARCH_FIELD_HINT);
  }

  private static String get(String key) {
    return BUNDLE.getString(key);
  }
}