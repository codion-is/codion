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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;

/**
 * Searches for entities based on a search text and set of String based condition columns.
 * Factory for {@link EntitySearchModel.Builder} instances via {@link EntitySearchModel#builder(EntityType, EntityConnectionProvider)}.
 */
public interface EntitySearchModel {

	/**
	 * Specifies whether searcy models handle entity edit events, by replacing updated entities and removing deleted ones
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see Builder#editEvents(boolean)
	 * @see EntityEditModel#POST_EDIT_EVENTS
	 */
	PropertyValue<Boolean> EDIT_EVENTS =
					booleanValue(EntitySearchModel.class.getName() + ".editEvents", true);

	/**
	 * Specifies the default search result limit, that is, the maximum number of results, null meaning no limit
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<Integer> DEFAULT_LIMIT = integerValue(EntitySearchModel.class.getName() + ".defaultLimit");

	/**
	 * @return the underlying {@link EntityDefinition}
	 */
	EntityDefinition entityDefinition();

	/**
	 * @return the connection provider used by this search model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * @return the {@link Search}
	 */
	Search search();

	/**
	 * @return the selection
	 */
	Selection selection();

	/**
	 * @return the columns used when performing a search
	 */
	Collection<Column<String>> columns();

	/**
	 * @return the {@link Value} controlling the search result limit
	 */
	Value<Integer> limit();

	/**
	 * <p>Controls the additional search condition supplier used when performing the next search data.
	 * <p>The supplied condition is AND'ed to the actual search condition.
	 * <p>NOTE, this does not affect the currently selected value(s), if any.
	 * <p>The condition supplier may not return null, doing so will cause an exception when searching.
	 * @return the {@link Value} controlling the additional condition supplier
	 */
	Value<Supplier<Condition>> condition();

	/**
	 * @return the settings associated with each search column
	 */
	Map<Column<String>, Settings> settings();

	/**
	 * Manages the search strings and provides search results.
	 */
	interface Search {

		/**
		 * @return the {@link ValueSet} representing the search strings
		 */
		ValueSet<String> strings();

		/**
		 * Performs a query based on the current search configuration and returns the result.
		 * Note that the number of search results may be limited via {@link #limit()}.
		 * @return a list containing the entities fulfilling the current condition
		 * @throws IllegalStateException in case no search columns are specified
		 * @see #limit()
		 */
		List<Entity> result();
	}

	/**
	 * Controls the selection
	 */
	interface Selection {

		/**
		 * @return a {@link Value} controlling the selected entity
		 */
		Value<Entity> entity();

		/**
		 * @return a {@link Value} controlling the selected entities
		 */
		ValueSet<Entity> entities();

		/**
		 * @return an {@link ObservableState} indicating whether the selection is empty
		 */
		ObservableState empty();

		/**
		 * Clears the selection
		 */
		void clear();
	}

	/**
	 * Column search settings
	 */
	interface Settings {

		/**
		 * @return a State representing whether a wildcard is automatically prepended to the search string
		 */
		State wildcardPrefix();

		/**
		 * @return a State representing whether a wildcard is automatically appended to the search string
		 */
		State wildcardPostfix();

		/**
		 * @return a State representing whether spaces in the search string are automatically replaced with wildcards
		 */
		State spaceAsWildcard();

		/**
		 * @return a State representing whether the search is case-sensitive
		 */
		State caseSensitive();
	}

	/**
	 * A builder for a {@link EntitySearchModel}.
	 */
	interface Builder {

		/**
		 * @param searchColumns the columns to search by
		 * @return this builder
		 * @throws IllegalArgumentException in case {@code columns} is empty or a column is not associated with the underlying entity
		 */
		Builder searchColumns(Collection<Column<String>> searchColumns);

		/**
		 * <p>The condition supplier may not return null, doing so will cause an exception when searching.
		 * @param condition the additional condition supplier to use when performing the search
		 * @return this builder instance
		 */
		Builder condition(Supplier<Condition> condition);

		/**
		 * @param attributes the attributes to include when querying entities from the database, an empty Collection means all
		 * @return this builder
		 * @throws IllegalArgumentException in case an attribute is not associated with the underlying entity
		 */
		Builder attributes(Collection<Attribute<?>> attributes);

		/**
		 * Defaults to {@link EntityDefinition#orderBy()}.
		 * @param orderBy the {@link OrderBy} to use when querying entities from the database
		 * @return this builder
		 */
		Builder orderBy(OrderBy orderBy);

		/**
		 * @param editEvents controls whether this search model should handle entity edit events, by updating any updated items and removing deleted ones
		 * @return this builder instance
		 * @see EntityEditModel.EditEvents
		 */
		Builder editEvents(boolean editEvents);

		/**
		 * @param limit the search result limit
		 * @return this builder
		 */
		Builder limit(int limit);

		/**
		 * @return a new {@link EntitySearchModel} based on this builder
		 */
		EntitySearchModel build();
	}

	/**
	 * Instantiates a new {@link EntitySearchModel.Builder}, initialized with the search columns for the given entity type
	 * @param entityType the type of the entity to search
	 * @param connectionProvider the EntityConnectionProvider to use when performing the search
	 * @return a new {@link EntitySearchModel.Builder} instance
	 * @see is.codion.framework.domain.entity.EntityDefinition.Columns#searchable()
	 */
	static EntitySearchModel.Builder builder(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new DefaultEntitySearchModel.DefaultBuilder(entityType, connectionProvider);
	}
}
