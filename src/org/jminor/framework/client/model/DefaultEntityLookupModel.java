/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A default EntityLookupModel implementation
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
   * The EntityConnectionProvider instance used by this EntityLookupModel
   */
  private final EntityConnectionProvider connectionProvider;

  private Criteria additionalLookupCriteria;
  private String searchString = "";
  private boolean multipleSelectionAllowed = true;
  private boolean caseSensitive = false;
  private boolean wildcardPrefix = true;
  private boolean wildcardPostfix = true;
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
  private String multipleValueSeparator = ",";
  private String description;

  /**
   * Instantiates a new EntityLookupModel
   * @param entityID the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @param lookupProperties the properties to search by, these must be string based
   */
  public DefaultEntityLookupModel(final String entityID, final EntityConnectionProvider connectionProvider, final List<Property.ColumnProperty> lookupProperties) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    Util.rejectNullValue(lookupProperties, "lookupProperties");
    this.connectionProvider = connectionProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
    this.description = Util.getCollectionContentsAsString(getLookupProperties(), false);
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  public final List<Property.ColumnProperty> getLookupProperties() {
    return Collections.unmodifiableList(lookupProperties);
  }

  /** {@inheritDoc} */
  public final boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  /** {@inheritDoc} */
  public final void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  /** {@inheritDoc} */
  public final String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  public final void setDescription(final String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  public final void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? Arrays.asList(entity) : null);
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public final List<Entity> getSelectedEntities() {
    return Collections.unmodifiableList(selectedEntities);
  }

  /** {@inheritDoc} */
  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /** {@inheritDoc} */
  public final boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
    return this;
  }

  /** {@inheritDoc} */
  public final boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
    return this;
  }

  /** {@inheritDoc} */
  public final String getWildcard() {
    return wildcard;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  /** {@inheritDoc} */
  public final String getMultipleValueSeparator() {
    return multipleValueSeparator;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator) {
    this.multipleValueSeparator = multipleValueSeparator;
    refreshSearchText();
    return this;
  }

  /** {@inheritDoc} */
  public final EntityLookupModel setAdditionalLookupCriteria(final Criteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
    return this;
  }

  /** {@inheritDoc} */
  public final void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
  }

  /** {@inheritDoc} */
  public final void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    evtSearchStringChanged.fire();
  }

  /** {@inheritDoc} */
  public final String getSearchString() {
    return this.searchString;
  }

  /** {@inheritDoc} */
  public final boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && searchString.isEmpty())
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchString);
  }

  /** {@inheritDoc} */
  public final List<Entity> performQuery() {
    try {
      return connectionProvider.getConnection().selectMany(getEntitySelectCriteria());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public final EventObserver searchStringObserver() {
    return evtSearchStringChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final void addSearchStringListener(final ActionListener listener) {
    evtSearchStringChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void addSelectedEntitiesListener(final ActionListener listener) {
    evtSelectedEntitiesChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSearchStringListener(final ActionListener listener) {
    evtSearchStringChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
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

    final CriteriaSet<Property.ColumnProperty> baseCriteria = new CriteriaSet<Property.ColumnProperty>(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowed ? searchString.split(multipleValueSeparator) : new String[] {searchString};
    for (final Property.ColumnProperty lookupProperty : lookupProperties) {
      for (final String lookupText : lookupTexts) {
        final String modifiedLookupText = (wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : "");
        baseCriteria.add(EntityCriteriaUtil.propertyCriteria(lookupProperty, caseSensitive, SearchType.LIKE, modifiedLookupText));
      }
    }

    return EntityCriteriaUtil.selectCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet<Property.ColumnProperty>(Conjunction.AND, additionalLookupCriteria, baseCriteria));
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

