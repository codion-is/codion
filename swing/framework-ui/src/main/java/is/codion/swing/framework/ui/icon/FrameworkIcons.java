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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.icon;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.control.ControlIcon;

import java.net.URL;

import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.common.utilities.Configuration.stringValue;

/**
 * Provides icons for framework ui components.
 * The icon color follows the 'Button.foreground' color of the current Look and feel.
 * Add custom icons via {@link #put(String, URL)} and retrieve them via {@link #get(String)}.
 * @see #instance()
 */
public interface FrameworkIcons {

	/**
	 * The default small icon size, note that this will affect the size of buttons
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 16
	 * </ul>
	 */
	PropertyValue<Integer> SMALL_SIZE = integerValue(FrameworkIcons.class.getName() + ".smallSize", 16);

	/**
	 * The default large icon size, note that this will affect the size of buttons
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 20
	 * </ul>
	 */
	PropertyValue<Integer> LARGE_SIZE = integerValue(FrameworkIcons.class.getName() + ".largeSize", 20);

	/**
	 * Specifies the name of the {@link FrameworkIcons} implementation class to use.
	 */
	PropertyValue<String> FRAMEWORK_ICONS = stringValue(FrameworkIcons.class.getName() + ".frameworkIconsName", DefaultFrameworkIcons.class.getName());

	/**
	 * Identifies the filter icon.
	 */
	String FILTER = "filter";

	/**
	 * Identifies the search icon.
	 */
	String SEARCH = "search";

	/**
	 * Identifies the add icon.
	 */
	String ADD = "add";

	/**
	 * Identifies the delete icon.
	 */
	String DELETE = "delete";

	/**
	 * Identifies the update icon.
	 */
	String UPDATE = "update";

	/**
	 * Identifies the copy icon.
	 */
	String COPY = "copy";

	/**
	 * Identifies the refresh icon.
	 */
	String REFRESH = "refresh";

	/**
	 * Identifies the clear icon.
	 */
	String CLEAR = "clear";

	/**
	 * Identifies the up icon.
	 */
	String UP = "up";

	/**
	 * Identifies the down icon.
	 */
	String DOWN = "down";

	/**
	 * Identifies the detail icon.
	 */
	String DETAIL = "detail";

	/**
	 * Identifies the print icon.
	 */
	String PRINT = "print";

	/**
	 * Identifies the edit icon.
	 */
	String EDIT = "edit";

	/**
	 * Identifies the summary icon.
	 */
	String SUMMARY = "summary";

	/**
	 * Identifies the edit-panel icon.
	 */
	String EDIT_PANEL = "edit-panel";

	/**
	 * Identifies the dependencies icon.
	 */
	String DEPENDENCIES = "dependencies";

	/**
	 * Identifies the settings icon.
	 */
	String SETTINGS = "settings";

	/**
	 * Identifies the calendar icon.
	 */
	String CALENDAR = "calendar";

	/**
	 * Identifies the edit-text icon.
	 */
	String EDIT_TEXT = "edit-text";

	/**
	 * Identifies the columns icon.
	 */
	String COLUMNS = "columns";

	/**
	 * Identifies the export icon.
	 */
	String EXPORT = "export";

	/**
	 * Identifies the logo icon.
	 */
	String LOGO = "logo";

	/**
	 * Adds the given icon. Retrieve an icon via {@link #get(String)}.
	 * @param identifier the identifier
	 * @param svgUrl the icon svg resource url
	 * @throws IllegalArgumentException in case an icon has already been associated with the given identifier
	 */
	void put(String identifier, URL svgUrl);

	/**
	 * Retrieves the {@link ControlIcon} associated with the given identifier.
	 * @param identifier the identifier
	 * @return the {@link ControlIcon} associated with the given identifier
	 * @throws IllegalArgumentException in case no icon has been associated with the given identifier
	 * @see #put(String, URL)
	 */
	ControlIcon get(String identifier);

	/**
	 * @return icon for the 'filter' action.
	 */
	ControlIcon filter();

	/**
	 * @return icon for the 'search' action.
	 */
	ControlIcon search();

	/**
	 * @return icon for the 'add' action.
	 */
	ControlIcon add();

	/**
	 * @return icon for the 'delete' action.
	 */
	ControlIcon delete();

	/**
	 * @return icon for the 'update' action.
	 */
	ControlIcon update();

	/**
	 * @return icon for the 'copy' action.
	 */
	ControlIcon copy();

	/**
	 * @return icon for the 'refresh' action.
	 */
	ControlIcon refresh();

	/**
	 * @return icon for the 'clear' action.
	 */
	ControlIcon clear();

	/**
	 * @return icon for the 'up' action.
	 */
	ControlIcon up();

	/**
	 * @return icon for the 'down' action.
	 */
	ControlIcon down();

	/**
	 * @return icon for the 'detail' action.
	 */
	ControlIcon detail();

	/**
	 * @return icon for the 'print' action.
	 */
	ControlIcon print();

	/**
	 * @return icon for the 'clear selection' action.
	 */
	ControlIcon clearSelection();

	/**
	 * @return icon for the 'edit' action.
	 */
	ControlIcon edit();

	/**
	 * @return icon for the 'summary' action.
	 */
	ControlIcon summary();

	/**
	 * @return icon for the 'edit panel' action.
	 */
	ControlIcon editPanel();

	/**
	 * @return icon for the 'dependencies' action.
	 */
	ControlIcon dependencies();

	/**
	 * @return icon for a 'settings' action.
	 */
	ControlIcon settings();

	/**
	 * @return icon for a 'calendar' action
	 */
	ControlIcon calendar();

	/**
	 * @return icon for a 'editText' action
	 */
	ControlIcon editText();

	/**
	 * @return icon for a 'columns' action
	 */
	ControlIcon columns();

	/**
	 * @return the export icon.
	 */
	ControlIcon export();

	/**
	 * @return the logo icon.
	 */
	ControlIcon logo();

	/**
	 * @return a {@link FrameworkIcons} implementation of the type specified by
	 * {@link FrameworkIcons#FRAMEWORK_ICONS}.
	 * @throws IllegalArgumentException in case no such implementation is found
	 */
	static FrameworkIcons instance() {
		return DefaultFrameworkIcons.instance();
	}
}
