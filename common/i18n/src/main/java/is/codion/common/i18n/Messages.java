/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.i18n;

import java.util.ResourceBundle;

/**
 * A class providing shared i18n messages.
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
  private static final String SEARCH = "search";
  private static final String FIND = "find";
  private static final String CLEAR = "clear";
  private static final String CLEAR_MNEMONIC = "clear_mnemonic";
  private static final String CLEAR_TIP = "clear_tip";
  private static final String ADVANCED = "advanced";
  private static final String REFRESH = "refresh";
  private static final String REFRESH_MNEMONIC = "refresh_mnemonic";
  private static final String REFRESH_TIP = "refresh_tip";

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
   * @return search
   */
  public static String search() {
    return get(SEARCH);
  }

  /**
   * @return clear
   */
  public static String clear() {
    return get(CLEAR);
  }

  /**
   * @return clear tip
   */
  public static String clearTip() {
    return get(CLEAR_TIP);
  }

  /**
   * @return clear mnemonic
   */
  public static char clearMnemonic() {
    return get(CLEAR_MNEMONIC).charAt(0);
  }

  /**
   * @return advanced
   */
  public static String advanced() {
    return get(ADVANCED);
  }

  /**
   * @return find
   */
  public static String find() {
    return get(FIND);
  }

  /**
   * @return refresh
   */
  public static String refresh() {
    return get(REFRESH);
  }

  /**
   * @return refresh mnemonic
   */
  public static char refreshMnemonic() {
    return get(REFRESH_MNEMONIC).charAt(0);
  }

  /**
   * @return refresh tip
   */
  public static String refreshTip() {
    return get(REFRESH_TIP);
  }

  private static String get(String key) {
    return BUNDLE.getString(key);
  }
}