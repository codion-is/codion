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

  private static final String FILE = "file";
  private static final String FILE_MNEMONIC = "file_mnemonic";
  private static final String EXIT = "exit";
  private static final String EXIT_MNEMONIC = "exit_mnemonic";
  private static final String EXIT_TIP = "exit_tip";
  private static final String VIEW = "view";
  private static final String VIEW_MNEMONIC = "view_mnemonic";
  private static final String REFRESH_ALL = "refresh_all";
  private static final String SUPPORT_TABLES = "support_tables";
  private static final String SUPPORT_TABLES_MNEMONIC = "support_tables_mnemonic";
  private static final String CLEAR = "clear";
  private static final String CLEAR_MNEMONIC = "clear_mnemonic";
  private static final String CLEAR_TIP = "clear_tip";
  private static final String UPDATE = "update";
  private static final String UPDATE_MNEMONIC = "update_mnemonic";
  private static final String UPDATE_TIP = "update_tip";
  private static final String UPDATE_SELECTED_TIP = "update_selected_tip";
  private static final String DELETE = "delete";
  private static final String DELETE_MNEMONIC = "delete_mnemonic";
  private static final String DELETE_CURRENT_TIP = "delete_current_tip";
  private static final String DELETE_SELECTED_TIP = "delete_selected_tip";
  private static final String REFRESH = "refresh";
  private static final String REFRESH_MNEMONIC = "refresh_mnemonic";
  private static final String REFRESH_TIP = "refresh_tip";
  private static final String VIEW_DEPENDENCIES = "view_dependencies";
  private static final String VIEW_DEPENDENCIES_TIP = "view_dependencies_tip";
  private static final String ADD = "add";
  private static final String ADD_MNEMONIC = "add_mnemonic";
  private static final String ADD_TIP = "add_tip";
  private static final String SAVE = "save";
  private static final String SAVE_MNEMONIC = "save_mnemonic";
  private static final String EMPTY_USERNAME = "empty_username";
  private static final String CONFIRM_EXIT = "confirm_exit";
  private static final String CONFIRM_EXIT_TITLE = "confirm_exit_title";
  private static final String UNSAVED_DATA_WARNING = "unsaved_data_warning";
  private static final String UNSAVED_DATA_WARNING_TITLE = "unsaved_data_warning_title";
  private static final String CONFIRM_UPDATE = "confirm_update";
  private static final String CONFIRM_DELETE_SELECTED = "confirm_delete_selected";
  private static final String CONFIRM_DELETE_ENTITY = "confirm_delete_entity";
  private static final String CONFIRM_INSERT = "confirm_insert";
  private static final String SHOW = "show";
  private static final String NO_RESULTS_FROM_CONDITION = "no_results_from_condition";
  private static final String SEARCH = "search";
  private static final String SEARCH_MNEMONIC = "search_mnemonic";

  private static final String ADVANCED = "advanced";
  private static final String COPY_CELL = "copy_cell";
  private static final String COPY_TABLE_WITH_HEADER = "copy_table_with_header";

  private static final String SETTINGS = "settings";
  private static final String SELECT_INPUT_FIELD = "select_input_field";

  private FrameworkMessages() {}

  public static String file() {
    return get(FILE);
  }

  public static char fileMnemonic() {
    return get(FILE_MNEMONIC).charAt(0);
  }

  public static String exit() {
    return get(EXIT);
  }

  public static char exitMnemonic() {
    return get(EXIT_MNEMONIC).charAt(0);
  }

  public static String exitTip() {
    return get(EXIT_TIP);
  }

  public static String view() {
    return get(VIEW);
  }

  public static char viewMnemonic() {
    return get(VIEW_MNEMONIC).charAt(0);
  }

  public static String refreshAll() {
    return get(REFRESH_ALL);
  }

  public static String supportTables() {
    return get(SUPPORT_TABLES);
  }

  public static char supportTablesMnemonic() {
    return get(SUPPORT_TABLES_MNEMONIC).charAt(0);
  }

  public static String clear() {
    return get(CLEAR);
  }

  public static char clearMnemonic() {
    return get(CLEAR_MNEMONIC).charAt(0);
  }

  public static String clearTip() {
    return get(CLEAR_TIP);
  }

  public static String update() {
    return get(UPDATE);
  }

  public static char updateMnemonic() {
    return get(UPDATE_MNEMONIC).charAt(0);
  }

  public static String updateTip() {
    return get(UPDATE_TIP);
  }

  public static String updateSelectedTip() {
    return get(UPDATE_SELECTED_TIP);
  }

  public static String delete() {
    return get(DELETE);
  }

  public static char deleteMnemonic() {
    return get(DELETE_MNEMONIC).charAt(0);
  }

  public static String deleteCurrentTip() {
    return get(DELETE_CURRENT_TIP);
  }

  public static String deleteSelectedTip() {
    return get(DELETE_SELECTED_TIP);
  }

  public static String refresh() {
    return get(REFRESH);
  }

  public static char refreshMnemonic() {
    return get(REFRESH_MNEMONIC).charAt(0);
  }

  public static String refreshTip() {
    return get(REFRESH_TIP);
  }

  public static String viewDependencies() {
    return get(VIEW_DEPENDENCIES);
  }

  public static String viewDependenciesTip() {
    return get(VIEW_DEPENDENCIES_TIP);
  }

  public static String add() {
    return get(ADD);
  }

  public static char addMnemonic() {
    return get(ADD_MNEMONIC).charAt(0);
  }

  public static String addTip() {
    return get(ADD_TIP);
  }

  public static String save() {
    return get(SAVE);
  }

  public static char saveMnemonic() {
    return get(SAVE_MNEMONIC).charAt(0);
  }

  public static String emptyUsername() {
    return get(EMPTY_USERNAME);
  }

  public static String confirmExit() {
    return get(CONFIRM_EXIT);
  }

  public static String confirmExitTitle() {
    return get(CONFIRM_EXIT_TITLE);
  }

  public static String unsavedDataWarning() {
    return get(UNSAVED_DATA_WARNING);
  }

  public static String unsavedDataWarningTitle() {
    return get(UNSAVED_DATA_WARNING_TITLE);
  }

  public static String confirmUpdate() {
    return get(CONFIRM_UPDATE);
  }

  public static String confirmDeleteSelected() {
    return get(CONFIRM_DELETE_SELECTED);
  }

  public static String confirmDeleteEntity() {
    return get(CONFIRM_DELETE_ENTITY);
  }

  public static String confirmInsert() {
    return get(CONFIRM_INSERT);
  }

  public static String show() {
    return get(SHOW);
  }

  public static String noResultsFromCondition() {
    return get(NO_RESULTS_FROM_CONDITION);
  }

  public static String search() {
    return get(SEARCH);
  }

  public static char searchMnemonic() {
    return get(SEARCH_MNEMONIC).charAt(0);
  }

  public static String advanced() {
    return get(ADVANCED);
  }

  public static String copyCell() {
    return get(COPY_CELL);
  }

  public static String copyTableWithHeader() {
    return get(COPY_TABLE_WITH_HEADER);
  }

  public static String settings() {
    return get(SETTINGS);
  }

  public static String selectInputField() {
    return get(SELECT_INPUT_FIELD);
  }

  private static String get(String key) {
    return BUNDLE.getString(key);
  }
}
