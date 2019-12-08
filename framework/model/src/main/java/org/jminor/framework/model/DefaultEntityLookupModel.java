/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.TextUtil;
import org.jminor.common.Util;
import org.jminor.common.db.ConditionType;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.framework.db.condition.Conditions.conditionSet;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

/**
 * A default EntityLookupModel implementation
 */
public final class DefaultEntityLookupModel implements EntityLookupModel {

  private static final Entity.ToString DEFAULT_TO_STRING = Object::toString;

  private final Event<Collection<Entity>> selectedEntitiesChangedEvent = Events.event();
  private final State searchStringRepresentsSelectedState = States.state(true);

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityId;

  /**
   * The properties to use when doing the lookup
   */
  private final Collection<ColumnProperty> lookupProperties;

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
  private final Map<ColumnProperty, LookupSettings> propertyLookupSettings = new HashMap<>();

  private final Value<String> searchStringValue = Values.value("");
  private final Value<String> multipleItemSeparatorValue = Values.value(",");
  private final Value<Boolean> multipleSelectionAllowedValue = Values.value(true);

  private Entity.ToString toStringProvider = DEFAULT_TO_STRING;
  private Condition.Provider additionalConditionProvider;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = Property.WILDCARD_CHARACTER.get();
  private String description;

  /**
   * Instantiates a new EntityLookupModel, using the search properties for the given entity type
   * @param entityId the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @see EntityDefinition#getSearchProperties()
   */
  public DefaultEntityLookupModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, connectionProvider.getDomain().getDefinition(entityId).getSearchProperties());
  }

  /**
   * Instantiates a new EntityLookupModel
   * @param entityId the ID of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @param lookupProperties the properties to search by, these must be string based
   */
  public DefaultEntityLookupModel(final String entityId, final EntityConnectionProvider connectionProvider,
                                  final Collection<ColumnProperty> lookupProperties) {
    requireNonNull(entityId, "entityId");
    requireNonNull(connectionProvider, "connectionProvider");
    requireNonNull(lookupProperties, "lookupProperties");
    validateLookupProperties(entityId, lookupProperties);
    this.connectionProvider = connectionProvider;
    this.entityId = entityId;
    this.lookupProperties = lookupProperties;
    this.description = TextUtil.getCollectionContentsAsString(getLookupProperties(), false);
    initializeDefaultSettings();
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ColumnProperty> getLookupProperties() {
    return unmodifiableCollection(lookupProperties);
  }

  /** {@inheritDoc} */
  @Override
  public void setResultSorter(final Comparator<Entity> resultSorter) {
    requireNonNull(resultSorter, "resultSorter");
    this.resultSorter = resultSorter;
  }

  /** {@inheritDoc} */
  @Override
  public String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  @Override
  public void setDescription(final String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? singletonList(entity) : null);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedEntities(final Collection<Entity> entities) {
    if (nullOrEmpty(entities) && this.selectedEntities.isEmpty()) {
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
    selectedEntitiesChangedEvent.fire(unmodifiableCollection(selectedEntities));
  }

  /** {@inheritDoc} */
  @Override
  public Collection<Entity> getSelectedEntities() {
    return unmodifiableCollection(selectedEntities);
  }

  @Override
  public Map<ColumnProperty, LookupSettings> getPropertyLookupSettings() {
    return propertyLookupSettings;
  }

  /** {@inheritDoc} */
  @Override
  public String getWildcard() {
    return wildcard;
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel setWildcard(final String wildcard) {
    this.wildcard = wildcard;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel setAdditionalConditionProvider(final Condition.Provider additionalConditionProvider) {
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
    this.toStringProvider = toStringProvider == null ? DEFAULT_TO_STRING : toStringProvider;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  /** {@inheritDoc} */
  @Override
  public void setSearchString(final String searchString) {
    this.searchStringValue.set(searchString == null ? "" : searchString);
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  /** {@inheritDoc} */
  @Override
  public String getSearchString() {
    return this.searchStringValue.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> performQuery() {
    try {
      final List<Entity> result = connectionProvider.getConnection().select(getEntitySelectCondition());
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
  public void addSelectedEntitiesListener(final EventDataListener<Collection<Entity>> listener) {
    selectedEntitiesChangedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSearchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.getObserver();
  }

  /**
   * @return a condition based on this lookup model including any additional lookup condition
   * @throws IllegalStateException in case no lookup properties are specified
   * @see #setAdditionalConditionProvider(Condition.Provider)
   */
  private EntitySelectCondition getEntitySelectCondition() {
    if (lookupProperties.isEmpty()) {
      throw new IllegalStateException("No search properties provided for lookup model: " + entityId);
    }
    final Condition.Set baseCondition = conditionSet(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionAllowedValue.get() ? searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (final ColumnProperty lookupProperty : lookupProperties) {
      for (final String rawLookupText : lookupTexts) {
        final boolean wildcardPrefix = propertyLookupSettings.get(lookupProperty).getWildcardPrefixValue().get();
        final boolean wildcardPostfix = propertyLookupSettings.get(lookupProperty).getWildcardPostfixValue().get();
        final boolean caseSensitive = propertyLookupSettings.get(lookupProperty).getCaseSensitiveValue().get();
        final String lookupText = rawLookupText.trim();
        final String modifiedLookupText = searchStringValue.get().equals(wildcard) ? wildcard : ((wildcardPrefix ? wildcard : "") + lookupText + (wildcardPostfix ? wildcard : ""));
        final PropertyCondition condition = Conditions.propertyCondition(lookupProperty.getPropertyId(),
                ConditionType.LIKE, modifiedLookupText).setCaseSensitive(caseSensitive);
        baseCondition.add(condition);
      }
    }

    return entitySelectCondition(entityId, additionalConditionProvider == null ? baseCondition :
            conditionSet(Conjunction.AND, additionalConditionProvider.getCondition(), baseCondition))
            .setOrderBy(connectionProvider.getDomain().getDefinition(entityId).getOrderBy());
  }

  private void initializeDefaultSettings() {
    for (final ColumnProperty property : lookupProperties) {
      propertyLookupSettings.put(property, new DefaultLookupSettings());
    }
  }

  private void bindEventsInternal() {
    searchStringValue.addListener(() ->
            searchStringRepresentsSelectedState.set(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.addListener(this::refreshSearchText);
  }

  private String toString(final Collection<Entity> entities) {
    return Util.join(multipleItemSeparatorValue.get(), entities, entity -> toStringProvider.toString(entity));
  }

  private static void validateLookupProperties(final String entityId, final Collection<ColumnProperty> lookupProperties) {
    for (final ColumnProperty property : lookupProperties) {
      if (!entityId.equals(property.getEntityId())) {
        throw new IllegalArgumentException("Property '" + property + "' is not part of entity " + entityId);
      }
      if (!property.isString()) {
        throw new IllegalArgumentException("Property '" + property + "' is not a String based property");
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
