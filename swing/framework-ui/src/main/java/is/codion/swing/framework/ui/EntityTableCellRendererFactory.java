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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.DefaultUISettings;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jspecify.annotations.Nullable;

import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.darker;

/**
 * A {@link FilterTableCellRenderer.Factory} for {@link EntityTablePanel}
 */
public class EntityTableCellRendererFactory implements FilterTableCellRenderer.Factory<Attribute<?>, SwingEntityTableModel> {

	/**
	 * @param attribute the attribute
	 * @param tableModel the table model
	 * @return a new {@link FilterTableCellRenderer} for the given attribute
	 */
	public FilterTableCellRenderer<?> create(Attribute<?> attribute, SwingEntityTableModel tableModel) {
		return builder(tableModel.entityDefinition().attributes().definition(attribute)).build();
	}

	static <T> FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> configure(AttributeDefinition<T> attributeDefinition,
																																								FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> renderer) {
		renderer.uiSettings(new EntityUISettings())
						.formatter(attributeDefinition::format);
		if (itemBased(attributeDefinition)) {
			// Otherwise the horizontal aligment is based on the Item value type
			renderer.horizontalAlignment(FilterTableCellRenderer.HORIZONTAL_ALIGNMENT.getOrThrow());
		}

		return renderer;
	}

	private static <T> FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> builder(AttributeDefinition<T> attributeDefinition) {
		return configure(attributeDefinition, FilterTableCellRenderer.builder()
						.<Entity, Attribute<?>, T>columnClass(attributeDefinition.attribute().type().valueClass()));
	}

	private static <T> boolean itemBased(AttributeDefinition<T> definition) {
		return definition instanceof ValueAttributeDefinition<?> && !((ValueAttributeDefinition<?>) definition).items().isEmpty();
	}

	private static final class EntityUISettings extends DefaultUISettings<Attribute<?>> {

		private @Nullable Color filteredConditionBackground;
		private @Nullable Color alternateFilteredConditionBackground;

		private @Nullable ObservableState conditionEnabled;
		private boolean conditionEnabledSet = false;

		@Override
		public void update(int leftPadding, int rightPadding) {
			super.update(leftPadding, rightPadding);
			filteredConditionBackground = darker(background(), DOUBLE_DARKENING_FACTOR);
			alternateFilteredConditionBackground = darker(alternateBackground(), DOUBLE_DARKENING_FACTOR);
		}

		@Override
		public Color background(Attribute<?> attribute, boolean alternateRow, Color cellBackgroundColor,
														FilterTableModel<?, Attribute<?>> tableModel) {
			boolean filter = filterEnabled(attribute, tableModel);
			boolean condition = conditionEnabled(attribute, (SwingEntityTableModel) tableModel);
			if (condition || filter) {
				return filteredConditionBackground(alternateRow, condition && filter, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		private boolean conditionEnabled(Attribute<?> attribute, SwingEntityTableModel tableModel) {
			if (conditionEnabledSet) {
				return conditionEnabled != null && conditionEnabled.is();
			}

			ConditionModel<?> condition = tableModel.queryModel().condition().conditionModel().get().get(attribute);
			conditionEnabled = condition == null ? null : condition.enabled();
			conditionEnabledSet = true;

			return conditionEnabled != null && conditionEnabled.is();
		}

		private Color filteredConditionBackground(boolean alternateRow, boolean filterAndConditionEnabled, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow ?
							(filterAndConditionEnabled ? alternateFilteredConditionBackground : alternateFilteredBackground()) :
							(filterAndConditionEnabled ? filteredConditionBackground : filteredBackground());
		}
	}
}
