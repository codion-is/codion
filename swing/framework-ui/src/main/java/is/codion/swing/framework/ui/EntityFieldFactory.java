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

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractForeignKeyConditionModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.FieldFactory;
import is.codion.swing.common.ui.component.text.TextComponents;
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
import static java.util.Objects.requireNonNull;

final class EntityFieldFactory implements FieldFactory<Attribute<?>> {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class, Entity.class);

	private final EntityComponents inputComponents;

	EntityFieldFactory(EntityComponents inputComponents) {
		this.inputComponents = requireNonNull(inputComponents);
	}

	@Override
	public boolean supportsType(Class<?> columnClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(columnClass));
	}

	@Override
	public JComponent createEqualField(ColumnConditionModel<? extends Attribute<?>, ?> conditionModel) {
		if (conditionModel.columnIdentifier() instanceof ForeignKey) {
			return Sizes.setPreferredHeight(createEqualForeignKeyField(conditionModel), TextComponents.preferredTextFieldHeight());
		}

		return inputComponents.component((Attribute<Object>) conditionModel.columnIdentifier())
						.link((Value<Object>) conditionModel.equalValue())
						.build();
	}

	@Override
	public Optional<JComponent> createUpperBoundField(ColumnConditionModel<? extends Attribute<?>, ?> conditionModel) {
		Class<?> columnClass = conditionModel.columnClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no upper bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<Object>) conditionModel.columnIdentifier())
						.link((Value<Object>) conditionModel.upperBoundValue())
						.build());
	}

	@Override
	public Optional<JComponent> createLowerBoundField(ColumnConditionModel<? extends Attribute<?>, ?> conditionModel) {
		Class<?> columnClass = conditionModel.columnClass();
		if (columnClass.equals(Boolean.class) || columnClass.equals(Entity.class)) {
			return Optional.empty();//no lower bound field required for booleans or entities
		}

		return Optional.of(inputComponents.component((Attribute<Object>) conditionModel.columnIdentifier())
						.link((Value<Object>) conditionModel.lowerBoundValue())
						.build());
	}

	@Override
	public JComponent createInField(ColumnConditionModel<? extends Attribute<?>, ?> conditionModel) {
		if (conditionModel.columnIdentifier() instanceof ForeignKey) {
			return Sizes.setPreferredHeight(createInForeignKeyField(conditionModel), TextComponents.preferredTextFieldHeight());
		}

		return listBox((ComponentValue<Object, JComponent>)
						inputComponents.component(conditionModel.columnIdentifier())
										.buildValue(), (ValueSet<Object>) conditionModel.inValues())
						.build();
	}

	private JComponent createEqualForeignKeyField(ColumnConditionModel<? extends Attribute<?>, ?> model) {
		if (model instanceof ForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).equalSearchModel();

			return inputComponents.foreignKeySearchField((ForeignKey) model.columnIdentifier(), searchModel).build();
		}
		if (model instanceof SwingForeignKeyConditionModel) {
			EntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) model).equalComboBoxModel();

			return inputComponents.foreignKeyComboBox((ForeignKey) model.columnIdentifier(), comboBoxModel)
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(comboBox -> comboBoxModel.refresh())
							.build();
		}

		throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
	}

	private JComponent createInForeignKeyField(ColumnConditionModel<? extends Attribute<?>, ?> model) {
		if (model instanceof AbstractForeignKeyConditionModel) {
			EntitySearchModel searchModel = ((AbstractForeignKeyConditionModel) model).inSearchModel();

			return configureSearchField(searchModel, inputComponents
							.foreignKeySearchField((ForeignKey) model.columnIdentifier(), searchModel).build());
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
