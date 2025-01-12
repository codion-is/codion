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
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ComponentFactory;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
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
public final class EntityConditionComponentFactory implements ComponentFactory {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class, Entity.class);

	private final EntityComponents inputComponents;
	private final Attribute<?> attribute;

	/**
	 * @param entityDefinition the entity definition
	 * @param attribute the attribute
	 */
	public EntityConditionComponentFactory(EntityDefinition entityDefinition, Attribute<?> attribute) {
		this.inputComponents = entityComponents(entityDefinition);
		this.attribute = requireNonNull(attribute);
	}

	@Override
	public boolean supportsType(Class<?> valueClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(valueClass));
	}

	@Override
	public <T> JComponent component(ConditionModel<T> conditionModel, Value<T> operand) {
		if (attribute instanceof ForeignKey) {
			return createEqualForeignKeyField((ConditionModel<Entity>) conditionModel, (Value<Entity>) operand);
		}

		return inputComponents.component((Attribute<T>) attribute)
						.link(operand)
						.build();
	}

	@Override
	public <T> JComponent component(ConditionModel<T> conditionModel, ValueSet<T> operands) {
		if (attribute instanceof ForeignKey) {
			return createInForeignKeyField((ConditionModel<Entity>) conditionModel, (ValueSet<Entity>) operands);
		}

		return listBox((ComponentValue<T, JComponent>)
						inputComponents.component(attribute)
										.buildValue(), conditionModel.operands().in()).build();
	}

	private JComponent createEqualForeignKeyField(ConditionModel<Entity> model, Value<Entity> operand) {
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).equalSearchModel();

			return inputComponents.searchField((ForeignKey) attribute, searchModel)
							.singleSelection()
							.link(operand)
							.build();
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) model).equalComboBoxModel();

			return inputComponents.comboBox((ForeignKey) attribute, comboBoxModel)
							.link(operand)
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(comboBox -> comboBoxModel.items().refresh())
							.build();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}

	private JComponent createInForeignKeyField(ConditionModel<Entity> model, ValueSet<Entity> operands) {
		ForeignKey foreignKey = (ForeignKey) attribute;
		EntitySearchModel searchModel = getEntitySearchModel(model);

		boolean searchable = !searchModel.entityDefinition().columns().searchable().isEmpty();

		return inputComponents.searchField(foreignKey, searchModel)
						.multiSelection()
						.link(operands)
						//.editable(searchable) todo
						.searchHintEnabled(searchable)
						.build();

	}

	private static EntitySearchModel getEntitySearchModel(ConditionModel<Entity> model) {
		if (model instanceof ForeignKeyConditionModel) {
			return ((ForeignKeyConditionModel) model).inSearchModel();
		}
		else if (model instanceof SwingForeignKeyConditionModel) {
			return ((SwingForeignKeyConditionModel) model).inSearchModel();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}
}
