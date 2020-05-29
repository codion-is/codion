/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A interface responsible for doing entity lookups based on a set of condition properties.
 */
public interface EntityLookupModel {

  /**
   * @return the id of the entity this lookup model is based on
   */
  String getEntityId();

  /**
   * @return the connection provider used by this lookup model
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
   * @throws IllegalArgumentException if this lookup model does not allow multiple selections and entities.size() is larger than 1
   */
  void setSelectedEntities(List<Entity> entities);

  /**
   * @return a string describing this lookup model, by default a comma separated list of search property names
   */
  String getDescription();

  /**
   * @param description a string describing this lookup model
   */
  void setDescription(String description);

  /**
   * @return a list containing the properties used when performing a lookup
   */
  Collection<ColumnProperty<?>> getLookupProperties();

  /**
   * @param resultSorter the comparator used to sort the lookup result, null if the result should not be sorted
   */
  void setResultSorter(Comparator<Entity> resultSorter);

  /**
   * Refreshes the search text so that is represents the selected entities
   */
  void refreshSearchText();

  /**
   * Sets the search string to use when performing the next lookup
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
   * @throws IllegalStateException in case no lookup properties are specified
   */
  List<Entity> performQuery();

  /**
   * Sets the additional lookup condition provider to use when performing the next lookup.
   * This condition is AND'ed to the actual lookup condition.
   * NOTE, this does not affect the currently selected value(s), if any.
   * @param additionalConditionProvider the additional lookup condition provider
   */
  void setAdditionalConditionProvider(Condition.Provider additionalConditionProvider);

  /**
   * Override the default toString() for lookup elements when displayed
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
   * @return the settings associated with the lookup properties
   */
  Map<ColumnProperty<?>, LookupSettings> getPropertyLookupSettings();

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
   * Property lookup settings
   */
  interface LookupSettings {

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
