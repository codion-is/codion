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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.ColorProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jspecify.annotations.Nullable;

import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.darker;

/**
 * @see #builder(Attribute, SwingEntityTableModel)
 * @see #factory()
 */
public final class EntityTableCellRenderer {

	private EntityTableCellRenderer() {}

	/**
	 * @param attribute the attribute
	 * @param tableModel the table model
	 * @param <T> the attribute value type
	 * @return a new {@link FilterTableCellRenderer.Builder} instance for the given attribute
	 */
	public static <T> FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> builder(Attribute<T> attribute,
																																										 SwingEntityTableModel tableModel) {
		EntityDefinition entityDefinition = tableModel.entityDefinition();
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
		ConditionModel<?> queryCondition = tableModel.queryModel()
						.condition()
						.optional(attribute)
						.orElse(null);

		FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> builder = FilterTableCellRenderer.builder()
						.<Entity, Attribute<?>, T>columnClass(attributeDefinition.attribute().type().valueClass())
						.uiSettings(new EntityUISettings(queryCondition))
						.string(attributeDefinition::string);
		if (!attributeDefinition.items().isEmpty()) {
			// Otherwise the horizontal aligment is based on the Item value type
			builder.horizontalAlignment(FilterTableCellRenderer.HORIZONTAL_ALIGNMENT.getOrThrow());
		}

		return builder;
	}

	/**
	 * @return a new default {@link Factory} instance
	 */
	public static Factory factory() {
		return new DefaultFactory();
	}

	/**
	 * @param <T> the attribute value type
	 */
	public interface EntityColorProvider<T> extends ColorProvider<Entity, Attribute<?>, T> {

		@Override
		Color color(FilterTable<Entity, Attribute<?>> table, Entity entity, Attribute<?> attribute, T value);
	}

	/**
	 * A {@link SwingEntityTableModel} based table cell factory.
	 */
	public interface Factory extends FilterTableCellRenderer.Factory<Entity, Attribute<?>> {

		/**
		 * @param attribute the attribute
		 * @param tableModel the table model
		 * @return a new {@link FilterTableCellRenderer}
		 */
		FilterTableCellRenderer<?> create(Attribute<?> attribute, SwingEntityTableModel tableModel);

		@Override
		default FilterTableCellRenderer<?> create(Attribute<?> attribute, FilterTableModel<Entity, Attribute<?>> tableModel) {
			return create(attribute, (SwingEntityTableModel) tableModel);
		}
	}

	private static final class DefaultFactory implements Factory {

		@Override
		public FilterTableCellRenderer<?> create(Attribute<?> attribute, SwingEntityTableModel tableModel) {
			return builder(attribute, tableModel).build();
		}
	}

	private static final class EntityUISettings extends FilterTableCellRenderer.DefaultUISettings {

		private final @Nullable ConditionModel<?> queryCondition;

		private @Nullable Color filteredConditionBackground;
		private @Nullable Color alternateFilteredConditionBackground;

		private EntityUISettings(@Nullable ConditionModel<?> queryCondition) {
			this.queryCondition = queryCondition;
		}

		@Override
		public void update(int leftPadding, int rightPadding) {
			super.update(leftPadding, rightPadding);
			filteredConditionBackground = darker(background(), DOUBLE_DARKENING_FACTOR);
			alternateFilteredConditionBackground = darker(alternateBackground(), DOUBLE_DARKENING_FACTOR);
		}

		@Override
		public Color background(boolean filterEnabled, boolean alternateRow, Color cellBackgroundColor) {
			boolean conditionEnabled = queryCondition != null && queryCondition.enabled().is();
			if (conditionEnabled || filterEnabled) {
				return filteredConditionBackground(alternateRow, conditionEnabled && filterEnabled, cellBackgroundColor);
			}

			return cellBackgroundColor;
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
