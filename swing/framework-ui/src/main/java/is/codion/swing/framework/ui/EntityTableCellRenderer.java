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

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.awt.Color;

import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.alternateRow;
import static java.util.Objects.requireNonNull;

/**
 * @see #builder(Attribute, SwingEntityTableModel)
 * @see #factory(SwingEntityTableModel)
 */
public final class EntityTableCellRenderer {

	private EntityTableCellRenderer() {}

	/**
	 * @param attribute the attribute
	 * @param tableModel the table model
	 * @return a new {@link FilterTableCellRenderer.Builder} instance for the given attribute
	 * @param <T> the attribute value type
	 */
	public static <T> FilterTableCellRenderer.Builder<T> builder(Attribute<T> attribute, SwingEntityTableModel tableModel) {
		EntityDefinition entityDefinition = tableModel.entityDefinition();
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
		ConditionModel<?> queryCondition = tableModel.queryModel()
						.conditions()
						.optional(attribute)
						.orElse(null);

		return FilterTableCellRenderer.builder(attributeDefinition.attribute().type().valueClass())
						.uiSettings(new EntityUISettings(queryCondition))
						.string(attributeDefinition::string);
	}

	/**
	 * @param tableModel the table model
	 * @return a new {@link FilterTableCellRenderer.Factory}
	 */
	public static FilterTableCellRenderer.Factory<Attribute<?>> factory(SwingEntityTableModel tableModel) {
		return new Factory(tableModel);
	}

	private static final class EntityUISettings extends FilterTableCellRenderer.DefaultUISettings {

		private final ConditionModel<?> queryCondition;

		private Color doubleShadedBackgroundColor;
		private Color doubleShadedAlternateBackgroundColor;

		private EntityUISettings(ConditionModel<?> queryCondition) {
			this.queryCondition = queryCondition;
		}

		@Override
		public void update(int leftPadding, int rightPadding) {
			super.update(leftPadding, rightPadding);
			doubleShadedBackgroundColor = darker(backgroundColor(), DOUBLE_DARKENING_FACTOR);
			doubleShadedAlternateBackgroundColor = darker(alternateBackgroundColor(), DOUBLE_DARKENING_FACTOR);
		}

		@Override
		public Color shadedBackgroundColor(boolean filterEnabled, int row, Color cellBackgroundColor) {
			boolean conditionEnabled = queryCondition != null && queryCondition.enabled().get();
			if (conditionEnabled || filterEnabled) {
				return shadedBackgroundColor(row, conditionEnabled && filterEnabled, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		private Color shadedBackgroundColor(int row, boolean doubleShading, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow(row) ?
							(doubleShading ? doubleShadedAlternateBackgroundColor : shadedAlternateBackgroundColor()) :
							(doubleShading ? doubleShadedBackgroundColor : shadedBackgroundColor());
		}
	}

	private static final class Factory implements FilterTableCellRenderer.Factory<Attribute<?>> {

		private final SwingEntityTableModel tableModel;

		Factory(SwingEntityTableModel tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public FilterTableCellRenderer create(Attribute<?> attribute) {
			return builder(requireNonNull(attribute), tableModel).build();
		}
	}
}
