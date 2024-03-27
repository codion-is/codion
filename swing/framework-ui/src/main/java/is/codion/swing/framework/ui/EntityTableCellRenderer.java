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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.Builder;
import is.codion.swing.framework.model.SwingEntityTableModel;

/**
 * Provides {@link is.codion.swing.common.ui.component.table.FilteredTableCellRenderer}
 * implementations for EntityTablePanels via {@link #builder(SwingEntityTableModel, Attribute)}.
 */
public interface EntityTableCellRenderer {

	/**
	 * Instantiates a new {@link Builder} with defaults based on the given attribute.
	 * @param tableModel the table model providing the data to render
	 * @param attribute the attribute
	 * @return a new {@link Builder} instance
	 */
	static Builder<Entity, Attribute<?>> builder(SwingEntityTableModel tableModel, Attribute<?> attribute) {
		return new EntityTableCellRendererBuilder(tableModel, attribute);
	}
}
