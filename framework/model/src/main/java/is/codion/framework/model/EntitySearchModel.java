/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Responsible for performing entity searches based on a search text and set of condition attributes.
 */
public interface EntitySearchModel {

  /**
   * @return the type of the entity this search model is based on
   */
  EntityType<?> getEntityType();

  /**
   * @return the connection provider used by this search model
   */
  EntityConnectionProvider getConnectionProvider();

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
   * @return the attributes used when performing a search
   */
  Collection<Attribute<String>> getSearchAttributes();

  /**
   * @param resultSorter the comparator used to sort the search result, null if the result should not be sorted
   */
  void setResultSorter(Comparator<Entity> resultSorter);

  /**
   * Refreshes the search text so that is represents the selected entities
   */
  void refreshSearchText();

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
   * Sets the wildcard to use
   * @param wildcard the wildcard
   */
  void setWildcard(String wildcard);

  /**
   * @return the wildcard being used by this model
   */
  String getWildcard();

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
   * Sets the additional search condition provider to use when performing the next search.
   * This condition is AND'ed to the actual search condition.
   * NOTE, this does not affect the currently selected value(s), if any.
   * @param additionalConditionProvider the additional search condition provider
   */
  void setAdditionalConditionProvider(Condition.Provider additionalConditionProvider);

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
  void addSelectedEntitiesListener(EventDataListener<List<Entity>> listener);

  /**
   * @return a StateObserver indicating whether or not the search string represents the selected entities
   */
  StateObserver getSearchStringRepresentsSelectedObserver();

  /**
   * @return the settings associated with the search attributes
   */
  Map<Attribute<String>, SearchSettings> getAttributeSearchSettings();

  /**
   * @return the Value representing the search string
   */
  Value<String> getSearchStringValue();

  /**
   * @return the Value representing the multiple item separator setting
   */
  Value<String> getMultipleItemSeparatorValue();

  /**
   * @return the Value representing the multiple selection enabled setting
   */
  Value<Boolean> getMultipleSelectionEnabledValue();

  /**
   * Attribute search settings
   */
  interface SearchSettings {

    /**
     * @return a Value representing whether or not a wildcard is automatically prepended to the search string
     */
    Value<Boolean> getWildcardPrefixValue();

    /**
     * @return a Value representing whether or not a wildcard is automatically appended to the search string
     */
    Value<Boolean> getWildcardPostfixValue();

    /**
     * @return a Value representing whether or not the search is case sensitive
     */
    Value<Boolean> getCaseSensitiveValue();
  }
}
