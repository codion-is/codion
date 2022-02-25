/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
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
  private final Value<Boolean> multipleSelectionEnabledValue = Value.value(true, false);

  private Function<Entity, String> toStringProvider = DEFAULT_TO_STRING;
  private Supplier<Condition> additionalConditionSupplier;
  private Comparator<Entity> resultSorter = new EntityComparator();
  private String wildcard = Property.WILDCARD_CHARACTER.get();
  private String description;

  /**
   * Instantiates a new DefaultEntitySearchModel, using the search properties for the given entity type
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @see EntityDefinition#getSearchAttributes()
   */
  public DefaultEntitySearchModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getSearchAttributes());
  }

  /**
   * Instantiates a new EntitySearchModel
   * @param entityType the type of the entity to search
   * @param connectionProvider the EntityConnectionProvider to use when performing the search
   * @param searchAttributes the attributes to search by
   */
  public DefaultEntitySearchModel(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                  final Collection<Attribute<String>> searchAttributes) {
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
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public Collection<Attribute<String>> getSearchAttributes() {
    return unmodifiableCollection(searchAttributes);
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
    if (entities != null) {
      if (entities.size() > 1 && !multipleSelectionEnabledValue.get()) {
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
  public Map<Attribute<String>, SearchSettings> getAttributeSearchSettings() {
    return attributeSearchSettings;
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
  public void setAdditionalConditionSupplier(final Supplier<Condition> additionalConditionSupplier) {
    this.additionalConditionSupplier = additionalConditionSupplier;
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
    String selectedAsString = toString(getSelectedEntities());
    return (selectedEntities.isEmpty() && nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
  }

  @Override
  public List<Entity> performQuery() {
    try {
      List<Entity> result = connectionProvider.getConnection().select(getEntitySelectCondition());
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
   * @return a condition based on this search model including any additional search condition
   * @throws IllegalStateException in case no search properties are specified
   * @see #setAdditionalConditionProvider(Condition.Provider)
   */
  private SelectCondition getEntitySelectCondition() {
    if (searchAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes provided for search model: " + entityType);
    }
    Collection<Condition> conditions = new ArrayList<>();
    String[] searchTexts = multipleSelectionEnabledValue.get() ?
            searchStringValue.get().split(multipleItemSeparatorValue.get()) : new String[] {searchStringValue.get()};
    for (final Attribute<String> searchAttribute : searchAttributes) {
      SearchSettings searchSettings = attributeSearchSettings.get(searchAttribute);
      for (final String rawSearchText : searchTexts) {
        conditions.add(where(searchAttribute)
                .equalTo(prepareSearchText(rawSearchText, searchSettings))
                .caseSensitive(searchSettings.getCaseSensitiveValue().get()));
      }
    }
    Condition.Combination conditionCombination = combination(Conjunction.OR, conditions);

    return (additionalConditionSupplier == null ? conditionCombination :
            additionalConditionSupplier.get().and(conditionCombination))
            .toSelectCondition().orderBy(connectionProvider.getEntities().getDefinition(entityType).getOrderBy());
  }

  private String prepareSearchText(final String rawSearchText, final SearchSettings searchSettings) {
    boolean wildcardPrefix = searchSettings.getWildcardPrefixValue().get();
    boolean wildcardPostfix = searchSettings.getWildcardPostfixValue().get();

    return rawSearchText.equals(wildcard) ? wildcard :
            ((wildcardPrefix ? wildcard : "") + rawSearchText.trim() + (wildcardPostfix ? wildcard : ""));
  }

  private void bindEventsInternal() {
    searchStringValue.addListener(() ->
            searchStringRepresentsSelectedState.set(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.addListener(this::refreshSearchText);
  }

  private String createDescription() {
    EntityDefinition definition = connectionProvider.getEntities().getDefinition(entityType);

    return searchAttributes.stream()
            .map(attribute -> definition.getProperty(attribute).getCaption())
            .collect(joining(", "));
  }

  private String toString(final List<Entity> entities) {
    return entities.stream()
            .map(toStringProvider)
            .collect(joining(multipleItemSeparatorValue.get()));
  }

  private void validateType(final Entity entity) {
    if (!entity.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Entities of type " + entityType + " exptected, got " + entity.getEntityType());
    }
  }

  private static void validateSearchAttributes(final EntityType entityType, final Collection<Attribute<String>> searchAttributes) {
    for (final Attribute<String> attribute : searchAttributes) {
      if (!entityType.equals(attribute.getEntityType())) {
        throw new IllegalArgumentException("Attribute '" + attribute + "' is not part of entity " + entityType);
      }
    }
  }

  private static final class DefaultSearchSettings implements SearchSettings {

    private final Value<Boolean> wildcardPrefixValue = Value.value(true, false);
    private final Value<Boolean> wildcardPostfixValue = Value.value(true, false);
    private final Value<Boolean> caseSensitiveValue = Value.value(false, false);

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
