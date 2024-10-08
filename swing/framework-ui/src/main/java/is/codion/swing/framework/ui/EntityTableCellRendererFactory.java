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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.Builder;
import is.codion.swing.common.ui.component.table.FilterTableCellRendererFactory;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.model.SwingEntityTableModel;

import static java.util.Objects.requireNonNull;

/**
 * A {@link FilterTableCellRendererFactory} implementation.
 */
public class EntityTableCellRendererFactory implements FilterTableCellRendererFactory<Attribute<?>> {

	private final SwingEntityTableModel tableModel;

	/**
	 * @param tableModel the table model
	 */
	public EntityTableCellRendererFactory(SwingEntityTableModel tableModel) {
		this.tableModel = requireNonNull(tableModel);
	}

	@Override
	public FilterTableCellRenderer create(FilterTableColumn<Attribute<?>> column) {
		return builder(column).build();
	}

	/**
	 * @param column the column
	 * @return a builder for a {@link FilterTableCellRenderer} based on the given column
	 */
	protected final Builder<Attribute<?>> builder(FilterTableColumn<Attribute<?>> column) {
		return new EntityTableCellRendererBuilder(tableModel, requireNonNull(column).identifier());
	}

	/**
	 * @return the table model
	 */
	protected final SwingEntityTableModel tableModel() {
		return tableModel;
	}
}
