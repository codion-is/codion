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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;

import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.REFRESH;

final class ContinentTablePanel extends EntityTablePanel {

	ContinentTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config.includeSouthPanel(false));
		table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		configurePopupMenu(layout -> layout.clear()
						.control(REFRESH));
	}
}
