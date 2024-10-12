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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.ui.component.table.DefaultFilterTableCellRendererBuilder;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.Settings;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;

final class EntityTableCellRendererBuilder<T> extends DefaultFilterTableCellRendererBuilder<T> {

	private final ConditionModel<?> queryCondition;

	EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, Attribute<T> attribute) {
		this(requireNonNull(tableModel), tableModel.entityDefinition().attributes().definition(attribute));
	}

	private EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, AttributeDefinition<T> attributeDefinition) {
		super(requireNonNull(attributeDefinition).attribute().type().valueClass());
		requireNonNull(tableModel).entityDefinition().attributes().definition(attributeDefinition.attribute());
		queryCondition = tableModel.queryModel().conditions().optional(attributeDefinition.attribute()).orElse(null);
		filter(tableModel.filters().optional(attributeDefinition.attribute()).orElse(null));
		string(new DefaultString(attributeDefinition));
	}

	@Override
	protected Settings settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
		return new EntitySettings(queryCondition, leftPadding, rightPadding, alternateRowColoring);
	}

	private static final class EntitySettings extends Settings {

		private final ConditionModel<?> queryCondition;

		private Color backgroundColorDoubleShade;
		private Color backgroundColorAlternateDoubleShade;

		private EntitySettings(ConditionModel<?> queryCondition, int leftPadding, int rightPadding, boolean alternateRowColoring) {
			super(leftPadding, rightPadding, alternateRowColoring);
			this.queryCondition = queryCondition;
		}

		@Override
		protected void updateColors() {
			super.updateColors();
			backgroundColorDoubleShade = darker(backgroundColor(), DOUBLE_DARKENING_FACTOR);
			backgroundColorAlternateDoubleShade = darker(backgroundColorAlternate(), DOUBLE_DARKENING_FACTOR);
		}

		@Override
		protected Color backgroundColorShaded(ConditionModel<?> filter, int row, Color cellBackgroundColor) {
			boolean conditionEnabled = queryCondition != null && queryCondition.enabled().get();
			boolean filterEnabled = filter != null && filter.enabled().get();
			if (conditionEnabled || filterEnabled) {
				return backgroundColorShaded(row, conditionEnabled && filterEnabled, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		private Color backgroundColorShaded(int row, boolean doubleShading, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow(row) ?
							(doubleShading ? backgroundColorAlternateDoubleShade : backgroundColorAlternateShaded()) :
							(doubleShading ? backgroundColorDoubleShade : backgroundColorShaded());
		}
	}

	private final class DefaultString implements Function<T, String> {

		private final AttributeDefinition<T> definition;

		private DefaultString(AttributeDefinition<T> attributeDefinition) {
			this.definition = attributeDefinition;
		}

		@Override
		public String apply(T value) {
			return definition.string(value);
		}
	}
}
