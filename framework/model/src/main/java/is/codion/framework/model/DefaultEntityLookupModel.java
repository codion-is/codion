/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.state.States;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.framework.db.condition.Conditions.combination;
import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A default EntityLookupModel implementation
 */
public final class DefaultEntityLookupModel implements EntityLookupModel {

  private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;

  private final Event<List<Entity>> selectedEntitiesChangedEvent = Events.event();
  private final State searchStringRepresentsSelectedState = States.state(true);

  /**
   * The type of the entity this lookup model is based on
   */
  private final EntityType<?> entityType;

  /**
   * The attributes to use when doing the lookup
   */
  private final Collection<Attribute<String>> lookupAttributes;

  /**
   * The selected entities
   */
  private final List<Entity> selectedEntities = new ArrayList<>();

  /**
   * The EntityConnectionProvider instance used by this EntityLookupModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Contains the search settings for lookup properties
   */
  private final Map<Attribute<String>, LookupSettings> attributeLookupSettings = new HashMap<>();

  private final Value<String> searchStringValue = Values.value("");
  private final Value<String> multipleItemSeparatorValue = Values.value(",");
  private final Value<Boolean> multipleSelectionEnabledValue = Values.value(true, false);

  private Function<Entity, String> toStringProvider = DEFAULT_TO_STRING;
  private Condition.Provider additionalConditionProvider;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = Property.WILDCARD_CHARACTER.get();
  private String description;

  /**
   * Instantiates a new EntityLookupModel, using the search properties for the given entity type
   * @param entityType the type of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @see EntityDefinition#getSearchAttributes()
   */
  public DefaultEntityLookupModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getSearchAttributes());
  }

  /**
   * Instantiates a new EntityLookupModel
   * @param entityType the type of the entity to lookup
   * @param connectionProvider the EntityConnectionProvider to use when performing the lookup
   * @param lookupAttributes the attributes to search by
   */
  public DefaultEntityLookupModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                  final Collection<Attribute<String>> lookupAttributes) {
    requireNonNull(entityType, "entityType");
    requireNonNull(connectionProvider, "connectionProvider");
    requireNonNull(lookupAttributes, "lookupAttributes");
    validateLookupProperties(entityType, lookupAttributes);
    this.connectionProvider = connectionProvider;
    this.entityType = entityType;
    this.lookupAttributes = lookupAttributes;
    this.description = lookupAttributes.stream().map(Objects::toString).collect(joining(", "));
    initializeDefaultSettings();
    bindEventsInternal();
  }

  @Override
  public EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public Collection<Attribute<String>> getLookupAttributes() {
    return unmodifiableCollection(lookupAttributes);
  }

  @Override
  public void setResultSorter(final Comparator<Entity> resultSorter) {
    requireNonNull(resultSorter, "resultSorter");
    this.resultSorter = resultSorter;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(entity != null ? singletonList(entity) : null);
  }

  @Override
  public void setSelectedEntities(final List<Entity> entities) {
    if (nullOrEmpty(entities) && this.selectedEntities.isEmpty()) {
      return;
    }//no change
    if (entities != null && entities.size() > 1 && !multipleSelectionEnabledValue.get()) {
      throw new IllegalArgumentException("This EntityLookupModel does not allow the selection of multiple entities");
    }
    //todo handle non-loaded entities, select from db?
    this.selectedEntities.clear();
    if (entities != null) {
      this.selectedEntities.addAll(entities);
    }
    refreshSearchText();
    selectedEntitiesChangedEvent.onEvent(unmodifiableList(selectedEntities));
  }

  @Override
  public List<Entity> getSelectedEntities() {
    return unmodifiableList(selectedEntities);
  }

  @Override
  public Map<Attribute<String>, LookupSettings> getAttributeLookupSettings() {
    return attributeLookupSettings;
  }

  @Override
  public String getWildcard() {
    return wildcard;
  }

  @Override
  public void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  @Override
  public void setAdditionalConditionProvider(final Condition.Provider additionalConditionProvider) {
    this.additionalConditionProvider = additionalConditionProvider;
  }

  @Override
  public Function<Entity, String> getToStringProvider() {
    return toStringProvider;
  }

  @Override
  public void setToStringProvider(final Function<Entity, String> toStringProvider) {
    this.toStringProvider = toStringProvider == null ? DEFAULT_TO_STRING : toStringProvider;
  }

  @Override
  public void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public void setSearchString(final String searchString) {
    this.searchStringValue.set(searchString == null ? "" : searchString);
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public String getSearchString() {
    return this.searchStringValue.get();
  }

  @Override
  public boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
  }

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

  @Override
  public Value<String> getSearchStringValue() {
    return searchStringValue;
  }

  @Override
  public Value<String> getMultipleItemSeparatorValue() {
    return multipleItemSeparatorValue;
  }

  @Override
  public Value<Boolean> getMultipleSelectionEnabledValue() {
    return multipleSelectionEnabledValue;
  }

  @Override
  public void addSelectedEntitiesListener(final EventDataListener<List<Entity>> listener) {
    selectedEntitiesChangedEvent.addDataListener(listener);
  }

  @Override
  public StateObserver getSearchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.getObserver();
  }

  /**
   * @return a condition based on this lookup model including any additional lookup condition
   * @throws IllegalStateException in case no lookup properties are specified
   * @see #setAdditionalConditionProvider(Condition.Provider)
   */
  private SelectCondition getEntitySelectCondition() {
    if (lookupAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes provided for lookup model: " + entityType);
    }
    final Condition.Combination conditionCombination = combination(Conjunction.OR);
    final String[] lookupTexts = multipleSelectionEnabledValue.get() ?
            searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (final Attribute<String> lookupAttribute : lookupAttributes) {
      final LookupSettings lookupSettings = attributeLookupSettings.get(lookupAttribute);
      for (final String rawLookupText : lookupTexts) {
        final String lookupText = prepareLookupText(rawLookupText, lookupSettings);
        final AttributeCondition<String> condition = condition(lookupAttribute)
                .equalTo(lookupText).setCaseSensitive(lookupSettings.getCaseSensitiveValue().get());
        conditionCombination.add(condition);
      }
    }

    return (additionalConditionProvider == null ? conditionCombination :
            additionalConditionProvider.getCondition().and(conditionCombination))
            .select().orderBy(connectionProvider.getEntities().getDefinition(entityType).getOrderBy());
  }

  private String prepareLookupText(final String rawLookupText, final LookupSettings lookupSettings) {
    final boolean wildcardPrefix = lookupSettings.getWildcardPrefixValue().get();
    final boolean wildcardPostfix = lookupSettings.getWildcardPostfixValue().get();

    return rawLookupText.equals(wildcard) ? wildcard :
            ((wildcardPrefix ? wildcard : "") + rawLookupText.trim() + (wildcardPostfix ? wildcard : ""));
  }

  private void initializeDefaultSettings() {
    for (final Attribute<String> attribute : lookupAttributes) {
      attributeLookupSettings.put(attribute, new DefaultLookupSettings());
    }
  }

  private void bindEventsInternal() {
    searchStringValue.addListener(() ->
            searchStringRepresentsSelectedState.set(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.addListener(this::refreshSearchText);
  }

  private String toString(final List<Entity> entities) {
    return entities.stream().map(toStringProvider).collect(joining(multipleItemSeparatorValue.get()));
  }

  private static void validateLookupProperties(final EntityType<?> entityType, final Collection<Attribute<String>> lookupAttributes) {
    for (final Attribute<String> attribute : lookupAttributes) {
      if (!entityType.equals(attribute.getEntityType())) {
        throw new IllegalArgumentException("Attribute '" + attribute + "' is not part of entity " + entityType);
      }
    }
  }

  private static final class DefaultLookupSettings implements LookupSettings {

    private final Value<Boolean> wildcardPrefixValue = Values.value(true, false);
    private final Value<Boolean> wildcardPostfixValue = Values.value(true, false);
    private final Value<Boolean> caseSensitiveValue = Values.value(false, false);

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
