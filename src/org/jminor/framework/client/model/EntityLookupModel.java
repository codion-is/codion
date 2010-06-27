/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class responsible for doing entity lookups based on a set of criteria properties.
 */
public class EntityLookupModel {

  private final Event evtSelectedEntitiesChanged = new Event();
  private final Event evtSearchStringChanged = new Event();

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityID;

  /**
   * The properties to use when doing the lookup
   */
  private final List<Property> lookupProperties;

  /**
   * The selected entities
   */
  private final List<Entity> selectedEntities = new ArrayList<Entity>();

  /**
   * The EntityDbProvider instance used by this EntityLookupModel
   */
  private final EntityDbProvider dbProvider;

  private Criteria additionalLookupCriteria;
  private String searchString = "";
  private boolean multipleSelectionAllowed = true;
  private boolean caseSensitive = false;
  private boolean wildcardPrefix = true;
  private boolean wildcardPostfix = true;
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
  private String multipleValueSeparator = ",";

  /**
   * Instantiates a new EntityLookupModel
   * @param entityID the ID of the entity to lookup
   * @param dbProvider the EntityDbProvider to use when performing the lookup
   * @param lookupProperties the properties to search by, these must be string based
   */
  public EntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final List<Property> lookupProperties) {
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(dbProvider);
    Util.rejectNullValue(lookupProperties);
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
  }

  /**
   * @return the ID of the entity this lookup model is based on
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @return a list containing the properties used when performing a lookup
   */
  public List<Property> getLookupProperties() {
    return Collections.unmodifiableList(lookupProperties);
  }

  /**
   * @return a string describing this lookup model, by default a comma separated list of search property names
   */
  public String getDescription() {
    return Util.getListContentsAsString(getLookupProperties(), false);
  }

  /**
   * @return true if this lookup model allows selection of multiple entities
   */
  public boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  /**
   * Specifies whether or not this lookup model should allow selection of multiple entities
   * @param multipleSelectionAllowed if true then selection of multiple entities is allowed
   */
  public void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  /**
   * Sets the given entity as the selected entity
   * @param entity the entity to set as the selected entity
   */
  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? Arrays.asList(entity) : null);
  }

  /**
   * Sets the selected entities
   * @param entities the entities to set as selected
   * @throws IllegalArgumentException if this lookup model does not allow multiple selections and <code>entities.size() > 1</code>
   */
  public void setSelectedEntities(final List<Entity> entities) {
    if ((entities == null || entities.size() == 0) && this.selectedEntities.size() == 0) {
      return;
    }//no change
    if (entities != null && entities.size() > 1 && !multipleSelectionAllowed) {
      throw new IllegalArgumentException("This EntityLookupModel does not allow the selection of multiple entities");
    }

    this.selectedEntities.clear();
    if (entities != null) {
      this.selectedEntities.addAll(entities);
    }
    refreshSearchText();
    evtSelectedEntitiesChanged.fire();
  }

  /**
   * @return an unmodifiable view of the selected entities
   */
  public List<Entity> getSelectedEntities() {
    return Collections.unmodifiableList(selectedEntities);
  }

  /**
   * @return true if this lookup model is case sensitive
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * Specifies whether or not this lookup model should be case sensitive
   * @param caseSensitive if true then this lookup model is case sensitive
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /**
   * @return whether or not to automatically append a wildcard to the search string
   */
  public boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  /**
   * Specifies whether or not to automatically append a wildcard to the search string
   * @param wildcardPostfix if true then a wildcard is automatically appended to the search string
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
    return this;
  }

  /**
   * @return whether or not to automatically prefix the the search string with a wildcard
   */
  public boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  /**
   * Specifies whether or not to automatically prefix the search string with a wildcard
   * @param wildcardPrefix if true then a wildcard is automatically prefixed to the search string
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
    return this;
  }

  /**
   * @return the wildcard
   */
  public String getWildcard() {
    return wildcard;
  }

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  /**
   * @return the string used to separate multiple values when shown as a string
   */
  public String getMultipleValueSeparator() {
    return multipleValueSeparator;
  }

  /**
   * Sets the string to use to separate multiple values when shown as a string
   * @param multipleValueSeparator the separator string
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator) {
    this.multipleValueSeparator = multipleValueSeparator;
    refreshSearchText();
    return this;
  }

  /**
   * Sets the additional lookup criteria to use when performing the lookup.
   * This criteria is AND'ed to the actual lookup criteria.
   * @param additionalLookupCriteria the additional lookup criteria
   * @return this EntityLookupModel instance
   */
  public EntityLookupModel setAdditionalLookupCriteria(final Criteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
    return this;
  }

  /**
   * Refreshes the search text so that is represents the selected entities
   */
  public void refreshSearchText() {
    setSearchString(getSelectedEntities().size() == 0 ? "" : toString(getSelectedEntities()));
  }

  /**
   * Sets the search string to use when performing the next lookup
   * @param searchString the search string
   */
  public void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    evtSearchStringChanged.fire();
  }

  /**
   * @return the current search string value
   */
  public String getSearchString() {
    return this.searchString;
  }

  /**
   * @return true if the current search string represents the selected entities
   */
  public boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (getSelectedEntities().size() == 0 && searchString.length() == 0)
            || getSelectedEntities().size() > 0 && selectedAsString.equals(searchString);
  }

  /**
   * @return a criteria based on this lookup model including any additional lookup criteria
   * @see #setAdditionalLookupCriteria(org.jminor.common.db.criteria.Criteria)
   */
  public EntitySelectCriteria getEntitySelectCriteria() {
    if (searchString.equals(wildcard)) {
      return new EntitySelectCriteria(entityID);
    }

    final CriteriaSet<Property> baseCriteria = new CriteriaSet<Property>(CriteriaSet.Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowed ? searchString.split(multipleValueSeparator) : new String[] {searchString};
    for (final Property lookupProperty : lookupProperties) {
      for (final String lookupText : lookupTexts) {
        final String modifiedLookupText = (wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : "");
        baseCriteria.addCriteria(new PropertyCriteria(lookupProperty, SearchType.LIKE, modifiedLookupText).setCaseSensitive(caseSensitive));
      }
    }

    return new EntitySelectCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet<Property>(CriteriaSet.Conjunction.AND, additionalLookupCriteria, baseCriteria));
  }

  /**
   * Performs a query based on the select criteria
   * @return a list containing the entities fulfilling the current criteria
   */
  public List<Entity> performQuery() {
    try {
      return dbProvider.getEntityDb().selectMany(getEntitySelectCriteria());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return an Event fired each time the search string changes
   */
  public Event eventSearchStringChanged() {
    return evtSearchStringChanged;
  }

  /**
   * @return an Event fired each time the selected entities change
   */
  public Event eventSelectedEntitiesChanged() {
    return evtSelectedEntitiesChanged;
  }

  private String toString(final List<Entity> entityList) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < entityList.size(); i++) {
      stringBuilder.append(entityList.get(i).toString());
      if (i < entityList.size() - 1) {
        stringBuilder.append(multipleValueSeparator);
      }
    }

    return stringBuilder.toString();
  }
}
