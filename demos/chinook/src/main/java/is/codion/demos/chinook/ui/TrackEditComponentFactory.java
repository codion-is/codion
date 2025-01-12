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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.ui.component.DefaultEditComponentFactory;
import is.codion.swing.framework.ui.component.EntitySearchField;

/**
 * Provides a {@link EntitySearchField} using the {@link TrackSelectorFactory}.
 */
final class TrackEditComponentFactory extends DefaultEditComponentFactory<Entity, EntitySearchField> {

	TrackEditComponentFactory(ForeignKey trackForeignKey) {
		super(trackForeignKey);
	}

	@Override
	protected EntitySearchField.SingleSelectionBuilder searchField(ForeignKey foreignKey,
																																 EntityDefinition entityDefinition,
																																 EntitySearchModel searchModel) {
		return (EntitySearchField.SingleSelectionBuilder) super.searchField(foreignKey, entityDefinition, searchModel)
						.selectorFactory(new TrackSelectorFactory());
	}
}
