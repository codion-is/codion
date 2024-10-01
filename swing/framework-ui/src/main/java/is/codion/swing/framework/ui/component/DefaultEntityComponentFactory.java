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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityComponentFactory} implementation.
 * @param <T> the attribute type
 * @param <C> the component type
 */
public class DefaultEntityComponentFactory<T, C extends JComponent> implements EntityComponentFactory<T, C> {

	private final Attribute<T> attribute;

	/**
	 * @param attribute the attribute for which this factory creates a {@link ComponentValue}
	 */
	public DefaultEntityComponentFactory(Attribute<T> attribute) {
		this.attribute = requireNonNull(attribute);
	}

	@Override
	public ComponentValue<T, C> componentValue(SwingEntityEditModel editModel, T value) {
		requireNonNull(editModel, "editModel");
		EntityComponents inputComponents = entityComponents(editModel.entityDefinition());
		if (attribute instanceof ForeignKey) {
			return createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) value, inputComponents);
		}
		if (attribute.type().isTemporal()) {
			return createTemporalComponentValue(attribute, (Temporal) value, inputComponents);
		}

		return (ComponentValue<T, C>) inputComponents.component(attribute)
						.value(value)
						.buildValue();
	}

	/**
	 * @return the attribute
	 */
	protected final Attribute<T> attribute() {
		return attribute;
	}

	private ComponentValue<T, C> createForeignKeyComponentValue(ForeignKey foreignKey, SwingEntityEditModel editModel,
																															Entity value, EntityComponents inputComponents) {
		if (editModel.entities().definition(foreignKey.referencedType()).smallDataset()) {
			return (ComponentValue<T, C>) inputComponents.foreignKeyComboBox(foreignKey, editModel.createForeignKeyComboBoxModel(foreignKey))
							.value(value)
							.onSetVisible(comboBox -> comboBox.getModel().refresh())
							.buildValue();
		}

		return (ComponentValue<T, C>) inputComponents.foreignKeySearchField(foreignKey, editModel.createForeignKeySearchModel(foreignKey))
						.value(value)
						.buildValue();
	}

	private static <T, A extends Attribute<T>, C extends JComponent> ComponentValue<T, C> createTemporalComponentValue(A attribute,
																																																										 Temporal value,
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
