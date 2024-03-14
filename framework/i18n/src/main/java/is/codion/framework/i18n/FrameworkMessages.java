/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.i18n;

import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

/**
 * A class containing shared i18n messages used by the framework.
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
  private static final String SUPPORT_TABLES = "support_tables";
  private static final String SUPPORT_TABLES_MNEMONIC = "support_tables_mnemonic";
  private static final String UPDATE = "update";
  private static final String UPDATE_MNEMONIC = "update_mnemonic";
  private static final String UPDATE_TIP = "update_tip";
  private static final String EDIT = "edit";
  private static final String EDIT_SELECTED_TIP = "edit_selected_tip";
  private static final String DELETE = "delete";
  private static final String DELETE_MNEMONIC = "delete_mnemonic";
  private static final String DELETE_CURRENT_TIP = "delete_current_tip";
  private static final String DELETE_SELECTED_TIP = "delete_selected_tip";
  private static final String DEPENDENCIES = "dependencies";
  private static final String DEPENDENCIES_TIP = "dependencies_tip";
  private static final String ADD = "add";
  private static final String ADD_MNEMONIC = "add_mnemonic";
  private static final String ADD_TIP = "add_tip";
  private static final String SAVE = "save";
  private static final String SAVE_MNEMONIC = "save_mnemonic";
  private static final String CONFIRM_EXIT = "confirm_exit";
  private static final String CONFIRM_EXIT_TITLE = "confirm_exit_title";
  private static final String UNSAVED_DATA_WARNING = "unsaved_data_warning";
  private static final String UNSAVED_DATA_WARNING_TITLE = "unsaved_data_warning_title";
  private static final String CONFIRM_UPDATE = "confirm_update";
  private static final String CONFIRM_DELETE_SELECTED = "confirm_delete_selected";
  private static final String CONFIRM_DELETE = "confirm_delete";
  private static final String CONFIRM_INSERT = "confirm_insert";
  private static final String SHOW = "show";
  private static final String NO_SEARCH_RESULTS = "no_search_results";
  private static final String SEARCH = "search";
  private static final String FILTER = "filter";
  private static final String SEARCH_MNEMONIC = "search_mnemonic";

  private static final String COPY_CELL = "copy_cell";
  private static final String COPY_TABLE_WITH_HEADER = "copy_table_with_header";

  private static final String SETTINGS = "settings";
  private static final String SELECT_INPUT_FIELD = "select_input_field";
  private static final String SELECT_SEARCH_FIELD = "select_search_field";
  private static final String SELECT_FILTER_FIELD = "select_filter_field";

  private FrameworkMessages() {}

  /**
   * @return file
   */
  public static String file() {
    return get(FILE);
  }

  /**
   * @return file mnemonic
   */
  public static char fileMnemonic() {
    return get(FILE_MNEMONIC).charAt(0);
  }

  /**
   * @return exit
   */
  public static String exit() {
    return get(EXIT);
  }

  /**
   * @return exit mnemonic
   */
  public static char exitMnemonic() {
    return get(EXIT_MNEMONIC).charAt(0);
  }

  /**
   * @return exit tip
   */
  public static String exitTip() {
    return get(EXIT_TIP);
  }

  /**
   * @return view
   */
  public static String view() {
    return get(VIEW);
  }

  /**
   * @return view mnemonic
   */
  public static char viewMnemonic() {
    return get(VIEW_MNEMONIC).charAt(0);
  }

  /**
   * @return support tables
   */
  public static String supportTables() {
    return get(SUPPORT_TABLES);
  }

  /**
   * @return support table mnemonic
   */
  public static char supportTablesMnemonic() {
    return get(SUPPORT_TABLES_MNEMONIC).charAt(0);
  }

  /**
   * @return update
   */
  public static String update() {
    return get(UPDATE);
  }

  /**
   * @return update mnemonic
   */
  public static char updateMnemonic() {
    return get(UPDATE_MNEMONIC).charAt(0);
  }

  /**
   * @return update tip
   */
  public static String updateTip() {
    return get(UPDATE_TIP);
  }

  /**
   * @return edit
   */
  public static String edit() {
    return get(EDIT);
  }

  /**
   * @return edit selected tip
   */
  public static String editSelectedTip() {
    return get(EDIT_SELECTED_TIP);
  }

  /**
   * @return delete
   */
  public static String delete() {
    return get(DELETE);
  }

  /**
   * @return delete mnemonic
   */
  public static char deleteMnemonic() {
    return get(DELETE_MNEMONIC).charAt(0);
  }

  /**
   * @return delete current tip
   */
  public static String deleteCurrentTip() {
    return get(DELETE_CURRENT_TIP);
  }

  /**
   * @return delete selected tip
   */
  public static String deleteSelectedTip() {
    return get(DELETE_SELECTED_TIP);
  }

  /**
   * @return view dependencies
   */
  public static String dependencies() {
    return get(DEPENDENCIES);
  }

  /**
   * @return view dependencies tip
   */
  public static String dependenciesTip() {
    return get(DEPENDENCIES_TIP);
  }

  /**
   * @return add
   */
  public static String add() {
    return get(ADD);
  }

  /**
   * @return add mnemonic
   */
  public static char addMnemonic() {
    return get(ADD_MNEMONIC).charAt(0);
  }

  /**
   * @return add tip
   */
  public static String addTip() {
    return get(ADD_TIP);
  }

  /**
   * @return save
   */
  public static String save() {
    return get(SAVE);
  }

  /**
   * @return save mnemonic
   */
  public static char saveMnemonic() {
    return get(SAVE_MNEMONIC).charAt(0);
  }

  /**
   * @return confirm exit
   */
  public static String confirmExit() {
    return get(CONFIRM_EXIT);
  }

  /**
   * @return confirm exit title
   */
  public static String confirmExitTitle() {
    return get(CONFIRM_EXIT_TITLE);
  }

  /**
   * @return unsaved data warning
   */
  public static String unsavedDataWarning() {
    return get(UNSAVED_DATA_WARNING);
  }

  /**
   * @return unsaved data warning title
   */
  public static String unsavedDataWarningTitle() {
    return get(UNSAVED_DATA_WARNING_TITLE);
  }

  /**
   * @return confirm update
   */
  public static String confirmUpdate() {
    return get(CONFIRM_UPDATE);
  }

  /**
   * @param selectionCount the number of selected records
   * @return confirm delete selected
   */
  public static String confirmDeleteSelected(int selectionCount) {
    return format(get(CONFIRM_DELETE_SELECTED), selectionCount);
  }

  /**
   * @return confirm delete
   */
  public static String confirmDelete() {
    return get(CONFIRM_DELETE);
  }

  /**
   * @return confirm insert
   */
  public static String confirmInsert() {
    return get(CONFIRM_INSERT);
  }

  /**
   * @return show
   */
  public static String show() {
    return get(SHOW);
  }

  /**
   * @return no search results
   */
  public static String noSearchResults() {
    return get(NO_SEARCH_RESULTS);
  }

  /**
   * @return search
   */
  public static String search() {
    return get(SEARCH);
  }

  /**
   * @return filter
   */
  public static String filter() {
    return get(FILTER);
  }

  /**
   * @return search mnemonic
   */
  public static char searchMnemonic() {
    return get(SEARCH_MNEMONIC).charAt(0);
  }

  /**
   * @return copy cell
   */
  public static String copyCell() {
    return get(COPY_CELL);
  }

  /**
   * @return copy table with header
   */
  public static String copyTableWithHeader() {
    return get(COPY_TABLE_WITH_HEADER);
  }

  /**
   * @return settings
   */
  public static String settings() {
    return get(SETTINGS);
  }

  /**
   * @return select input field
   */
  public static String selectInputField() {
    return get(SELECT_INPUT_FIELD);
  }

  /**
   * @return select search field
   */
  public static String selectSearchField() {
    return get(SELECT_SEARCH_FIELD);
  }

  /**
   * @return select filter field
   */
  public static String selectFilterField() {
    return get(SELECT_FILTER_FIELD);
  }

  private static String get(String key) {
    return BUNDLE.getString(key);
  }
}
