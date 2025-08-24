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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.DefaultEditComponentFactory;
import is.codion.swing.framework.ui.component.EditComponentFactory;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class EntityTableCellEditorFactory implements FilterTableCellEditor.Factory<Attribute<?>> {

	private final SwingEntityEditModel editModel;

	EntityTableCellEditorFactory(SwingEntityEditModel editModel) {
		this.editModel = requireNonNull(editModel);
	}

	@Override
	public Optional<TableCellEditor> create(Attribute<?> attribute) {
		if (nonUpdatableForeignKey(attribute)) {
			return Optional.empty();
		}

		EditComponentFactory<JComponent, Object> componentFactory =
						new DefaultEditComponentFactory<>((Attribute<Object>) attribute);

		return Optional.of(FilterTableCellEditor.builder()
						.component(() -> componentFactory.component(editModel))
						.build());
	}

	private boolean nonUpdatableForeignKey(Attribute<?> attribute) {
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
