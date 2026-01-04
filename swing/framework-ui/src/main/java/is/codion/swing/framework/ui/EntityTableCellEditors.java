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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.DefaultEditComponent;
import is.codion.swing.framework.ui.component.EditComponent;

import javax.swing.JComponent;
import java.util.Optional;

final class EntityTableCellEditors implements FilterTableCellEditor.Factory<Entity, Attribute<?>> {

	@Override
	public Optional<FilterTableCellEditor<?, ?>> create(Attribute<?> attribute, FilterTable<Entity, Attribute<?>> table) {
		SwingEntityEditModel editModel = ((SwingEntityTableModel) table.model()).editModel();
		if (nonUpdatableForeignKey(attribute, editModel)) {
			return Optional.empty();
		}

		EditComponent<JComponent, Object> editComponent =
						new DefaultEditComponent<>((Attribute<Object>) attribute);

		return Optional.of(FilterTableCellEditor.builder()
						.component(() -> editComponent.component(editModel))
						.build());
	}

	private static boolean nonUpdatableForeignKey(Attribute<?> attribute, SwingEntityEditModel editModel) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;

			return foreignKey.references().stream()
							.map(ForeignKey.Reference::column)
							.map(referenceAttribute -> editModel.entityDefinition().columns().definition(referenceAttribute))
							.map(ColumnDefinition.class::cast)
							.noneMatch(ColumnDefinition::updatable);
		}

		return false;
	}
}
