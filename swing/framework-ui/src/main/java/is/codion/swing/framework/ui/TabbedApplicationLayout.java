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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * Application layout based on a JTabbedPane.
 */
public interface TabbedApplicationLayout extends EntityApplicationPanel.ApplicationLayout {

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (SwingConstants.TOP, SwingConstants.BOTTOM, SwingConstants.LEFT, SwingConstants.RIGHT)<br>
   * Default value: {@link SwingConstants#TOP}
   */
  PropertyValue<Integer> TAB_PLACEMENT = Configuration.integerValue("is.codion.swing.framework.ui.TabbedApplicationLayout.tabPlacement", SwingConstants.TOP);

  /**
   * @return the application tabbed pane
   */
  JTabbedPane applicationTabPane();
}
