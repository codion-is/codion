/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A class containing i18n message keys used in the framework package.
 */
public final class FrameworkMessages {

  private FrameworkMessages() {}

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.i18n.FrameworkMessages", Locale.getDefault());

  public static final String OptionPane_inputDialogTitle = "OptionPane.inputDialogTitle";
  public static final String OptionPane_messageDialogTitle = "OptionPane.messageDialogTitle";

  public static final String SELECTED = "selected";
  public static final String HIDDEN = "hidden";
  public static final String FILE = "file";
  public static final String FILE_MNEMONIC = "file_mnemonic";
  public static final String EXIT = "exit";
  public static final String EXIT_MNEMONIC = "exit_mnemonic";
  public static final String EXIT_TIP = "exit_tip";
  public static final String SETTINGS = "settings";
  public static final String TOOLS = "tools";
  public static final String TOOLS_MNEMONIC = "tools_mnemonic";
  public static final String VIEW = "view";
  public static final String VIEW_MNEMONIC = "view_mnemonic";
  public static final String REFRESH_ALL = "refresh_all";
  public static final String ALWAYS_ON_TOP = "always_on_top";
  public static final String HELP = "help";
  public static final String HELP_MNEMONIC = "help_mnemonic";
  public static final String ABOUT = "about";
  public static final String VERSION = "version";
  public static final String SUPPORT_TABLES = "support_tables";
  public static final String SUPPORT_TABLES_MNEMONIC = "support_tables_mnemonic";
  public static final String CLEAR = "clear";
  public static final String CLEAR_MNEMONIC = "clear_mnemonic";
  public static final String CLEAR_ALL_TIP = "clear_all_tip";
  public static final String UPDATE = "update";
  public static final String UPDATE_MNEMONIC = "update_mnemonic";
  public static final String UPDATE_TIP = "update_tip";
  public static final String DELETE = "delete";
  public static final String DELETE_MNEMONIC = "delete_mnemonic";
  public static final String DELETE_SELECTED = "delete_selected";
  public static final String DELETE_TIP = "delete_tip";
  public static final String UPDATE_SELECTED = "update_selected";
  public static final String UPDATE_SELECTED_RECORD = "update_selected_record";
  public static final String UPDATE_SELECTED_TIP = "update_selected_tip";
  public static final String REFRESH = "refresh";
  public static final String REFRESH_MNEMONIC = "refresh_mnemonic";
  public static final String REFRESH_TIP = "refresh_tip";
  public static final String PRINT_TABLE = "print_table";
  public static final String VIEW_DEPENDENCIES = "view_dependencies";
  public static final String VIEW_DEPENDENCIES_TIP = "view_dependencies_tip";
  public static final String LIMIT_QUERY = "limit_query";
  public static final String SHOW_ALL = "show_all";
  public static final String SET_PROPERTY_VALUE = "set_property_value";
  public static final String SAVE = "save";
  public static final String INSERT = "insert";
  public static final String INSERT_NEW = "insert_new";
  public static final String INSERT_MNEMONIC = "insert_mnemonic";
  public static final String INSERT_UPDATE = "insert_update";
  public static final String INSERT_UPDATE_TIP = "insert_update_tip";
  public static final String INSERT_TIP = "insert_tip";
  public static final String SELECT_PROPERTY_FOR_UPDATE = "select_property_for_update";
  public static final String EMPTY_USERNAME = "empty_username";
  public static final String EMPTY_PASSWORD = "empty_password";
  public static final String CONFIGURE_QUERY = "configure_query";
  public static final String CONFIRM_EXIT = "confirm_exit";
  public static final String CONFIRM_EXIT_TITLE = "confirm_exit_title";
  public static final String UPDATE_OR_INSERT = "update_or_insert";
  public static final String UPDATE_OR_INSERT_TITLE = "update_or_insert_title";
  public static final String CONFIRM_UPDATE = "confirm_update";
  public static final String CONFIRM_DELETE_SELECTED = "confirm_delete_selected";
  public static final String CONFIRM_DELETE_ENTITY = "confirm_delete_entity";
  public static final String CONFIRM_INSERT = "confirm_insert";
  public static final String REPORT_PRINTER = "report_printer";
  public static final String APPLY = "apply";
  public static final String APPLY_MNEMONIC = "apply_mnemonic";
  public static final String SHOW = "show";
  public static final String NO_RESULTS_FROM_CRITERIA = "no_results_from_criteria";
  public static final String SET_LOG_LEVEL = "set_log_level";
  public static final String SET_LOG_LEVEL_DESC = "set_log_level_desc";
  public static final String NONE_FOUND = "none_found";
  public static final String NO_DEPENDENT_RECORDS = "no_dependent_records";
  public static final String DEPENDENT_RECORDS_FOUND = "dependent_records_found";
  public static final String SELECTION_FILTER = "selection_filter";
  public static final String SELECTION_FILTER_DESC = "selection_filter_desc";
  public static final String CASCADE_REFRESH = "cascade_refresh";
  public static final String CASCADE_REFRESH_DESC = "cascade_refresh_desc";
  public static final String CHECK_DEPS_ON_DELETE = "check_deps_on_delete";
  public static final String CHECK_DEPS_ON_DELETE_DESC = "check_deps_on_delete_desc";
  public static final String RETRY = "retry";
  public static final String RETRY_TITLE = "retry_title";
  public static final String CONDITION = "condition";
  public static final String SEARCH = "search";
  public static final String SEARCH_MNEMONIC = "search_mnemonic";

  public static final String RECORD_NOT_FOUND = "record_not_found";
  public static final String MANY_RECORDS_FOUND = "many_records_found";
  public static final String SELECT_RECORD = "select_record";
  public static final String DETAIL_TABLES = "detail_tables";
  public static final String CLEAR_SELECTION_TIP = "clear_selection_tip";
  public static final String SELECTION_DOWN_TIP = "selection_down_tip";
  public static final String SELECTION_UP_TIP = "selection_up_tip";
  public static final String TOGGLE_DETAIL_TIP = "toggle_detail";
  public static final String TOGGLE_EDIT_TIP = "toggle_edit";
  public static final String SET_LOOK_AND_FEEL = "set_look_and_feel";
  public static final String FILTER_SETTINGS = "filter_settings";
  public static final String REQUIRE_QUERY_CRITERIA = "require_query_criteria";
  public static final String SELECT_ENTITY = "select_entity";
  public static final String TOGGLE_SUMMARY_TIP = "toggle_summary_tip";
  public static final String NONE = "none";
  public static final String SUM = "sum";
  public static final String AVERAGE = "average";
  public static final String MINIMUM = "minimum";
  public static final String MAXIMUM = "maximum";
  public static final String MINIMUM_AND_MAXIMUM = "minimum_and_maximum";
  public static final String ADVANCED = "advanced";
  public static final String APPLICATION_TREE = "view_application_tree";
  public static final String CHANGE_HISTORY = "change_history";
  public static final String COPY_CELL = "copy_cell";
  public static final String COPY_TABLE_WITH_HEADER = "copy_table_with_header";

  public static final String CASE_SENSITIVE = "case_sensitive";
  public static final String POSTFIX_WILDCARD = "postfix_wildcard";
  public static final String PREFIX_WILDCARD = "prefix_wildcard";
  public static final String PROPERTY_VALUE_IS_REQUIRED = "property_value_is_required";
  public static final String FILTER_BY = "filter_by";

  public static final String EXPORT_SELECTED = "export_selected";
  public static final String EXPORT_SELECTED_TIP = "export_selected_tip";
  public static final String EXPORT_SELECTED_DONE = "export_selected_done";

  public static final String PROPERTY_VALUE_TOO_LARGE = "property_value_too_large";
  public static final String PROPERTY_VALUE_TOO_SMALL = "property_value_too_small";

  public static String get(final String key) {
    return bundle.getString(key);
  }

  public static String[] getDefaultConfirmUpdateMessages() {
    return new String[]{get(CONFIRM_UPDATE), get(UPDATE)};
  }

  public static String[] getDefaultConfirmInsertMessages() {
    return new String[]{get(CONFIRM_INSERT), get(INSERT)};
  }

  public static String[] getDefaultConfirmDeleteMessages() {
    return new String[]{get(CONFIRM_DELETE_SELECTED), get(DELETE)};
  }
}
