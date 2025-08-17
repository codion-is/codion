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

import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.icon.Icons;
import is.codion.swing.common.ui.icon.Logos;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;

import static is.codion.common.Configuration.stringValue;

/**
 * Provides icons for framework ui components.
 * The icon color follows the 'Button.foreground' color of the current Look and feel.
 * Add custom icons via {@link #add(Ikon...)} and retrieve them via {@link #get(Ikon)}.
 * @see #instance()
 */
public interface FrameworkIcons extends Icons, Logos {

	/**
	 * Specifies the name of the {@link FrameworkIcons} implementation class to use.
	 */
	PropertyValue<String> FRAMEWORK_ICONS_CLASSNAME = stringValue(FrameworkIcons.class.getName() + ".frameworkIconsClassName", DefaultFrameworkIcons.class.getName());

	/**
	 * @return icon for the 'filter' action.
	 */
	ImageIcon filter();

	/**
	 * @return icon for the 'search' action.
	 */
	ImageIcon search();

	/**
	 * @return icon for the 'add' action.
	 */
	ImageIcon add();

	/**
	 * @return icon for the 'delete' action.
	 */
	ImageIcon delete();

	/**
	 * @return icon for the 'update' action.
	 */
	ImageIcon update();

	/**
	 * @return icon for the 'copy' action.
	 */
	ImageIcon copy();

	/**
	 * @return icon for the 'refresh' action.
	 */
	ImageIcon refresh();

	/**
	 * @return icon for the 'clear' action.
	 */
	ImageIcon clear();

	/**
	 * @return icon for the 'up' action.
	 */
	ImageIcon up();

	/**
	 * @return icon for the 'down' action.
	 */
	ImageIcon down();

	/**
	 * @return icon for the 'detail' action.
	 */
	ImageIcon detail();

	/**
	 * @return icon for the 'print' action.
	 */
	ImageIcon print();

	/**
	 * @return icon for the 'clear selection' action.
	 */
	ImageIcon clearSelection();

	/**
	 * @return icon for the 'edit' action.
	 */
	ImageIcon edit();

	/**
	 * @return icon for the 'summary' action.
	 */
	ImageIcon summary();

	/**
	 * @return icon for the 'edit panel' action.
	 */
	ImageIcon editPanel();

	/**
	 * @return icon for the 'dependencies' action.
	 */
	ImageIcon dependencies();

	/**
	 * @return icon for a 'settings' action.
	 */
	ImageIcon settings();

	/**
	 * @return icon for a 'calendar' action
	 */
	ImageIcon calendar();

	/**
	 * @return icon for a 'editText' action
	 */
	ImageIcon editText();

	/**
	 * @return icon for a 'columns' action
	 */
	ImageIcon columns();

	/**
	 * @return the logo icon.
	 */
	ImageIcon logo();

	/**
	 * @return a {@link FrameworkIcons} implementation of the type specified by
	 * {@link FrameworkIcons#FRAMEWORK_ICONS_CLASSNAME}.
	 * @throws IllegalArgumentException in case no such implementation is found
	 */
	static FrameworkIcons instance() {
		return DefaultFrameworkIcons.instance();
	}
}
