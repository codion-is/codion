/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.i18n;

import is.codion.common.resource.MessageBundle;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.text.MessageFormat.format;
import static java.util.ResourceBundle.getBundle;

/**
 * A class containing shared i18n messages used by the framework.
 */
public final class FrameworkMessages {

	private static final MessageBundle BUNDLE =
					messageBundle(FrameworkMessages.class, getBundle(FrameworkMessages.class.getName()));

	private static final String FILE = "file";
	private static final String FILE_MNEMONIC = "file_mnemonic";
	private static final String EXIT = "exit";
	private static final String EXIT_MNEMONIC = "exit_mnemonic";
	private static final String EXIT_TIP = "exit_tip";
	private static final String VIEW = "view";
	private static final String VIEW_MNEMONIC = "view_mnemonic";
	private static final String LOOKUP = "lookup";
	private static final String LOOKUP_MNEMONIC = "lookup_mnemonic";
	private static final String UPDATE = "update";
	private static final String UPDATE_MNEMONIC = "update_mnemonic";
	private static final String UPDATE_TIP = "update_tip";
	private static final String EDIT = "edit";
	private static final String EDIT_MNEMONIC = "edit_mnemonic";
	private static final String EDIT_SELECTED_TIP = "edit_selected_tip";
	private static final String DELETE = "delete";
	private static final String DELETE_MNEMONIC = "delete_mnemonic";
	private static final String DELETE_CURRENT_TIP = "delete_current_tip";
	private static final String DELETE_SELECTED_TIP = "delete_selected_tip";
	private static final String DEPENDENCIES = "dependencies";
	private static final String DEPENDENCIES_TIP = "dependencies_tip";
	private static final String INSERT = "insert";
	private static final String INSERT_MNEMONIC = "insert_mnemonic";
	private static final String INSERT_TIP = "insert_tip";
	private static final String ADD = "add";
	private static final String ADD_MNEMONIC = "add_mnemonic";
	private static final String ADD_TIP = "add_tip";
	private static final String SAVE = "save";
	private static final String SAVE_MNEMONIC = "save_mnemonic";
	private static final String CONFIRM_EXIT = "confirm_exit";
	private static final String CONFIRM_EXIT_TITLE = "confirm_exit_title";
	private static final String MODIFIED_WARNING = "modified_warning";
	private static final String MODIFIED_WARNING_TITLE = "modified_warning_title";
	private static final String CONFIRM_UPDATE = "confirm_update";
	private static final String CONFIRM_DELETE = "confirm_delete";
	private static final String CONFIRM_INSERT = "confirm_insert";
	private static final String NO_SEARCH_RESULTS = "no_search_results";
	private static final String SEARCH_NOUN = "search_noun";
	private static final String SEARCH_VERB = "search_verb";
	private static final String FILTER_NOUN = "filter_noun";
	private static final String FILTER_VERB = "filter_verb";
	private static final String SEARCH_MNEMONIC = "search_mnemonic";
	private static final String COPY_TABLE_WITH_HEADER = "copy_table_with_header";

	private static final String SETTINGS = "settings";
	private static final String SELECT_INPUT_FIELD = "select_input_field";

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
	 * @return lookup
	 */
	public static String lookup() {
		return get(LOOKUP);
	}

	/**
	 * @return lookup mnemonic
	 */
	public static char lookupMnemonic() {
		return get(LOOKUP_MNEMONIC).charAt(0);
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
	 * @return edit mnemonic
	 */
	public static char editMnemonic() {
		return get(EDIT_MNEMONIC).charAt(0);
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
	 * @return insert
	 */
	public static String insert() {
		return get(INSERT);
	}

	/**
	 * @return insert mnemonic
	 */
	public static char insertMnemonic() {
		return get(INSERT_MNEMONIC).charAt(0);
	}

	/**
	 * @return insert tip
	 */
	public static String insertTip() {
		return get(INSERT_TIP);
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
	 * @return unsaved modifications warning
	 */
	public static String modifiedWarning() {
		return get(MODIFIED_WARNING);
	}

	/**
	 * @return unsaved modifications warning title
	 */
	public static String modifiedWarningTitle() {
		return get(MODIFIED_WARNING_TITLE);
	}

	/**
	 * @return confirm update
	 */
	public static String confirmUpdate() {
		return get(CONFIRM_UPDATE);
	}

	/**
	 * @param count the number of records about to be deleted
	 * @return confirm delete selected
	 */
	public static String confirmDelete(int count) {
		return format(get(CONFIRM_DELETE), count);
	}

	/**
	 * @return confirm insert
	 */
	public static String confirmInsert() {
		return get(CONFIRM_INSERT);
	}

	/**
	 * @return no search results
	 */
	public static String noSearchResults() {
		return get(NO_SEARCH_RESULTS);
	}

	/**
	 * @return search as a verb
	 */
	public static String searchVerb() {
		return get(SEARCH_VERB);
	}

	/**
	 * @return search as a noun
	 */
	public static String searchNoun() {
		return get(SEARCH_NOUN);
	}

	/**
	 * @return filter as a verb
	 */
	public static String filterVerb() {
		return get(FILTER_VERB);
	}

	/**
	 * @return filter as a noun
	 */
	public static String filterNoun() {
		return get(FILTER_NOUN);
	}

	/**
	 * @return search mnemonic
	 */
	public static char searchMnemonic() {
		return get(SEARCH_MNEMONIC).charAt(0);
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

	private static String get(String key) {
		return BUNDLE.getString(key);
	}
}
