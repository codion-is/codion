/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default EntityLookupModel implementation
 */
public class DefaultEntityLookupModel implements EntityLookupModel {

  private final Event selectedEntitiesChangedEvent = Events.event();
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

  /**
   * Contains the search settings for lookup properties
   */
  private final Map<Property.ColumnProperty, LookupSettings> propertyLookupSettings = new HashMap<>();

  private final Value<String> searchStringValue = Values.value("");
  private final Value<String> multipleItemSeparatorValue = Values.value(",");
  private final Value<Boolean> multipleSelectionAllowedValue = Values.value(true);

  private Entity.ToString toStringProvider = null;
  private Criteria<Property.ColumnProperty> additionalLookupCriteria;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
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
    validateLookupProperties(entityID, lookupProperties);
    this.connectionProvider = connectionProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
    this.description = Util.getCollectionContentsAsString(getLookupProperties(), false);
    initializeDefaultSettings();
    bindEventsInternal();
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
  public void setResultSorter(final Comparator<Entity> resultSorter) {
    Util.rejectNullValue(resultSorter, "resultSorter");
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
    setSelectedEntities(entity != null ? Collections.singletonList(entity) : null);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntities(final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities) && this.selectedEntities.isEmpty()) {
      return;
    }//no change
    if (entities != null && entities.size() > 1 && !multipleSelectionAllowedValue.get()) {
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

  @Override
  public Map<Property.ColumnProperty, LookupSettings> getPropertyLookupSettings() {
    return propertyLookupSettings;
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
  public final EntityLookupModel setAdditionalLookupCriteria(final Criteria<Property.ColumnProperty> additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.ToString getToStringProvider() {
    return toStringProvider;
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel setToStringProvider(final Entity.ToString toStringProvider) {
    this.toStringProvider = toStringProvider;
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
    this.searchStringValue.set(searchString == null ? "" : searchString);
    searchStringRepresentsSelectedState.setActive(searchStringRepresentsSelected());
  }

  /** {@inheritDoc} */
  @Override
  public final String getSearchString() {
    return this.searchStringValue.get();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && Util.nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
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
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Value<String> getSearchStringValue() {
    return searchStringValue;
  }

  /** {@inheritDoc} */
  @Override
  public Value<String> getMultipleItemSeparatorValue() {
    return multipleItemSeparatorValue;
  }

  /** {@inheritDoc} */
  @Override
  public Value<Boolean> getMultipleSelectionAllowedValue() {
    return multipleSelectionAllowedValue;
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectedEntitiesListener(final EventListener listener) {
    selectedEntitiesChangedEvent.addListener(listener);
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
    final CriteriaSet<Property.ColumnProperty> baseCriteria = CriteriaUtil.criteriaSet(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowedValue.get() ? searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (final Property.ColumnProperty lookupProperty : lookupProperties) {
      for (final String rawLookupText : lookupTexts) {
        final boolean wildcardPrefix = propertyLookupSettings.get(lookupProperty).getWildcardPrefixValue().get();
        final boolean wildcardPostfix = propertyLookupSettings.get(lookupProperty).getWildcardPostfixValue().get();
        final boolean caseSensitive = propertyLookupSettings.get(lookupProperty).getCaseSensitiveValue().get();
        final String lookupText = rawLookupText.trim();
        final String modifiedLookupText = searchStringValue.get().equals(wildcard) ? wildcard : ((wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : ""));
        baseCriteria.add(EntityCriteriaUtil.propertyCriteria(lookupProperty, SearchType.LIKE, caseSensitive, modifiedLookupText));
      }
    }

    return EntityCriteriaUtil.selectCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
                    CriteriaUtil.criteriaSet(Conjunction.AND, additionalLookupCriteria, baseCriteria),
            Entities.getOrderByClause(getEntityID()));
  }

  private void initializeDefaultSettings() {
    for (final Property.ColumnProperty property : lookupProperties) {
      propertyLookupSettings.put(property, new DefaultLookupSettings());
    }
  }

  private void bindEventsInternal() {
    searchStringValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        searchStringRepresentsSelectedState.setActive(searchStringRepresentsSelected());
      }
    });
    multipleItemSeparatorValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        refreshSearchText();
      }
    });
  }

  private String toString(final Collection<Entity> entities) {
    final StringBuilder stringBuilder = new StringBuilder();
    int counter = 0;
    for (final Entity entity : entities) {
      if (toStringProvider != null) {
        stringBuilder.append(toStringProvider.toString(entity));
      }
      else {
        stringBuilder.append(entity.toString());
      }
      counter++;
      if (counter < entities.size()) {
        stringBuilder.append(multipleItemSeparatorValue.get());
      }
    }

    return stringBuilder.toString();
  }

  private static void validateLookupProperties(final String entityID, final Collection<Property.ColumnProperty> lookupProperties) {
    if (lookupProperties.isEmpty()) {
      throw new IllegalArgumentException("No lookup properties specified");
    }
    for (final Property.ColumnProperty property : lookupProperties) {
      if (!entityID.equals(property.getEntityID())) {
        throw new IllegalArgumentException("Property '" + property + "' is not part of entity " + entityID);
      }
      if (!property.isString()) {
        throw new IllegalArgumentException("Property '" + property + "' is not a String property");
      }
    }
  }

  private static final class DefaultLookupSettings implements LookupSettings {

    private final Value<Boolean> wildcardPrefixValue = Values.value(true);
    private final Value<Boolean> wildcardPostfixValue = Values.value(true);
    private final Value<Boolean> caseSensitiveValue = Values.value(false);

    @Override
    public Value<Boolean> getWildcardPrefixValue() {
      return wildcardPrefixValue;
    }

    @Override
    public Value<Boolean> getWildcardPostfixValue() {
      return wildcardPostfixValue;
    }

    @Override
    public Value<Boolean> getCaseSensitiveValue() {
      return caseSensitiveValue;
    }
  }

  private static final class EntityComparator implements Comparator<Entity>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final Entity o1, final Entity o2) {
      return o1.compareTo(o2);
    }
  }
}
