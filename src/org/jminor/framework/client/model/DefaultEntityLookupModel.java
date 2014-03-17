/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A default EntityLookupModel implementation
 */
public class DefaultEntityLookupModel implements EntityLookupModel {

  private final Event selectedEntitiesChangedEvent = Events.event();
  private final Event<String> searchStringChangedEvent = Events.event();
  private final State searchStringRepresentsSelectedState = States.state(true);

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityID;

  /**
   * The properties to use when doing the lookup
   */
  private final Collection<Property.ColumnProperty> lookupProperties;

  /**
   * The selected entities
   */
  private final Collection<Entity> selectedEntities = new ArrayList<>();

  /**
   * The EntityConnectionProvider instance used by this EntityLookupModel
   */
  private final EntityConnectionProvider connectionProvider;

  private Criteria<Property.ColumnProperty> additionalLookupCriteria;
  private String searchString = "";
  private boolean multipleSelectionAllowed = true;
  private boolean caseSensitive = false;
  private boolean wildcardPrefix = true;
  private boolean wildcardPostfix = true;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
  private String multipleValueSeparator = ",";
  private String description;

  /**
   * Instantiates a new EntityLookupModel
   * @param entityID the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @param lookupProperties the properties to search by, these must be string based
   */
  public DefaultEntityLookupModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                  final Collection<Property.ColumnProperty> lookupProperties) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    Util.rejectNullValue(lookupProperties, "lookupProperties");
    this.connectionProvider = connectionProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
    this.description = Util.getCollectionContentsAsString(getLookupProperties(), false);
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Property.ColumnProperty> getLookupProperties() {
    return Collections.unmodifiableCollection(lookupProperties);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public void setResultSorter(final Comparator<Entity> resultSorter) {
    this.resultSorter = resultSorter;
  }

  /** {@inheritDoc} */
  @Override
  public final String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  @Override
  public final void setDescription(final String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? Arrays.asList(entity) : null);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntities(final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities) && this.selectedEntities.isEmpty()) {
      return;
    }//no change
    if (entities != null && entities.size() > 1 && !multipleSelectionAllowed) {
      throw new IllegalArgumentException("This EntityLookupModel does not allow the selection of multiple entities");
    }
//todo handle non-loaded entities, see if combo box behaves normally
    this.selectedEntities.clear();
    if (entities != null) {
      this.selectedEntities.addAll(entities);
    }
    refreshSearchText();
    selectedEntitiesChangedEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getSelectedEntities() {
    return Collections.unmodifiableCollection(selectedEntities);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getWildcard() {
    return wildcard;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getMultipleValueSeparator() {
    return multipleValueSeparator;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setMultipleValueSeparator(final String multipleValueSeparator) {
    this.multipleValueSeparator = multipleValueSeparator;
    refreshSearchText();
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel setAdditionalLookupCriteria(final Criteria<Property.ColumnProperty> additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
    searchStringRepresentsSelectedState.setActive(searchStringRepresentsSelected());
  }

  /** {@inheritDoc} */
  @Override
  public final void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    searchStringRepresentsSelectedState.setActive(searchStringRepresentsSelected());
    searchStringChangedEvent.fire(this.searchString);
  }

  /** {@inheritDoc} */
  @Override
  public final String getSearchString() {
    return this.searchString;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && searchString.length() == 0)
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchString);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> performQuery() {
    try {
      final List<Entity> result = connectionProvider.getConnection().selectMany(getEntitySelectCriteria());
      if (resultSorter != null) {
        Collections.sort(result, resultSorter);
      }

      return result;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<String> getSearchStringObserver() {
    return searchStringChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addSearchStringListener(final EventInfoListener<String> listener) {
    searchStringChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectedEntitiesListener(final EventListener listener) {
    selectedEntitiesChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSearchStringListener(final EventListener listener) {
    searchStringChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSelectedEntitiesListener(final EventListener listener) {
    selectedEntitiesChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getSearchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.getObserver();
  }

  /**
   * @return a criteria based on this lookup model including any additional lookup criteria
   * @see #setAdditionalLookupCriteria(org.jminor.common.db.criteria.Criteria)
   */
  private EntitySelectCriteria getEntitySelectCriteria() {
    final CriteriaSet<Property.ColumnProperty> baseCriteria = new CriteriaSet<>(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowed ? searchString.split(multipleValueSeparator) : new String[] {searchString};
    for (final Property.ColumnProperty lookupProperty : lookupProperties) {
      for (final String rawLookupText : lookupTexts) {
        final String lookupText = rawLookupText.trim();
        final String modifiedLookupText = searchString.equals(wildcard) ? wildcard : ((wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : ""));
        baseCriteria.add(EntityCriteriaUtil.propertyCriteria(lookupProperty, caseSensitive, SearchType.LIKE, modifiedLookupText));
      }
    }

    return EntityCriteriaUtil.selectCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet<>(Conjunction.AND, additionalLookupCriteria, baseCriteria),
            Entities.getOrderByClause(getEntityID()));
  }

  private String toString(final Collection<Entity> entities) {
    final StringBuilder stringBuilder = new StringBuilder();
    int counter = 0;
    for (final Entity entity : entities) {
      stringBuilder.append(entity.toString());
      counter++;
      if (counter < entities.size()) {
        stringBuilder.append(multipleValueSeparator);
      }
    }

    return stringBuilder.toString();
  }

  private static final class EntityComparator implements Comparator<Entity>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final Entity o1, final Entity o2) {
      return o1.compareTo(o2);
    }
  }
}
