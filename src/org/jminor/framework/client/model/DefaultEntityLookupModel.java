/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: darri
 * Date: 29.6.2010
 * Time: 10:44:06
 */
public class DefaultEntityLookupModel implements EntityLookupModel {

  private final Event evtSelectedEntitiesChanged = Events.event();
  private final Event evtSearchStringChanged = Events.event();

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityID;

  /**
   * The properties to use when doing the lookup
   */
  private final List<Property.ColumnProperty> lookupProperties;

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
  public DefaultEntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final List<Property.ColumnProperty> lookupProperties) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(dbProvider, "dbProvider");
    Util.rejectNullValue(lookupProperties, "lookupProperties");
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
  }

  public String getDescription() {
    return Util.getCollectionContentsAsString(getLookupProperties(), false);
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return a list containing the properties used when performing a lookup
   */
  public final List<Property.ColumnProperty> getLookupProperties() {
    return Collections.unmodifiableList(lookupProperties);
  }

  public final boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  public final void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  public final void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? Arrays.asList(entity) : null);
  }

  public final void setSelectedEntities(final List<Entity> entities) {
    if ((entities == null || entities.isEmpty()) && this.selectedEntities.isEmpty()) {
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

  public final List<Entity> getSelectedEntities() {
    return Collections.unmodifiableList(selectedEntities);
  }

  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  public final EntityLookupModel setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public final boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  public final EntityLookupModel setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
    return this;
  }

  public final boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  public final EntityLookupModel setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
    return this;
  }

  public final String getWildcard() {
    return wildcard;
  }

  public final EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  public final String getMultipleValueSeparator() {
    return multipleValueSeparator;
  }

  public final EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator) {
    this.multipleValueSeparator = multipleValueSeparator;
    refreshSearchText();
    return this;
  }

  public final EntityLookupModel setAdditionalLookupCriteria(final Criteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
    return this;
  }

  public final void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
  }

  public final void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    evtSearchStringChanged.fire();
  }

  public final String getSearchString() {
    return this.searchString;
  }

  public final boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && searchString.length() == 0)
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchString);
  }

  public final List<Entity> performQuery() {
    try {
      return dbProvider.getEntityDb().selectMany(getEntitySelectCriteria());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final EventObserver searchStringObserver() {
    return evtSearchStringChanged;
  }

  public final void addSearchStringListener(final ActionListener listener) {
    evtSearchStringChanged.addListener(listener);
  }

  public final void addSelectedEntitiesListener(final ActionListener listener) {
    evtSelectedEntitiesChanged.addListener(listener);
  }

  public final void removeSearchStringListener(final ActionListener listener) {
    evtSearchStringChanged.removeListener(listener);
  }

  public final void removeSelectedEntitiesListener(final ActionListener listener) {
    evtSelectedEntitiesChanged.removeListener(listener);
  }

  /**
   * @return a criteria based on this lookup model including any additional lookup criteria
   * @see #setAdditionalLookupCriteria(org.jminor.common.db.criteria.Criteria)
   */
  private EntitySelectCriteria getEntitySelectCriteria() {
    if (searchString.equals(wildcard)) {
      return EntityCriteriaUtil.selectCriteria(entityID);
    }

    final CriteriaSet<Property.ColumnProperty> baseCriteria = new CriteriaSet<Property.ColumnProperty>(CriteriaSet.Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowed ? searchString.split(multipleValueSeparator) : new String[] {searchString};
    for (final Property.ColumnProperty lookupProperty : lookupProperties) {
      for (final String lookupText : lookupTexts) {
        final String modifiedLookupText = (wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : "");
        baseCriteria.add(EntityCriteriaUtil.propertyCriteria(lookupProperty, caseSensitive, SearchType.LIKE, modifiedLookupText));
      }
    }

    return EntityCriteriaUtil.selectCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet<Property.ColumnProperty>(CriteriaSet.Conjunction.AND, additionalLookupCriteria, baseCriteria));
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

