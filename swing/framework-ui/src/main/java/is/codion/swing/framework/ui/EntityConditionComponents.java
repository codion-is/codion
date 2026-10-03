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
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.ColumnConditionModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel.Models.Operand;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionComponents;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComponents;

import javax.swing.JComponent;

import static is.codion.swing.common.ui.component.Components.multiInput;
import static is.codion.swing.framework.model.component.SwingEntityComboBoxModel.model;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default component factory implementation for attributes.
 * <p>A foreign key operand component is a combo box when the referenced entity is based on a small dataset,
 * a search field otherwise, based on the models provided by {@link ForeignKeyConditionModel#models()}.
 * <p>An item or enum based column operand component is a combo box including a null item, for clearing the operand,
 * whether or not the column is nullable.
 * <p>Condition models other than {@link ColumnConditionModel} and {@link ForeignKeyConditionModel}, such as the
 * table filters, get the {@link ConditionComponents} defaults.
 * @see EntityDefinition#smallDataset()
 */
public class EntityConditionComponents implements ConditionComponents {

	private final EntityDefinition entityDefinition;
	private final EntityComponents inputComponents;

	/**
	 * @param entityDefinition the entity definition
	 */
	public EntityConditionComponents(EntityDefinition entityDefinition) {
		this.entityDefinition = requireNonNull(entityDefinition);
		this.inputComponents = entityComponents(entityDefinition);
	}

	@Override
	public boolean supports(Class<?> type) {
		return Entity.class.equals(requireNonNull(type)) || ConditionComponents.super.supports(type);
	}

	@Override
	public <T> JComponent equal(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			return createEqualForeignKeyField((ForeignKeyConditionModel) conditionModel);
		}
		if (!(conditionModel instanceof ColumnConditionModel)) {
			return ConditionComponents.super.equal(conditionModel);
		}

		return component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().equal())
						.build();
	}

	@Override
	public <T> JComponent lower(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			throw new IllegalArgumentException("Lower bound not supported for foreign key conditions");
		}
		if (!(conditionModel instanceof ColumnConditionModel)) {
			return ConditionComponents.super.lower(conditionModel);
		}

		return component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().lower())
						.build();
	}

	@Override
	public <T> JComponent upper(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			throw new IllegalArgumentException("Upper bound not supported for foreign key conditions");
		}
		if (!(conditionModel instanceof ColumnConditionModel)) {
			return ConditionComponents.super.upper(conditionModel);
		}

		return component(((ColumnConditionModel<T>) conditionModel).attribute())
						.link(conditionModel.operands().upper())
						.build();
	}

	@Override
	public <T> JComponent in(ConditionModel<T> conditionModel) {
		if (conditionModel instanceof ForeignKeyConditionModel) {
			return createInForeignKeyField((ForeignKeyConditionModel) conditionModel);
		}
		if (!(conditionModel instanceof ColumnConditionModel)) {
			return ConditionComponents.super.in(conditionModel);
		}

		return multiInput()
						.component(component(((ColumnConditionModel<T>) conditionModel).attribute()).buildValue())
						.link(conditionModel.operands().in())
						.format(conditionModel.format().orElse(null))
						.caption(conditionModel.caption().orElse(null))
						.build();
	}

	private <T> ComponentValueBuilder<? extends JComponent, T, ?> component(Column<T> column) {
		// a null item for clearing the operand, a non-nullable column included
		if (!entityDefinition.columns().definition(column).items().isEmpty()) {
			return inputComponents.itemComboBox(column)
							.nullable(true);
		}
		if (column.type().isEnum()) {
			return inputComponents.comboBox(column, SwingFilterComboBoxModel.builder()
							.items(asList(column.type().get().getEnumConstants()))
							.nullable(true)
							.build());
		}

		return inputComponents.component(column);
	}

	private JComponent createEqualForeignKeyField(ForeignKeyConditionModel conditionModel) {
		Operand models = conditionModel.models().equal();
		if (smallDataset(conditionModel.attribute())) {
			return inputComponents.comboBox(conditionModel.attribute(), model(models.comboBoxModel()))
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(EntityConditionComponents::refreshIfCleared)
							.link(conditionModel.operands().equal())
							.build();
		}

		return inputComponents.searchField(conditionModel.attribute(), models.searchModel())
						.link(conditionModel.operands().equal())
						.build();
	}

	private JComponent createInForeignKeyField(ForeignKeyConditionModel conditionModel) {
		return multiInput()
						.component(createInForeignKeyComponent(conditionModel))
						.link(conditionModel.operands().in())
						.caption(conditionModel.caption().orElse(null))
						.build();
	}

	private ComponentValue<? extends JComponent, Entity> createInForeignKeyComponent(ForeignKeyConditionModel conditionModel) {
		Operand models = conditionModel.models().in();
		if (smallDataset(conditionModel.attribute())) {
			// a combo box, the entity selected added with Insert, Enter being the combo box's
			return inputComponents.comboBox(conditionModel.attribute(), model(models.comboBoxModel()))
							.completionMode(Completion.Mode.MAXIMUM_MATCH)
							.onSetVisible(EntityConditionComponents::refreshIfCleared)
							.buildValue();
		}

		EntitySearchModel searchModel = models.searchModel();
		boolean searchable = !searchModel.entityDefinition().columns().searchable().isEmpty();

		// a search field, the entity selected added with Enter or Insert, clearing the field for the next search
		return inputComponents.searchField(conditionModel.attribute(), searchModel)
						.editable(searchable)
						.searchHintEnabled(searchable)
						.buildValue();
	}

	private boolean smallDataset(ForeignKey foreignKey) {
		return entityDefinition.foreignKeys().referencedBy(foreignKey).smallDataset();
	}

	private static void refreshIfCleared(EntityComboBox comboBox) {
		SwingEntityComboBoxModel model = comboBox.model();
		if (model.items().cleared()) {
			model.items().refresh();
		}
	}
}
