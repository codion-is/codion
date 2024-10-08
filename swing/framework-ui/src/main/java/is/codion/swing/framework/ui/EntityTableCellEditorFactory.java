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
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;
import java.util.Optional;

import static is.codion.swing.common.ui.component.table.FilterTableCellEditor.filterTableCellEditor;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilterTableCellEditor.Factory} implementation.
 */
public class EntityTableCellEditorFactory implements FilterTableCellEditor.Factory<Attribute<?>> {

	private final SwingEntityEditModel editModel;

	/**
	 * @param editModel the edit model
	 */
	public EntityTableCellEditorFactory(SwingEntityEditModel editModel) {
		this.editModel = requireNonNull(editModel);
	}

	@Override
	public Optional<TableCellEditor> create(FilterTableColumn<Attribute<?>> column) {
		if (nonUpdatableForeignKey(column.identifier())) {
			return Optional.empty();
		}

		EntityComponentFactory<Object, JComponent> componentFactory =
						new DefaultEntityComponentFactory<>((Attribute<Object>) column.identifier());

		return Optional.of(filterTableCellEditor(() ->
						componentFactory.componentValue(editModel, null)));
	}

	/**
	 * @return the edit model
	 */
	protected final SwingEntityEditModel editModel() {
		return editModel;
	}

	private boolean nonUpdatableForeignKey(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;

			return foreignKey.references().stream()
							.map(ForeignKey.Reference::column)
							.map(referenceAttribute -> editModel.entityDefinition().columns().definition(referenceAttribute))
							.filter(ColumnDefinition.class::isInstance)
							.map(ColumnDefinition.class::cast)
							.noneMatch(ColumnDefinition::updatable);
		}

		return false;
	}
}
