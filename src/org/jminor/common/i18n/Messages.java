/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.i18n;

import javax.swing.UIManager;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A class containing i18n message keys used in the common package.
 */
public final class Messages {

  private Messages() {}

  private static final ResourceBundle BUNDLE =
          ResourceBundle.getBundle("org.jminor.common.i18n.Messages", Locale.getDefault());

  public static final String CANCEL = "cancel";
  public static final String CANCEL_MNEMONIC = "cancel_mnemonic";
  public static final String CLOSE = "close";
  public static final String CLOSE_MNEMONIC = "close_mnemonic";
  public static final String DETAILS = "details";
  public static final String PRINT = "print";
  public static final String SAVE = "save";
  public static final String SAVE_MNEMONIC = "save_mnemonic";
  public static final String EXCEPTION = "exception";
  public static final String MESSAGE = "message";
  public static final String SHOW_DETAILS = "show_details";
  public static final String PRINT_ERROR_REPORT = "print_error_report";
  public static final String PRINT_ERROR_REPORT_MNEMONIC = "print_error_report_mnemonic";
  public static final String CLOSE_DIALOG = "close_dialog";
  public static final String SAVE_ERROR_LOG = "save_error_log";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String OK = "ok";
  public static final String OK_MNEMONIC = "ok_mnemonic";
  public static final String COPY = "copy";
  public static final String COPY_MNEMONIC = "copy_mnemonic";
  public static final String COPY_TO_CLIPBOARD = "copy_to_clipboard";
  public static final String SEND = "send";
  public static final String SEND_MNEMONIC = "send_mnemonic";
  public static final String SEND_EMAIL = "send_email";
  public static final String INPUT_EMAIL_ADDRESS = "input_email_address";
  public static final String MESSAGE_HAS_BEEN_SENT = "message_has_been_sent";
  public static final String EXC_DLG_EMAIL_INSTRUCTIONS = "exc_dlg_email_instructions";
  public static final String FILE_EXISTS = "file_exists";
  public static final String FILE_NOT_FOUND = "file_not_found";
  public static final String OVERWRITE_FILE = "overwrite_file";
  public static final String LOGIN = "login";
  public static final String LOGOUT = "logout";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String CHOOSE_DATE = "choose_date";
  public static final String UNIQUE_KEY_ERROR = "unique_key_error";
  public static final String CHILD_RECORD_ERROR = "child_record_error";
  public static final String NULL_VALUE_ERROR = "null_value_error";
  public static final String INTEGRITY_CONSTRAINT_ERROR = "integrity_constraint_error";
  public static final String CHECK_CONSTRAINT_ERROR = "check_constraint_error";
  public static final String MISSING_PRIVILEGES_ERROR = "missing_privileges_error";
  public static final String LOGIN_CREDENTIALS_ERROR = "login_credentials_error";
  public static final String TABLE_NOT_FOUND_ERROR = "table_not_found_error";
  public static final String USER_UNABLE_TO_CONNECT_ERROR = "user_cannot_connect";
  public static final String VALUE_TOO_LARGE_FOR_COLUMN_ERROR = "value_too_large_for_column_error";
  public static final String VIEW_HAS_ERRORS_ERROR = "view_has_errors_error";
  public static final String DATABASE_EXCEPTION = "database_exception";
  public static final String VALUE_MISSING = "value_missing";
  public static final String SELECT_DATE = "select_date";
  public static final String UNABLE_TO_OPEN_FILE = "unable_to_open_file";
  public static final String RECORD_MODIFIED_EXCEPTION = "record_modified_exception";
  public static final String SELECT_VALUE = "select_value";
  public static final String SELECT_INPUT_FIELD = "select_input_field";
  public static final String SELECT_COLUMNS = "select_columns";
  public static final String SEARCH_FIELD_HINT = "search_field_hint";
  public static final String UNKNOWN_FILE_TYPE = "unknown_file_type";
  public static final String REGULAR_EXPRESSION_SEARCH = "regular_expression_search";
  public static final String SETTINGS = "settings";
  public static final String NO_CONNECTION_AVAILABLE = "no_connection_available";

  static {
    UIManager.put("OptionPane.yesButtonText", get(YES));
    UIManager.put("OptionPane.noButtonText", get(NO));
    UIManager.put("OptionPane.cancelButtonText", get(CANCEL));
    UIManager.put("OptionPane.okButtonText", get(OK));
    UIManager.put("OptionPane.inputDialogTitle", get("OptionPane.inputDialogTitle"));
    UIManager.put("OptionPane.messageDialogTitle", get("OptionPane.messageDialogTitle"));
  }

  public static String get(final String key) {
    return BUNDLE.getString(key);
  }
}