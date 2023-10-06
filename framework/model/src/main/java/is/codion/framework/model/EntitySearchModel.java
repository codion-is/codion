/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Condition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Responsible for performing entity searches based on a search text and set of condition attributes.
 * Factory for {@link EntitySearchModel.Builder} instances via {@link EntitySearchModel#builder(EntityType, EntityConnectionProvider)}.
 */
public interface EntitySearchModel {

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
  Value<Entity> selectedEntity();

  /**
   * @return a Value controlling the selected entities
   */
  ValueSet<Entity> selectedEntities();

  /**
   * @return a string describing this search model, by default a comma separated list of search column names
   */
  String description();

  /**
   * @return the columns used when performing a search
   */
  Collection<Column<String>> searchColumns();

  /**
   * Resets the search string so that is represents the selected entities
   */
  void resetSearchString();

  /**
   * @return the Value controlling the wildcard character
   */
  Value<Character> wildcard();

  /**
   * Performs a query based on the current search configuration
   * @return a list containing the entities fulfilling the current condition
   * @throws IllegalStateException in case no search attributes are specified
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
   * Note that changing this value does not change the actual search string according
   * @return the Value controlling the function providing the {@code toString()} implementation
   * for the entities displayed by this model
   */
  Value<Function<Entity, String>> toStringFunction();

  /**
   * @return a StateObserver indicating whether the search string represents the selected entities
   */
  StateObserver searchStringModified();

  /**
   * @return a StateObserver indicating whether the selection is empty
   */
  StateObserver selectionEmpty();

  /**
   * @return the settings associated with the search Column
   */
  Map<Column<String>, SearchSettings> columnSearchSettings();

  /**
   * @return the Value representing the search string
   */
  Value<String> searchString();

  /**
   * @return the Value representing the text used to separate multiple entities
   */
  Value<String> separator();

  /**
   * Note that changing this state clears the selected entities.
   * @return the State representing the single selection enabled setting
   */
  State singleSelection();

  /**
   * Attribute search settings
   */
  interface SearchSettings {

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
     * @param searchColumns the search columns
     * @return this builder
     * @throws IllegalArgumentException in case {@code searchColumns} is empty
     */
    Builder searchColumns(Collection<Column<String>> searchColumns);

    /**
     * Override the default toString() for search elements when displayed in a field based on this model
     * @param toStringFunction the toString function
     * @return this builder
     */
    Builder toStringFunction(Function<Entity, String> toStringFunction);

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
     * @return a new {@link EntitySearchModel} based on this builder
     */
    EntitySearchModel build();
  }

  /**
   * Instantiates a new {@link EntitySearchModel.Builder}, initialized with the search columns for the given entity type
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @return a new {@link EntitySearchModel.Builder} instance
   * @see is.codion.framework.domain.entity.EntityDefinition.Columns#searchColumns()
   */
  static EntitySearchModel.Builder builder(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return new DefaultEntitySearchModel.DefaultBuilder(entityType, connectionProvider);
  }
}
