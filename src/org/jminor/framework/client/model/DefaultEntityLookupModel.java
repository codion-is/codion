/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.model.SearchType;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.Configuration;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

/**
 * User: darri
 * Date: 29.6.2010
 * Time: 10:44:06
 */
public class DefaultEntityLookupModel implements EntityLookupModel {

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
  public DefaultEntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final List<Property> lookupProperties) {
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

  public String getDescription() {
    return Util.getListContentsAsString(getLookupProperties(), false);
  }

  public boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  public void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? Arrays.asList(entity) : null);
  }

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

  public List<Entity> getSelectedEntities() {
    return Collections.unmodifiableList(selectedEntities);
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public EntityLookupModel setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

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

  public EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  public String getMultipleValueSeparator() {
    return multipleValueSeparator;
  }

  public EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator) {
    this.multipleValueSeparator = multipleValueSeparator;
    refreshSearchText();
    return this;
  }

  public EntityLookupModel setAdditionalLookupCriteria(final Criteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
    return this;
  }

  public void refreshSearchText() {
    setSearchString(getSelectedEntities().size() == 0 ? "" : toString(getSelectedEntities()));
  }

  public void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    evtSearchStringChanged.fire();
  }

  public String getSearchString() {
    return this.searchString;
  }

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

  public List<Entity> performQuery() {
    try {
      return dbProvider.getEntityDb().selectMany(getEntitySelectCriteria());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Event eventSearchStringChanged() {
    return evtSearchStringChanged;
  }

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

