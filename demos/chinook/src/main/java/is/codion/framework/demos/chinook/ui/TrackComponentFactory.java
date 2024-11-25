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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntitySearchField;

/**
 * Provides a {@link EntitySearchField} using the {@link TrackSelectorFactory}.
 */
final class TrackComponentFactory extends DefaultEntityComponentFactory<Entity, EntitySearchField> {

	TrackComponentFactory(ForeignKey trackForeignKey) {
		super(trackForeignKey);
	}

	@Override
	public ComponentValue<Entity, EntitySearchField> componentValue(SwingEntityEditModel editModel,
																																	Entity value) {
		ComponentValue<Entity, EntitySearchField> componentValue = super.componentValue(editModel, value);
		EntitySearchField trackSearchField = componentValue.component();
		trackSearchField.selectorFactory().set(new TrackSelectorFactory());

		return componentValue;
	}
}
