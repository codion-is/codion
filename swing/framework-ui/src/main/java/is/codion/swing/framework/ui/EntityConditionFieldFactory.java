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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.FieldFactory;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.component.Components.listBox;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Objects.requireNonNull;

/**
 * A default field factory implementation for attributes.
 */
public final class EntityConditionFieldFactory implements FieldFactory {

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
	public EntityConditionFieldFactory(EntityDefinition entityDefinition, Attribute<?> attribute) {
		this.inputComponents = entityComponents(entityDefinition);
		this.attribute = requireNonNull(attribute);
	}

	@Override
	public boolean supportsType(Class<?> valueClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(valueClass));
	}

	@Override
	public <T> JComponent createEqualField(ConditionModel<T> condition) {
		if (attribute instanceof ForeignKey) {
			return createEqualForeignKeyField(condition);
		}

		return inputComponents.component((Attribute<T>) attribute)
						.link(condition.operands().equal())
						.build();
	}

	@Override
	public <T> Optional<JComponent> createUpperBoundField(ConditionModel<T> condition) {
		Class<?> columnClass = condition.valueClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no upper bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<T>) attribute)
						.link(condition.operands().upperBound())
						.build());
	}

	@Override
	public <T> Optional<JComponent> createLowerBoundField(ConditionModel<T> condition) {
		Class<T> columnClass = condition.valueClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no lower bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<T>) attribute)
						.link(condition.operands().lowerBound())
						.build());
	}

	@Override
	public <T> JComponent createInField(ConditionModel<T> condition) {
		if (attribute instanceof ForeignKey) {
			return createInForeignKeyField((ConditionModel<Entity>) condition);
		}

		return listBox((ComponentValue<T, JComponent>)
						inputComponents.component(attribute)
										.buildValue(), condition.operands().in()).build();
	}

	private <T> JComponent createEqualForeignKeyField(ConditionModel<T> model) {
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).equalSearchModel();

			return inputComponents.foreignKeySearchField((ForeignKey) attribute, searchModel).build();
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) model).equalComboBoxModel();

			return inputComponents.foreignKeyComboBox((ForeignKey) attribute, comboBoxModel)
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(comboBox -> comboBoxModel.refresh())
							.build();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}

	private JComponent createInForeignKeyField(ConditionModel<Entity> model) {
		ForeignKey foreignKey = (ForeignKey) attribute;
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).inSearchModel();

			return configureSearchField(searchModel, inputComponents
							.foreignKeySearchField(foreignKey, searchModel).build());
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((SwingForeignKeyConditionModel) model).inSearchModel();

			return configureSearchField(searchModel, inputComponents
							.foreignKeySearchField(foreignKey, searchModel).build());
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}

	private static EntitySearchField configureSearchField(EntitySearchModel searchModel, EntitySearchField searchField) {
		boolean searchable = !searchModel.connectionProvider().entities()
						.definition(searchModel.entityType()).columns().searchable().isEmpty();
		if (!searchable) {
			searchField.setEditable(false);
			searchField.hint().set("");
		}

		return searchField;
	}
}
