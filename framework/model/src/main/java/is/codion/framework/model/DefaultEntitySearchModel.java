/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.ColumnCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.db.condition.Condition.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultEntitySearchModel implements EntitySearchModel {

  private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;
  private static final String DEFAULT_SEPARATOR = ",";

  private final Event<List<Entity>> selectedEntitiesChangedEvent = Event.event();
  private final State searchStringRepresentsSelectedState = State.state(true);

  /**
   * The type of the entity this search model is based on
   */
  private final EntityType entityType;

  /**
   * The columns to use when doing the search
   */
  private final Collection<Column<String>> searchColumns;

  /**
   * The selected entities
   */
  private final List<Entity> selectedEntities = new ArrayList<>();

  /**
   * The EntityConnectionProvider instance used by this EntitySearchModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Contains the search settings for search columns
   */
  private final Map<Column<String>, SearchSettings> columnSearchSettings = new HashMap<>();

  private final Value<String> searchStringValue = Value.value("");
  private final Value<String> multipleItemSeparatorValue;
  private final State singleSelectionState = State.state(false);
  private final Value<Character> wildcardValue = Value.value(Text.WILDCARD_CHARACTER.get(), Text.WILDCARD_CHARACTER.get());
  private final State selectionEmptyState = State.state(true);

  private Function<Entity, String> toStringProvider;
  private Comparator<Entity> resultSorter;
  private String description;
  private Supplier<Condition> additionalConditionSupplier;

  private DefaultEntitySearchModel(DefaultBuilder builder) {
    this.entityType = builder.entityType;
    this.connectionProvider = builder.connectionProvider;
    this.multipleItemSeparatorValue = Value.value(builder.multipleItemSeparator, DEFAULT_SEPARATOR);
    this.searchColumns = unmodifiableCollection(builder.searchColumns);
    this.searchColumns.forEach(attribute -> columnSearchSettings.put(attribute, new DefaultSearchSettings()));
    this.toStringProvider = builder.toStringProvider;
    this.resultSorter = builder.resultSorter;
    this.description = builder.description == null ? createDescription() : builder.description;
    this.singleSelectionState.set(builder.singleSelection);
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
  public Collection<Column<String>> searchColumns() {
    return searchColumns;
  }

  @Override
  public void setResultSorter(Comparator<Entity> resultSorter) {
    this.resultSorter = requireNonNull(resultSorter, "resultSorter");
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
  public Optional<Entity> getSelectedEntity() {
    return selectedEntities.isEmpty() ? Optional.empty() : Optional.of(selectedEntities.get(0));
  }

  @Override
  public void setSelectedEntities(List<Entity> entities) {
    if (nullOrEmpty(entities) && selectedEntities.isEmpty()) {
      return;//no change
    }
    if (entities != null) {
      if (entities.size() > 1 && singleSelectionState.get()) {
        throw new IllegalArgumentException("This EntitySearchModel does not allow the selection of multiple entities");
      }
      entities.forEach(this::validateType);
    }
    //todo handle non-loaded entities, select from db?
    selectedEntities.clear();
    if (entities != null) {
      selectedEntities.addAll(entities);
    }
    resetSearchString();
    selectionEmptyState.set(selectedEntities.isEmpty());
    selectedEntitiesChangedEvent.accept(unmodifiableList(selectedEntities));
  }

  @Override
  public List<Entity> getSelectedEntities() {
    return unmodifiableList(selectedEntities);
  }

  @Override
  public Map<Column<String>, SearchSettings> columnSearchSettings() {
    return columnSearchSettings;
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
  public void resetSearchString() {
    setSearchString(selectedEntitiesToString());
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public void setSearchString(String searchString) {
    searchStringValue.set(searchString == null ? "" : searchString);
    searchStringRepresentsSelectedState.set(searchStringRepresentsSelected());
  }

  @Override
  public String getSearchString() {
    return searchStringValue.get();
  }

  @Override
  public boolean searchStringRepresentsSelected() {
    String selectedAsString = selectedEntitiesToString();
    return (selectedEntities.isEmpty() && nullOrEmpty(searchStringValue.get()))
            || !selectedEntities.isEmpty() && selectedAsString.equals(searchStringValue.get());
  }

  @Override
  public List<Entity> performQuery() {
    try {
      List<Entity> result = connectionProvider.connection().select(select());
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
  public State singleSelectionState() {
    return singleSelectionState;
  }

  @Override
  public void addSelectedEntitiesListener(Consumer<List<Entity>> listener) {
    selectedEntitiesChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedEntitiesListener(Consumer<List<Entity>> listener) {
    selectedEntitiesChangedEvent.removeDataListener(listener);
  }

  @Override
  public StateObserver searchStringRepresentsSelectedObserver() {
    return searchStringRepresentsSelectedState.observer();
  }

  @Override
  public StateObserver selectionEmptyObserver() {
    return selectionEmptyState.observer();
  }

  /**
   * @return a select instance based on this search model including any additional search condition
   * @throws IllegalStateException in case no search columns are specified
   * @see #setAdditionalConditionSupplier(Supplier)
   */
  private Select select() {
    if (searchColumns.isEmpty()) {
      throw new IllegalStateException("No search columns provided for search model: " + entityType);
    }
    Collection<Condition> conditions = new ArrayList<>();
    String[] searchStrings = singleSelectionState.get() ?
            new String[] {searchStringValue.get()} : searchStringValue.get().split(multipleItemSeparatorValue.get());
    for (Column<String> searchColumn : searchColumns) {
      SearchSettings searchSettings = columnSearchSettings.get(searchColumn);
      for (String rawSearchString : searchStrings) {
        ColumnCondition.Builder<String> builder = column(searchColumn);
        String preparedSearchString = prepareSearchString(rawSearchString, searchSettings);
        boolean containsWildcards = containsWildcards(preparedSearchString);
        if (searchSettings.caseSensitiveState().get()) {
          conditions.add(containsWildcards ? builder.like(preparedSearchString) : builder.equalTo(preparedSearchString));
        }
        else {
          conditions.add(containsWildcards ? builder.likeIgnoreCase(preparedSearchString) : builder.equalToIgnoreCase(preparedSearchString));
        }
      }
    }
    Condition conditionCombination = or(conditions);
    Select.Builder selectBuilder = additionalConditionSupplier == null ?
            Select.where(conditionCombination) :
            Select.where(and(additionalConditionSupplier.get(), conditionCombination));

    return selectBuilder
            .orderBy(connectionProvider.entities().definition(entityType).orderBy())
            .build();
  }

  private String prepareSearchString(String rawSearchString, SearchSettings searchSettings) {
    boolean wildcardPrefix = searchSettings.wildcardPrefixState().get();
    boolean wildcardPostfix = searchSettings.wildcardPostfixState().get();

    return rawSearchString.equals(String.valueOf(wildcardValue.get())) ? String.valueOf(wildcardValue.get()) :
            ((wildcardPrefix ? wildcardValue.get() : "") + rawSearchString.trim() + (wildcardPostfix ? wildcardValue.get() : ""));
  }

  private void bindEventsInternal() {
    searchStringValue.addListener(() ->
            searchStringRepresentsSelectedState.set(searchStringRepresentsSelected()));
    multipleItemSeparatorValue.addListener(this::resetSearchString);
  }

  private String createDescription() {
    EntityDefinition definition = connectionProvider.entities().definition(entityType);

    return searchColumns.stream()
            .map(column -> definition.columnDefinition(column).caption())
            .collect(joining(", "));
  }

  private String selectedEntitiesToString() {
    return selectedEntities.stream()
            .map(toStringProvider)
            .collect(joining(multipleItemSeparatorValue.get()));
  }

  private void validateType(Entity entity) {
    if (!entity.entityType().equals(entityType)) {
      throw new IllegalArgumentException("Entities of type " + entityType + " exptected, got " + entity.entityType());
    }
  }

  private static boolean containsWildcards(String value) {
    return value.contains("%") || value.contains("_");
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

  static final class DefaultBuilder implements Builder {

    private final EntityType entityType;
    private final EntityConnectionProvider connectionProvider;
    private Collection<Column<String>> searchColumns;
    private Function<Entity, String> toStringProvider = DEFAULT_TO_STRING;
    private Comparator<Entity> resultSorter = new EntityComparator();
    private String description;
    private boolean singleSelection = false;
    private String multipleItemSeparator = DEFAULT_SEPARATOR;

    DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
      this.entityType = requireNonNull(entityType);
      this.connectionProvider = requireNonNull(connectionProvider);
      this.searchColumns = connectionProvider.entities().definition(entityType).searchColumns();
    }

    @Override
    public Builder searchColumns(Collection<Column<String>> searchColumns) {
      if (requireNonNull(searchColumns).isEmpty()) {
        throw new IllegalArgumentException("One or more search column is required");
      }
      validateSearchColumns(searchColumns);
      this.searchColumns = searchColumns;
      return this;
    }

    @Override
    public Builder toStringProvider(Function<Entity, String> toStringProvider) {
      this.toStringProvider = requireNonNull(toStringProvider);
      return this;
    }

    @Override
    public Builder resultSorter(Comparator<Entity> resultSorter) {
      this.resultSorter = resultSorter;
      return this;
    }

    @Override
    public Builder description(String description) {
      this.description = requireNonNull(description);
      return this;
    }

    @Override
    public Builder singleSelection(boolean singleSelection) {
      this.singleSelection = singleSelection;
      return this;
    }

    @Override
    public Builder multipleItemSeparator(String multipleItemSeparator) {
      this.multipleItemSeparator = requireNonNull(multipleItemSeparator);
      return this;
    }

    @Override
    public EntitySearchModel build() {
      return new DefaultEntitySearchModel(this);
    }

    private void validateSearchColumns(Collection<Column<String>> searchColumns) {
      for (Column<String> column : searchColumns) {
        if (!entityType.equals(column.entityType())) {
          throw new IllegalArgumentException("Column '" + column + "' is not part of entity " + entityType);
        }
      }
    }
  }
}
