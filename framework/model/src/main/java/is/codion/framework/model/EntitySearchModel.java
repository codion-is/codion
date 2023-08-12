/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
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
   * @return the first selected entity or an empty Optional in case no entity is selected
   */
  Optional<Entity> getSelectedEntity();

  /**
   * @return an unmodifiable view of the selected entities
   */
  List<Entity> getSelectedEntities();

  /**
   * Sets the given entity as the selected entity
   * @param entity the entity to set as the selected entity
   */
  void setSelectedEntity(Entity entity);

  /**
   * Sets the selected entities
   * @param entities the entities to set as selected
   * @throws IllegalArgumentException if this search model does not allow multiple selections and entities.size() is larger than 1
   */
  void setSelectedEntities(List<Entity> entities);

  /**
   * @return a string describing this search model, by default a comma separated list of search attribute names
   */
  String getDescription();

  /**
   * @param description a string describing this search model
   */
  void setDescription(String description);

  /**
   * @return the columns used when performing a search
   */
  Collection<Column<String>> searchColumns();

  /**
   * @param resultSorter the comparator used to sort the search result, null if the result should not be sorted
   */
  void setResultSorter(Comparator<Entity> resultSorter);

  /**
   * Resets the search string so that is represents the selected entities
   */
  void resetSearchString();

  /**
   * Sets the search string to use when performing the next search
   * @param searchString the search string
   */
  void setSearchString(String searchString);

  /**
   * @return the current search string value
   */
  String getSearchString();

  /**
   * @return the Value controlling the wildcard character
   */
  Value<Character> wildcardValue();

  /**
   * @return true if the current search string represents the selected entities
   */
  boolean searchStringRepresentsSelected();

  /**
   * Performs a query based on the select condition
   * @return a list containing the entities fulfilling the current condition
   * @throws IllegalStateException in case no search attributes are specified
   */
  List<Entity> performQuery();

  /**
   * Sets the additional search criteria provider to use when performing the next search.
   * This criteria is AND'ed to the actual search criteria.
   * NOTE, this does not affect the currently selected value(s), if any.
   * @param additionalCriteriaSupplier the additional search criteria provider
   */
  void setAdditionalCriteriaSupplier(Supplier<Criteria> additionalCriteriaSupplier);

  /**
   * Override the default toString() for search elements when displayed
   * in a field based on this model
   * @param toStringProvider provides string representations
   */
  void setToStringProvider(Function<Entity, String> toStringProvider);

  /**
   * @return the toString provider, null if none is specified
   */
  Function<Entity, String> getToStringProvider();

  /**
   * @param listener a listener to be notified each time the selected entities are changed
   */
  void addSelectedEntitiesListener(Consumer<List<Entity>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedEntitiesListener(Consumer<List<Entity>> listener);

  /**
   * @return a StateObserver indicating whether the search string represents the selected entities
   */
  StateObserver searchStringRepresentsSelectedObserver();

  /**
   * @return a StateObserver indicating whether the selection is empty
   */
  StateObserver selectionEmptyObserver();

  /**
   * @return the settings associated with the search Column
   */
  Map<Column<String>, SearchSettings> columnSearchSettings();

  /**
   * @return the Value representing the search string
   */
  Value<String> searchStringValue();

  /**
   * @return the Value representing the multiple item separator setting
   */
  Value<String> multipleItemSeparatorValue();

  /**
   * @return the State representing the single selection enabled setting
   */
  State singleSelectionState();

  /**
   * Attribute search settings
   */
  interface SearchSettings {

    /**
     * @return a State representing whether a wildcard is automatically prepended to the search string
     */
    State wildcardPrefixState();

    /**
     * @return a State representing whether a wildcard is automatically appended to the search string
     */
    State wildcardPostfixState();

    /**
     * @return a State representing whether the search is case-sensitive
     */
    State caseSensitiveState();
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
     * @param toStringProvider the toString provider
     * @return this builder
     */
    Builder toStringProvider(Function<Entity, String> toStringProvider);

    /**
     * @param resultSorter the comparator used to sort the search result, null if the result should not be sorted
     * @return this builder
     */
    Builder resultSorter(Comparator<Entity> resultSorter);

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
    Builder multipleItemSeparator(String multipleItemSeparator);

    /**
     * @return a new {@link EntitySearchModel} based on this builder
     */
    EntitySearchModel build();
  }

  /**
   * Instantiates a new {@link EntitySearchModel.Builder}, initialized with the search properties for the given entity type
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @return a new {@link EntitySearchModel.Builder} instance
   * @see EntityDefinition#searchColumns()
   */
  static EntitySearchModel.Builder builder(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return new DefaultEntitySearchModel.DefaultBuilder(entityType, connectionProvider);
  }
}
