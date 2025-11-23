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
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.Configurer;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

import static is.codion.swing.common.ui.color.Colors.darker;
import static java.util.Objects.requireNonNull;

/**
 * A {@link FilterTableCellRenderer.Factory} for {@link EntityTablePanel}
 */
public class EntityTableCellRendererFactory implements FilterTableCellRenderer.Factory<Entity, Attribute<?>> {

	/**
	 * @param attribute the attribute
	 * @param table the table
	 * @return a new {@link FilterTableCellRenderer} for the given attribute
	 */
	public final FilterTableCellRenderer<Entity, Attribute<?>, ?> create(Attribute<?> attribute, FilterTable<Entity, Attribute<?>> table) {
		SwingEntityTableModel model = (SwingEntityTableModel) requireNonNull(table).model();

		return create(model.entityDefinition().attributes().definition(attribute), model);
	}

	/**
	 * @param attributeDefinition the attribute definition
	 * @param tableModel the table model
	 * @param <T> the attribute type
	 * @return a new {@link FilterTableCellRenderer} for the given attribute
	 */
	protected <T> FilterTableCellRenderer<Entity, Attribute<?>, T> create(AttributeDefinition<T> attributeDefinition, SwingEntityTableModel tableModel) {
		return builder(attributeDefinition).build();
	}

	/**
	 * @param attributeDefinition the attribute definition
	 * @param <T> the attribute value type
	 * @return a {@link FilterTableCellRenderer.Builder} based on the given attribute
	 */
	protected <T> FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> builder(AttributeDefinition<T> attributeDefinition) {
		return configure(requireNonNull(attributeDefinition), FilterTableCellRenderer.<Entity, Attribute<?>>builder()
						.columnClass(attributeDefinition.attribute().type().valueClass()));
	}

	static <T> FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> configure(AttributeDefinition<T> attributeDefinition,
																																								FilterTableCellRenderer.Builder<Entity, Attribute<?>, T> renderer) {
		renderer.formatter(attributeDefinition::format)
						.configurer(new ConditionIndicator());
		if (itemBased(attributeDefinition)) {
			// Otherwise the horizontal aligment is based on the Item value type
			renderer.horizontalAlignment(FilterTableCellRenderer.HORIZONTAL_ALIGNMENT.getOrThrow());
		}

		return renderer;
	}

	private static <T> boolean itemBased(AttributeDefinition<T> definition) {
		return definition instanceof ValueAttributeDefinition<?> && !((ValueAttributeDefinition<?>) definition).items().isEmpty();
	}

	static final class ConditionIndicator implements Configurer<Entity, Attribute<?>> {

		private static final double DARKENING_FACTOR = 0.9;

		private @Nullable ObservableState conditionEnabled;
		private boolean conditionEnabledSet = false;

		boolean enabled;

		@Override
		public void configure(FilterTable<Entity, Attribute<?>> table, Attribute<?> attribute, JComponent component) {
			if (conditionEnabled(attribute, (SwingEntityTableModel) table.model())) {
				component.setBackground(darker(component.getBackground(), DARKENING_FACTOR));
			}
		}

		@Override
		public boolean enabled() {
			return enabled;
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
	}
}
