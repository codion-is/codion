/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.TextUtil;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
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
import java.util.Objects;

/**
 * A default EntityLookupModel implementation
 */
public class DefaultEntityLookupModel implements EntityLookupModel {

  private final Event<Collection<Entity>> selectedEntitiesChangedEvent = Events.event();
  private final State searchStringRepresentsSelectedState = States.state(true);

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityId;

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
   * The conditions instance
   */
  private final EntityConditions entityConditions;

  /**
   * Contains the search settings for lookup properties
   */
  private final Map<Property.ColumnProperty, LookupSettings> propertyLookupSettings = new HashMap<>();

  private final Value<String> searchStringValue = Values.value("");
  private final Value<String> multipleItemSeparatorValue = Values.value(",");
  private final Value<Boolean> multipleSelectionAllowedValue = Values.value(true);

  private Entity.ToString toStringProvider = null;
  private Condition.Provider<Property.ColumnProperty> additionalConditionProvider;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = Property.WILDCARD_CHARACTER.get();
  private String description;

  /**
   * Instantiates a new EntityLookupModel, using the search properties for the given entity type
   * @param entityId the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @see Entities#getSearchProperties(String)
   */
  public DefaultEntityLookupModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, connectionProvider.getEntities().getSearchProperties(entityId));
  }

  /**
   * Instantiates a new EntityLookupModel
   * @param entityId the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @param lookupProperties the properties to search by, these must be string based
   */
  public DefaultEntityLookupModel(final String entityId, final EntityConnectionProvider connectionProvider,
                                  final Collection<Property.ColumnProperty> lookupProperties) {
    Objects.requireNonNull(entityId, "entityId");
    Objects.requireNonNull(connectionProvider, "connectionProvider");
    Objects.requireNonNull(lookupProperties, "lookupProperties");
    validateLookupProperties(entityId, lookupProperties);
    this.connectionProvider = connectionProvider;
    this.entityConditions = connectionProvider.getConditions();
    this.entityId = entityId;
    this.lookupProperties = lookupProperties;
    this.description = TextUtil.getCollectionContentsAsString(getLookupProperties(), false);
    initializeDefaultSettings();
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
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
    Objects.requireNonNull(resultSorter, "resultSorter");
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
    //todo handle non-loaded entities, select from db?
    this.selectedEntities.clear();
    if (entities != null) {
      this.selectedEntities.addAll(entities);
    }
    refreshSearchText();
    selectedEntitiesChangedEvent.fire(Collections.unmodifiableCollection(selectedEntities));
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
  public final EntityLookupModel setAdditionalConditionProvider(final Condition.Provider<Property.ColumnProperty> additionalConditionProvider) {
    this.additionalConditionProvider = additionalConditionProvider;
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
      final List<Entity> result = connectionProvider.getConnection().selectMany(getEntitySelectCondition());
      if (resultSorter != null) {
        result.sort(resultSorter);
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
  public final void addSelectedEntitiesListener(final EventInfoListener<Collection<Entity>> listener) {
    selectedEntitiesChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getSearchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.getObserver();
  }

  /**
   * @return a condition based on this lookup model including any additional lookup condition
   * @throws IllegalStateException in case no lookup properties are specified
   * @see #setAdditionalLookupCondition(Condition)
   */
  private EntitySelectCondition getEntitySelectCondition() {
    if (lookupProperties.isEmpty()) {
      throw new IllegalStateException("No lookup properties provided for lookup model: " + entityId);
    }
    final Condition.Set<Property.ColumnProperty> baseCondition = Conditions.conditionSet(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowedValue.get() ? searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (final Property.ColumnProperty lookupProperty : lookupProperties) {
      for (final String rawLookupText : lookupTexts) {
        final boolean wildcardPrefix = propertyLookupSettings.get(lookupProperty).getWildcardPrefixValue().get();
        final boolean wildcardPostfix = propertyLookupSettings.get(lookupProperty).getWildcardPostfixValue().get();
        final boolean caseSensitive = propertyLookupSettings.get(lookupProperty).getCaseSensitiveValue().get();
        final String lookupText = rawLookupText.trim();
        final String modifiedLookupText = searchStringValue.get().equals(wildcard) ? wildcard : ((wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : ""));
        baseCondition.add(entityConditions.propertyCondition(lookupProperty, Condition.Type.LIKE, caseSensitive, modifiedLookupText));
      }
    }

    return entityConditions.selectCondition(entityId, additionalConditionProvider == null ? baseCondition :
                    Conditions.conditionSet(Conjunction.AND, additionalConditionProvider.getCondition(), baseCondition))
            .setOrderBy(connectionProvider.getEntities().getOrderBy(entityId));
  }

  private void initializeDefaultSettings() {
    for (final Property.ColumnProperty property : lookupProperties) {
      propertyLookupSettings.put(property, new DefaultLookupSettings());
    }
  }

  private void bindEventsInternal() {
    searchStringValue.getObserver().addListener(() ->
            searchStringRepresentsSelectedState.setActive(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.getObserver().addListener(this::refreshSearchText);
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

  private static void validateLookupProperties(final String entityId, final Collection<Property.ColumnProperty> lookupProperties) {
    for (final Property.ColumnProperty property : lookupProperties) {
      if (!entityId.equals(property.getEntityId())) {
        throw new IllegalArgumentException("Property '" + property + "' is not part of entity " + entityId);
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
