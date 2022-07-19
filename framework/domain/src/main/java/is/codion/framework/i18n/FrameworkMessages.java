/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.i18n;

import java.util.ResourceBundle;

/**
 * A class containing i18n keys for messages used by the framework.
 */
public final class FrameworkMessages {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(FrameworkMessages.class.getName());

  public static final String FILE = "file";
  public static final String FILE_MNEMONIC = "file_mnemonic";
  public static final String EXIT = "exit";
  public static final String EXIT_MNEMONIC = "exit_mnemonic";
  public static final String EXIT_TIP = "exit_tip";
  public static final String VIEW = "view";
  public static final String VIEW_MNEMONIC = "view_mnemonic";
  public static final String REFRESH_ALL = "refresh_all";
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
  public static final String DELETE_TIP = "delete_tip";
  public static final String UPDATE_SELECTED_TIP = "update_selected_tip";
  public static final String REFRESH = "refresh";
  public static final String REFRESH_MNEMONIC = "refresh_mnemonic";
  public static final String REFRESH_TIP = "refresh_tip";
  public static final String VIEW_DEPENDENCIES = "view_dependencies";
  public static final String VIEW_DEPENDENCIES_TIP = "view_dependencies_tip";
  public static final String ADD = "add";
  public static final String ADD_MNEMONIC = "add_mnemonic";
  public static final String ADD_TIP = "add_tip";
  public static final String SAVE = "save";
  public static final String SAVE_MNEMONIC = "save_mnemonic";
  public static final String EMPTY_USERNAME = "empty_username";
  public static final String CONFIRM_EXIT = "confirm_exit";
  public static final String CONFIRM_EXIT_TITLE = "confirm_exit_title";
  public static final String UNSAVED_DATA_WARNING = "unsaved_data_warning";
  public static final String UNSAVED_DATA_WARNING_TITLE = "unsaved_data_warning_title";
  public static final String CONFIRM_UPDATE = "confirm_update";
  public static final String CONFIRM_DELETE_SELECTED = "confirm_delete_selected";
  public static final String CONFIRM_DELETE_ENTITY = "confirm_delete_entity";
  public static final String CONFIRM_INSERT = "confirm_insert";
  public static final String SHOW = "show";
  public static final String NO_RESULTS_FROM_CONDITION = "no_results_from_condition";
  public static final String SEARCH = "search";
  public static final String SEARCH_MNEMONIC = "search_mnemonic";

  public static final String ADVANCED = "advanced";
  public static final String COPY_CELL = "copy_cell";
  public static final String COPY_TABLE_WITH_HEADER = "copy_table_with_header";

  public static final String SETTINGS = "settings";
  public static final String SELECT_INPUT_FIELD = "select_input_field";

  private FrameworkMessages() {}

  /**
   * Retrieves the locale string associated with the given key
   * @param key the key
   * @return the string associated with the key
   */
  public static String get(String key) {
    return BUNDLE.getString(key);
  }
}
