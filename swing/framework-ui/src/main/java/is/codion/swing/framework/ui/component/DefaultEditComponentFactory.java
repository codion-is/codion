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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EditComponentFactory} implementation.
 * @param <T> the attribute type
 * @param <C> the component type
 */
public class DefaultEditComponentFactory<T, C extends JComponent> implements EditComponentFactory<T, C> {

	private final Attribute<T> attribute;

	/**
	 * @param attribute the attribute for which this factory creates a {@link ComponentValue}
	 */
	public DefaultEditComponentFactory(Attribute<T> attribute) {
		this.attribute = requireNonNull(attribute);
	}

	@Override
	public ComponentValue<T, C> componentValue(SwingEntityEditModel editModel, T value) {
		requireNonNull(editModel);
		if (attribute instanceof ForeignKey) {
			return createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) value);
		}
		if (attribute.type().isTemporal()) {
			return createTemporalComponentValue(attribute, (Temporal) value, entityComponents(editModel.entityDefinition()));
		}

		return (ComponentValue<T, C>) entityComponents(editModel.entityDefinition())
						.component(attribute)
						.value(value)
						.buildValue();
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
	protected EntityComboBox.Builder comboBox(ForeignKey foreignKey,EntityDefinition entityDefinition,
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
		return entityComponents(entityDefinition).searchField(foreignKey, searchModel).singleSelection();
	}

	private ComponentValue<T, C> createForeignKeyComponentValue(ForeignKey foreignKey, SwingEntityEditModel editModel, Entity value) {
		if (editModel.entities().definition(foreignKey.referencedType()).smallDataset()) {
			return (ComponentValue<T, C>) comboBox(foreignKey, editModel.entityDefinition(), editModel.createComboBoxModel(foreignKey))
							.value(value)
							.onSetVisible(comboBox -> comboBox.getModel().items().refresh())
							.buildValue();
		}

		return (ComponentValue<T, C>) searchField(foreignKey, editModel.entityDefinition(), editModel.createSearchModel(foreignKey))
						.value(value)
						.buildValue();
	}

	private static <T, A extends Attribute<T>, C extends JComponent> ComponentValue<T, C> createTemporalComponentValue(A attribute, Temporal value,
																																																										 EntityComponents inputComponents) {
		if (TemporalFieldPanel.supports((Class<Temporal>) attribute.type().valueClass())) {
			return (ComponentValue<T, C>) inputComponents.temporalFieldPanel((Attribute<Temporal>) attribute)
							.value(value)
							.buildValue();
		}

		return (ComponentValue<T, C>) inputComponents.temporalField((Attribute<Temporal>) attribute)
						.value(value)
						.buildValue();
	}
}
