/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventInfoListener;
import org.jminor.common.StateObserver;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Value;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A interface responsible for doing entity lookups based on a set of criteria properties.
 */
public interface EntityLookupModel extends EntityDataProvider {

  /**
   * @return an unmodifiable view of the selected entities
   */
  Collection<Entity> getSelectedEntities();

  /**
   * Sets the given entity as the selected entity
   * @param entity the entity to set as the selected entity
   */
  void setSelectedEntity(final Entity entity);

  /**
   * Sets the selected entities
   * @param entities the entities to set as selected
   * @throws IllegalArgumentException if this lookup model does not allow multiple selections and entities.size() is larger than 1
   */
  void setSelectedEntities(final Collection<Entity> entities);

  /**
   * @return a string describing this lookup model, by default a comma separated list of search property names
   */
  String getDescription();

  /**
   * @param description a string describing this lookup model
   */
  void setDescription(final String description);

  /**
   * @return a list containing the properties used when performing a lookup
   */
  Collection<Property.ColumnProperty> getLookupProperties();

  /**
   * @param resultSorter the comparator used to sort the lookup result, null if the result should not be sorted
   */
  void setResultSorter(final Comparator<Entity> resultSorter);

  /**
   * Refreshes the search text so that is represents the selected entities
   */
  void refreshSearchText();

  /**
   * Sets the search string to use when performing the next lookup
   * @param searchString the search string
   */
  void setSearchString(final String searchString);

  /**
   * @return the current search string value
   */
  String getSearchString();

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setWildcard(final String wildcard);

  /**
   * @return the wildcard being used by this model
   */
  String getWildcard();

  /**
   * @return true if the current search string represents the selected entities
   */
  boolean searchStringRepresentsSelected();

  /**
   * Performs a query based on the select criteria
   * @return a list containing the entities fulfilling the current criteria
   */
  List<Entity> performQuery();

  /**
   * Sets the additional lookup criteria to use when performing the next lookup.
   * This criteria is AND'ed to the actual lookup criteria.
   * NOTE, this does not affect the currently selected value(s), if any.
   * @param additionalLookupCriteria the additional lookup criteria
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setAdditionalLookupCriteria(final Criteria<Property.ColumnProperty> additionalLookupCriteria);

  /**
   * Override the default toString() for lookup elements when displayed
   * in a field based on this model
   * @param toStringProvider provides string representations
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setToStringProvider(Entity.ToString toStringProvider);

  /**
   * @return the toString provider, null if none is specified
   */
  Entity.ToString getToStringProvider();

  /**
   * @param listener a listener to be notified each time the selected entities are changed
   */
  void addSelectedEntitiesListener(final EventInfoListener<Collection<Entity>> listener);

  /**
   * @return a StateObserver indicating whether or not the search string represents the selected entities
   */
  StateObserver getSearchStringRepresentsSelectedObserver();

  /**
   * @return the settings associated with the lookup properties
   */
  Map<Property.ColumnProperty, LookupSettings> getPropertyLookupSettings();

  /**
   * @return the Value representing the search string
   */
  Value<String> getSearchStringValue();

  /**
   * @return the Value representing the multiple item separator setting
   */
  Value<String> getMultipleItemSeparatorValue();

  /**
   * @return the Value representing the multiple selection allowed setting
   */
  Value<Boolean> getMultipleSelectionAllowedValue();

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
