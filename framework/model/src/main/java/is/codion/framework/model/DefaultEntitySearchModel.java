/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.framework.db.condition.Conditions.combination;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A default EntitySearchModel implementation
 */
public final class DefaultEntitySearchModel implements EntitySearchModel {

  private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;
  private static final String DEFAULT_SEPARATOR = ",";

  private final Event<List<Entity>> selectedEntitiesChangedEvent = Event.event();
  private final State searchStringRepresentsSelectedState = State.state(true);

  /**
   * The type of the entity this search model is based on
   */
  private final EntityType entityType;

  /**
   * The attributes to use when doing the search
   */
  private final Collection<Attribute<String>> searchAttributes;

  /**
   * The selected entities
   */
  private final List<Entity> selectedEntities = new ArrayList<>();

  /**
   * The EntityConnectionProvider instance used by this EntitySearchModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Contains the search settings for search properties
   */
  private final Map<Attribute<String>, SearchSettings> attributeSearchSettings = new HashMap<>();

  private final Value<String> searchStringValue = Value.value("");
  private final Value<String> multipleItemSeparatorValue = Value.value(DEFAULT_SEPARATOR, DEFAULT_SEPARATOR);
  private final State multipleSelectionEnabledState = State.state(true);
  private final Value<Character> wildcardValue = Value.value(Text.WILDCARD_CHARACTER.get(), Text.WILDCARD_CHARACTER.get());

  private Function<Entity, String> toStringProvider = DEFAULT_TO_STRING;
  private Supplier<Condition> additionalConditionSupplier;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String description;

  /**
   * Instantiates a new DefaultEntitySearchModel, using the search properties for the given entity type
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @see EntityDefinition#searchAttributes()
   */
  public DefaultEntitySearchModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.entities().getDefinition(entityType).searchAttributes());
  }

  /**
   * Instantiates a new EntitySearchModel
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @param searchAttributes the attributes to search by
   */
  public DefaultEntitySearchModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                  Collection<Attribute<String>> searchAttributes) {
    requireNonNull(entityType, "entityType");
    requireNonNull(connectionProvider, "connectionProvider");
    requireNonNull(searchAttributes, "searchAttributes");
    validateSearchAttributes(entityType, searchAttributes);
    this.connectionProvider = connectionProvider;
    this.entityType = entityType;
    this.searchAttributes = searchAttributes;
    this.description = createDescription();
    this.searchAttributes.forEach(attribute -> attributeSearchSettings.put(attribute, new DefaultSearchSettings()));
    bindEventsInternal();
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public Collection<Attribute<String>> searchAttributes() {
    return unmodifiableCollection(searchAttributes);
  }

  @Override
  public void setResultSorter(Comparator<Entity> resultSorter) {
    requireNonNull(resultSorter, "resultSorter");
    this.resultSorter = resultSorter;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public void setSelectedEntity(Entity entity) {
    setSelectedEntities(entity != null ? singletonList(entity) : null);
  }

  @Override
  public void setSelectedEntities(List<Entity> entities) {
    if (nullOrEmpty(entities) && this.selectedEntities.isEmpty()) {
      return;
    }//no change
    if (entities != null) {
      if (entities.size() > 1 && !multipleSelectionEnabledState.get()) {
        throw new IllegalArgumentException("This EntitySearchModel does not allow the selection of multiple entities");
      }
      entities.forEach(this::validateType);
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
  public Map<Attribute<String>, SearchSettings> attributeSearchSettings() {
    return attributeSearchSettings;
  }

  @Override
  public Value<Character> wildcardValue() {
    return wildcardValue;
  }

  @Override
  public void setAdditionalConditionSupplier(Supplier<Condition> additionalConditionSupplier) {
    this.additionalConditionSupplier = additionalConditionSupplier;
  }

  @Override
  public Function<Entity, String> getToStringProvider() {
    return toStringProvider;
  }

  @Override
  public void setToStringProvider(Function<Entity, String> toStringProvider) {
    this.toStringProvider = toStringProvider == null ? DEFAULT_TO_STRING : toStringProvider;
  }

  @Override
  public void refreshSearchText() {
    setSearchString(selectedEntities.isEmpty() ? "" : toString(getSelectedEntities()));
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public void setSearchString(String searchString) {
    this.searchStringValue.set(searchString == null ? "" : searchString);
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public String getSearchString() {
    return this.searchStringValue.get();
  }

  @Override
  public boolean searchStringRepresentsSelected() {
    String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
  }

  @Override
  public List<Entity> performQuery() {
    try {
      List<Entity> result = connectionProvider.connection().select(getEntitySelectCondition());
      if (resultSorter != null) {
        result.sort(resultSorter);
      }

      return result;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Value<String> searchStringValue() {
    return searchStringValue;
  }

  @Override
  public Value<String> multipleItemSeparatorValue() {
    return multipleItemSeparatorValue;
  }

  @Override
  public State multipleSelectionEnabledState() {
    return multipleSelectionEnabledState;
  }

  @Override
  public void addSelectedEntitiesListener(EventDataListener<List<Entity>> listener) {
    selectedEntitiesChangedEvent.addDataListener(listener);
  }

  @Override
  public StateObserver searchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.observer();
  }

  /**
   * @return a condition based on this search model including any additional search condition
   * @throws IllegalStateException in case no search properties are specified
   * @see #setAdditionalConditionProvider(Condition.Provider)
   */
  private SelectCondition getEntitySelectCondition() {
    if (searchAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes provided for search model: " + entityType);
    }
    Collection<Condition> conditions = new ArrayList<>();
    String[] searchTexts = multipleSelectionEnabledState.get() ?
            searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (Attribute<String> searchAttribute : searchAttributes) {
      SearchSettings searchSettings = attributeSearchSettings.get(searchAttribute);
      for (String rawSearchText : searchTexts) {
        AttributeCondition.Builder<String> builder = where(searchAttribute);
        if (searchSettings.caseSensitiveState().get()) {
          conditions.add(builder.equalTo(prepareSearchText(rawSearchText, searchSettings)));
        }
        else {
          conditions.add(builder.equalToIgnoreCase(prepareSearchText(rawSearchText, searchSettings)));
        }
      }
    }
    Condition.Combination conditionCombination = combination(Conjunction.OR, conditions);

    return (additionalConditionSupplier == null ? conditionCombination :
            additionalConditionSupplier.get().and(conditionCombination))
            .selectBuilder()
            .orderBy(connectionProvider.entities().getDefinition(entityType).orderBy())
            .build();
  }

  private String prepareSearchText(String rawSearchText, SearchSettings searchSettings) {
    boolean wildcardPrefix = searchSettings.wildcardPrefixState().get();
    boolean wildcardPostfix = searchSettings.wildcardPostfixState().get();

    return rawSearchText.equals(String.valueOf(wildcardValue.get())) ? String.valueOf(wildcardValue.get()) :
            ((wildcardPrefix ? wildcardValue.get() : "") + rawSearchText.trim() + (wildcardPostfix ? wildcardValue.get() : ""));
  }

  private void bindEventsInternal() {
    searchStringValue.addListener(() ->
            searchStringRepresentsSelectedState.set(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.addListener(this::refreshSearchText);
  }

  private String createDescription() {
    EntityDefinition definition = connectionProvider.entities().getDefinition(entityType);

    return searchAttributes.stream()
            .map(attribute -> definition.property(attribute).caption())
            .collect(joining(", "));
  }

  private String toString(List<Entity> entities) {
    return entities.stream()
            .map(toStringProvider)
            .collect(joining(multipleItemSeparatorValue.get()));
  }

  private void validateType(Entity entity) {
    if (!entity.entityType().equals(entityType)) {
      throw new IllegalArgumentException("Entities of type " + entityType + " exptected, got " + entity.entityType());
    }
  }

  private static void validateSearchAttributes(EntityType entityType, Collection<Attribute<String>> searchAttributes) {
    for (Attribute<String> attribute : searchAttributes) {
      if (!entityType.equals(attribute.entityType())) {
        throw new IllegalArgumentException("Attribute '" + attribute + "' is not part of entity " + entityType);
      }
    }
  }

  private static final class DefaultSearchSettings implements SearchSettings {

    private final State wildcardPrefixState = State.state(true);
    private final State wildcardPostfixState = State.state(true);
    private final State caseSensitiveState = State.state(false);

    @Override
    public State wildcardPrefixState() {
      return wildcardPrefixState;
    }

    @Override
    public State wildcardPostfixState() {
      return wildcardPostfixState;
    }

    @Override
    public State caseSensitiveState() {
      return caseSensitiveState;
    }
  }

  private static final class EntityComparator implements Comparator<Entity>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(Entity o1, Entity o2) {
      return o1.compareTo(o2);
    }
  }
}
