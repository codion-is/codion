/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Event;
import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * A interface responsible for doing entity lookups based on a set of criteria properties.
 */
public interface EntityLookupModel extends EntityDataProvider {

  /**
   * @return an Event fired each time the selected entities change
   */
  Event eventSelectedEntitiesChanged();

  /**
   * @return an Event fired each time the search string changes
   */
  Event eventSearchStringChanged();

  /**
   * @return an unmodifiable view of the selected entities
   */
  List<Entity> getSelectedEntities();

  /**
   * Sets the given entity as the selected entity
   * @param entity the entity to set as the selected entity
   */
  void setSelectedEntity(final Entity entity);

  /**
   * Sets the selected entities
   * @param entities the entities to set as selected
   * @throws IllegalArgumentException if this lookup model does not allow multiple selections and <code>entities.size() > 1</code>
   */
  void setSelectedEntities(final List<Entity> entities);

  /**
   * @return a string describing this lookup model, by default a comma separated list of search property names
   */
  String getDescription();

  /**
   * @return true if this lookup model allows selection of multiple entities
   */
  boolean isMultipleSelectionAllowed();

  /**
   * Specifies whether or not this lookup model should allow selection of multiple entities
   * @param multipleSelectionAllowed if true then selection of multiple entities is allowed
   */
  void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed);

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
   * @return true if this lookup model is case sensitive
   */
  boolean isCaseSensitive();

  /**
   * Specifies whether or not this lookup model should be case sensitive
   * @param caseSensitive if true then this lookup model is case sensitive
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setCaseSensitive(final boolean caseSensitive);

  /**
   * @return whether or not to automatically prefix the the search string with a wildcard
   */
  boolean isWildcardPrefix();

  /**
   * Specifies whether or not to automatically prefix the search string with a wildcard
   * @param wildcardPrefix if true then a wildcard is automatically prefixed to the search string
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setWildcardPrefix(final boolean wildcardPrefix);

  /**
   * @return whether or not to automatically append a wildcard to the search string
   */
  boolean isWildcardPostfix();

  /**
   * Specifies whether or not to automatically append a wildcard to the search string
   * @param wildcardPostfix if true then a wildcard is automatically appended to the search string
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setWildcardPostfix(final boolean wildcardPostfix);

  /**
   * Sets the string to use to separate multiple values when shown as a string
   * @param multipleValueSeparator the separator string
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator);

  /**
   * @return the string used to separate multiple values when shown as a string
   */
  String getMultipleValueSeparator();

  /**
   * Sets the additional lookup criteria to use when performing the lookup.
   * This criteria is AND'ed to the actual lookup criteria.
   * @param additionalLookupCriteria the additional lookup criteria
   * @return this EntityLookupModel instance
   */
  EntityLookupModel setAdditionalLookupCriteria(final Criteria additionalLookupCriteria);
}
