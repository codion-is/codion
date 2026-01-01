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

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.model.ColumnConditionModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionComponents;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComponents;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.listBox;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Objects.requireNonNull;

/**
 * A default component factory implementation for attributes.
 */
public class EntityConditionComponents implements ConditionComponents {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class, Entity.class);

	private final EntityComponents inputComponents;

	/**
	 * @param entityDefinition the entity definition
	 */
	public EntityConditionComponents(EntityDefinition entityDefinition) {
		this.inputComponents = entityComponents(entityDefinition);
	}

	@Override
	public boolean supportsType(Class<?> valueClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(valueClass));
	}

	@Override
	public <T> JComponent equal(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			return createEqualForeignKeyField((ForeignKeyConditionModel) conditionModel);
		}

		return inputComponents.component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().equal())
						.build();
	}

	@Override
	public <T> JComponent lower(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			throw new IllegalArgumentException("Lower bound not supported for foreign key conditions");
		}

		return inputComponents.component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().lower())
						.build();
	}

	@Override
	public <T> JComponent upper(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			throw new IllegalArgumentException("Upper bound not supported for foreign key conditions");
		}

		return inputComponents.component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().upper())
						.build();
	}

	@Override
	public <T> JComponent in(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			return createInForeignKeyField((ForeignKeyConditionModel) conditionModel);
		}

		return listBox()
						.itemValue(inputComponents.component(((ColumnConditionModel<T>) conditionModel).attribute()).buildValue())
						.linkedValue(conditionModel.operands().in())
						.build();
	}

	private JComponent createEqualForeignKeyField(ForeignKeyConditionModel conditionModel) {
		if (conditionModel instanceof SwingForeignKeyConditionModel) {
			EntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) conditionModel).equalComboBoxModel();

			return inputComponents.comboBox(conditionModel.attribute(), comboBoxModel)
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(EntityConditionComponents::refreshIfCleared)
							.build();
		}
		if (conditionModel instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = conditionModel.equalSearchModel();

			return inputComponents.searchField(conditionModel.attribute(), searchModel)
							.singleSelection()
							.build();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + conditionModel);
	}

	private JComponent createInForeignKeyField(ForeignKeyConditionModel conditionModel) {
		EntitySearchModel searchModel = searchModel(conditionModel);

		boolean searchable = !searchModel.entityDefinition().columns().searchable().isEmpty();

		return inputComponents.searchField(conditionModel.attribute(), searchModel)
						.multiSelection()
						.editable(searchable)
						.searchHintEnabled(searchable)
						.build();

	}

	private static EntitySearchModel searchModel(ConditionModel<Entity> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			return ((ForeignKeyConditionModel) conditionModel).inSearchModel();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + conditionModel);
	}

	private static void refreshIfCleared(EntityComboBox comboBox) {
		EntityComboBoxModel model = comboBox.model();
		if (model.items().cleared()) {
			model.items().refresh();
		}
	}
}
