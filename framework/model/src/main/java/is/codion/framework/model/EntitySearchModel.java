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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Searches for entities based on a search text and set of String based condition columns.
 * Factory for {@link EntitySearchModel.Builder} instances via {@link EntitySearchModel#builder(EntityType, EntityConnectionProvider)}.
 */
public interface EntitySearchModel {

  /**
   * Specifies the default search result limit, that is, the maximum number of results, null meaning no limit<br>
   * Value type: Integer<br>
   * Default value: null
   */
  PropertyValue<Integer> DEFAULT_LIMIT = Configuration.integerValue("is.codion.framework.model.EntitySearchModel.defaultLimit");

  /**
   * @return the type of the entity this search model is based on
   */
  EntityType entityType();

  /**
   * @return the connection provider used by this search model
   */
  EntityConnectionProvider connectionProvider();

  /**
   * @return a Value controlling the selected entity
   */
  Value<Entity> entity();

  /**
   * @return a Value controlling the selected entities
   */
  ValueSet<Entity> entities();

  /**
   * @return a string describing this search model, by default a comma separated list of search column names
   */
  String description();

  /**
   * @return the columns used when performing a search
   */
  Collection<Column<String>> columns();

  /**
   * Resets the search string so that is represents the selected entities
   */
  void reset();

  /**
   * @return the Value controlling the wildcard character
   */
  Value<Character> wildcard();

  /**
   * @return the value controlling the search result limit
   */
  Value<Integer> limit();

  /**
   * Performs a query based on the current search configuration and returns the result.
   * Note that the number of search results may be limited via {@link #limit()}.
   * @return a list containing the entities fulfilling the current condition
   * @throws IllegalStateException in case no search columns are specified
   * @see #limit()
   */
  List<Entity> search();

  /**
   * Sets the additional search condition supplier to use when performing the next search.
   * This condition is AND'ed to the actual search condition.
   * NOTE, this does not affect the currently selected value(s), if any.
   * @return the Value controlling the additional condition supplier
   */
  Value<Supplier<Condition>> condition();

  /**
   * Note that changing this value does not change the search string accordingly.
   * @return the Value controlling the function providing the {@code toString()} implementation
   * for the entities displayed by this model
   * @see #reset()
   */
  Value<Function<Entity, String>> stringFunction();

  /**
   * @return a StateObserver indicating whether the search string represents the selected entities
   */
  StateObserver searchStringModified();

  /**
   * @return a StateObserver indicating whether the selection is empty
   */
  StateObserver selectionEmpty();

  /**
   * @return the settings associated with each search column
   */
  Map<Column<String>, Settings> settings();

  /**
   * @return the Value representing the search string
   */
  Value<String> searchString();

  /**
   * @return the Value representing the text used to separate multiple entities
   */
  Value<String> separator();

  /**
   * @return true if single selection is enabled
   */
  boolean singleSelection();

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
     * @return a State representing whether the search is case-sensitive
     */
    State caseSensitive();
  }

  /**
   * A builder for a {@link EntitySearchModel}.
   */
  interface Builder {

    /**
     * @param columns the columns to search by
     * @return this builder
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder columns(Collection<Column<String>> columns);

    /**
     * Override the default toString() for search elements when displayed in a field based on this model
     * @param stringFunction the function providing the toString() functionality
     * @return this builder
     */
    Builder stringFunction(Function<Entity, String> stringFunction);

    /**
     * @param description the description
     * @return this builder
     */
    Builder description(String description);

    /**
     * Default false
     * @param singleSelection true if single selection should be enabled
     * @return this builder
     */
    Builder singleSelection(boolean singleSelection);

    /**
     * Default ','
     * @param multipleItemSeparator the text used to separate multiple selected items
     * @return this builder
     */
    Builder separator(String multipleItemSeparator);

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
