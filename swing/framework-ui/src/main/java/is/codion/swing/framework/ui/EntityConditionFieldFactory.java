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

import is.codion.common.model.condition.ColumnConditionModel;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
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
 * A default field factory implementation.
 */
public final class EntityConditionFieldFactory implements FieldFactory<Attribute<?>> {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class, Entity.class);

	private final EntityComponents inputComponents;

	/**
	 * @param entityDefinition the entity definition
	 */
	public EntityConditionFieldFactory(EntityDefinition entityDefinition) {
		this.inputComponents = entityComponents(entityDefinition);
	}

	@Override
	public boolean supportsType(Class<?> valueClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(valueClass));
	}

	@Override
	public JComponent createEqualField(ColumnConditionModel<Attribute<?>, ?> condition) {
		if (condition.identifier() instanceof ForeignKey) {
			return createEqualForeignKeyField(condition);
		}

		return inputComponents.component((Attribute<Object>) condition.identifier())
						.link((Value<Object>) condition.operands().equal())
						.build();
	}

	@Override
	public Optional<JComponent> createUpperBoundField(ColumnConditionModel<Attribute<?>, ?> condition) {
		Class<?> columnClass = condition.valueClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no upper bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<Object>) condition.identifier())
						.link((Value<Object>) condition.operands().upperBound())
						.build());
	}

	@Override
	public Optional<JComponent> createLowerBoundField(ColumnConditionModel<Attribute<?>, ?> condition) {
		Class<?> columnClass = condition.valueClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no lower bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<Object>) condition.identifier())
						.link((Value<Object>) condition.operands().lowerBound())
						.build());
	}

	@Override
	public JComponent createInField(ColumnConditionModel<Attribute<?>, ?> condition) {
		if (condition.identifier() instanceof ForeignKey) {
			return createInForeignKeyField(condition);
		}

		return listBox((ComponentValue<Object, JComponent>)
						inputComponents.component(condition.identifier())
										.buildValue(), (ValueSet<Object>) condition.operands().in()).build();
	}

	private JComponent createEqualForeignKeyField(ColumnConditionModel<? extends Attribute<?>, ?> model) {
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).equalSearchModel();

			return inputComponents.foreignKeySearchField((ForeignKey) model.identifier(), searchModel).build();
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) model).equalComboBoxModel();

			return inputComponents.foreignKeyComboBox((ForeignKey) model.identifier(), comboBoxModel)
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(comboBox -> comboBoxModel.refresh())
							.build();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}

	private JComponent createInForeignKeyField(ColumnConditionModel<? extends Attribute<?>, ?> model) {
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).inSearchModel();

			return configureSearchField(searchModel, inputComponents
							.foreignKeySearchField((ForeignKey) model.identifier(), searchModel).build());
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((SwingForeignKeyConditionModel) model).inSearchModel();

			return configureSearchField(searchModel, inputComponents
							.foreignKeySearchField((ForeignKey) model.identifier(), searchModel).build());
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
