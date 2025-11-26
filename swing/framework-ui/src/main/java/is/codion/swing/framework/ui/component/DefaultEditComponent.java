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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EditComponent} implementation.
 * @param <C> the component type
 * @param <T> the attribute type
 */
public class DefaultEditComponent<C extends JComponent, T> implements EditComponent<C, T> {

	/**
	 * Specifies the default number of text field columns
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 20
	 * </ul>
	 */
	public static final PropertyValue<Integer> DEFAULT_TEXT_FIELD_COLUMNS =
					integerValue(DefaultEditComponent.class.getName() + ".defaultTextFieldColumns", 20);

	private static final int MAXIMUM_TEXT_FIELD_COLUMNS = 30;

	private final Attribute<T> attribute;

	/**
	 * @param attribute the attribute for which this instance creates a {@link ComponentValue}
	 */
	public DefaultEditComponent(Attribute<T> attribute) {
		this.attribute = requireNonNull(attribute);
	}

	@Override
	public ComponentValue<C, T> component(SwingEntityEditModel editModel) {
		requireNonNull(editModel);
		if (attribute instanceof ForeignKey) {
			return createForeignKeyComponentValue((ForeignKey) attribute, editModel);
		}
		if (attribute.type().isTemporal()) {
			return createTemporalComponentValue(attribute, entityComponents(editModel.entityDefinition()));
		}
		AttributeDefinition<T> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);
		if (!(attributeDefinition instanceof ValueAttributeDefinition)) {
			throw new IllegalArgumentException("Value based attribute expected: " + attribute);
		}
		EntityComponents components = entityComponents(editModel.entityDefinition());
		ValueAttributeDefinition<T> definition = (ValueAttributeDefinition<T>) attributeDefinition;
		if (attribute.type().isString() && definition.items().isEmpty()) {
			return (ComponentValue<C, T>) components.textFieldPanel((Attribute<String>) attribute)
							.columns(textFieldColumns((ValueAttributeDefinition<String>) definition))
							.buildValue();
		}

		return (ComponentValue<C, T>) components.component(attribute).buildValue();
	}

	/**
	 * @return the attribute
	 */
	protected final Attribute<T> attribute() {
		return attribute;
	}

	/**
	 * @param foreignKey the foreign key
	 * @param entityDefinition the entity definition
	 * @param comboBoxModel the {@link EntityComboBoxModel} to base the combo box on
	 * @return a {@link EntityComboBox.Builder} instance
	 */
	protected EntityComboBox.Builder comboBox(ForeignKey foreignKey, EntityDefinition entityDefinition,
																						EntityComboBoxModel comboBoxModel) {
		return EntityComponents.entityComponents(entityDefinition).comboBox(foreignKey, comboBoxModel);
	}

	/**
	 * @param foreignKey the foreign key
	 * @param entityDefinition the entity definition
	 * @param searchModel the {@link EntitySearchModel} to base the search field on
	 * @return a {@link EntitySearchField.SingleSelectionBuilder} instance
	 * @throws IllegalArgumentException in case {@code searchModel} is not configured for single selection
	 */
	protected EntitySearchField.SingleSelectionBuilder searchField(ForeignKey foreignKey, EntityDefinition entityDefinition,
																																 EntitySearchModel searchModel) {
		return entityComponents(entityDefinition).searchField(foreignKey, searchModel)
						.singleSelection()
						.searchOnFocusLost(false);
	}

	private ComponentValue<C, T> createForeignKeyComponentValue(ForeignKey foreignKey, SwingEntityEditModel editModel) {
		if (editModel.entities().definition(foreignKey.referencedType()).smallDataset()) {
			return (ComponentValue<C, T>) comboBox(foreignKey, editModel.entityDefinition(), editModel.createComboBoxModel(foreignKey))
							.onSetVisible(comboBox -> comboBox.getModel().items().refresh())
							.buildValue();
		}

		return (ComponentValue<C, T>) searchField(foreignKey, editModel.entityDefinition(), editModel.createSearchModel(foreignKey))
						.buildValue();
	}

	private static <T, A extends Attribute<T>, C extends JComponent> ComponentValue<C, T> createTemporalComponentValue(A attribute,
																																																										 EntityComponents inputComponents) {
		if (TemporalFieldPanel.supports((Class<Temporal>) attribute.type().valueClass())) {
			return (ComponentValue<C, T>) inputComponents.temporalFieldPanel((Attribute<Temporal>) attribute).buildValue();
		}

		return (ComponentValue<C, T>) inputComponents.temporalField((Attribute<Temporal>) attribute).buildValue();
	}

	private static int textFieldColumns(ValueAttributeDefinition<String> definition) {
		int defaultTextFieldColumns = DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow();
		if (definition.maximumLength() > defaultTextFieldColumns) {
			return MAXIMUM_TEXT_FIELD_COLUMNS;
		}

		return defaultTextFieldColumns;
	}
}
