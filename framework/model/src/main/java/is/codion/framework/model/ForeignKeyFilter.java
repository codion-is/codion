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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;

/**
 * A filter based on a foreign key, restricting the entities a model provides to those referencing the filter keys.
 * @see EntityComboBoxModel.Filter#get(ForeignKey)
 * @see EntitySearchModel.Filter#get(ForeignKey)
 */
public interface ForeignKeyFilter {

	/**
	 * Filters the model so that only entities referencing the given key are included.
	 * @param key the key to filter by
	 * @throws IllegalArgumentException in case the key is not of the referenced entity type
	 */
	void set(Entity.Key key);

	/**
	 * Filters the model so that only entities referencing the given keys are included.
	 * If {@code keys} is empty and {@link #strict()} filtering is enabled, all entities are filtered.
	 * @param keys the keys to filter by
	 * @throws IllegalArgumentException in case a key is not of the referenced entity type
	 */
	void set(Collection<Entity.Key> keys);

	/**
	 * @return the current filter keys, an empty collection if this filter is cleared
	 */
	Collection<Entity.Key> get();

	/**
	 * Clears and disables this foreign key filter
	 */
	void clear();

	/**
	 * Controls whether foreign key filtering should be strict or not.
	 * A strict foreign key filter filters all entities if no filter keys are specified and filters
	 * individual entities if the reference key is null. This is true by default.
	 * @return the {@link State} controlling whether foreign key filtering should be strict
	 * @see #set(Collection)
	 */
	State strict();

	/**
	 * Links the given combo box model, representing the referenced entities, to this filter,
	 * so that the selection in the given model filters this model, an entity selected in this
	 * model selecting the entity it references in the given model.
	 * <p>Note that a {@link EntityComboBoxModel} being filtered refreshes {@code filterModel} each time it is refreshed itself.
	 * @param filterModel the combo box model filtering this model
	 * @throws IllegalArgumentException in case the given model is not of the referenced entity type
	 */
	void link(EntityComboBoxModel filterModel);

	/**
	 * Links the given search model, representing the referenced entities, to this filter,
	 * so that the selection in the given model filters this model, an entity selected in this
	 * model selecting the entity it references in the given model.
	 * @param filterModel the search model filtering this model
	 * @throws IllegalArgumentException in case the given model is not of the referenced entity type
	 */
	void link(EntitySearchModel filterModel);
}
