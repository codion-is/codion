/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Condition;

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
import static is.codion.framework.domain.entity.attribute.Condition.and;
import static is.codion.framework.domain.entity.attribute.Condition.or;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultEntitySearchModel implements EntitySearchModel {

  private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;
  private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;
  private static final String DEFAULT_SEPARATOR = ",";

  private final State searchStringModified = State.state();

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
  private final Value<List<Entity>> entities = Value.value(emptyList(), emptyList());

  /**
   * The EntityConnectionProvider instance used by this EntitySearchModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Contains the search settings for search columns
   */
  private final Map<Column<String>, SearchSettings> columnSearchSettings = new HashMap<>();

  private final Value<String> searchString = Value.value("", "");
  private final Value<String> multipleItemSeparator;
  private final State singleSelection = State.state(false);
  private final Value<Character> wildcard = Value.value(Text.WILDCARD_CHARACTER.get(), Text.WILDCARD_CHARACTER.get());
  private final Value<Supplier<Condition>> additionalConditionSupplier = Value.value(NULL_CONDITION_SUPPLIER, NULL_CONDITION_SUPPLIER);
  private final Value<Function<Entity, String>> toStringFunction = Value.value(DEFAULT_TO_STRING, DEFAULT_TO_STRING);
  private final State selectionEmpty = State.state(true);

  private Comparator<Entity> resultSorter;
  private String description;

  private DefaultEntitySearchModel(DefaultBuilder builder) {
    this.entityType = builder.entityType;
    this.connectionProvider = builder.connectionProvider;
    this.multipleItemSeparator = Value.value(builder.multipleItemSeparator, DEFAULT_SEPARATOR);
    this.searchColumns = unmodifiableCollection(builder.searchColumns);
    this.searchColumns.forEach(attribute -> columnSearchSettings.put(attribute, new DefaultSearchSettings()));
    this.toStringFunction.set(builder.toStringFunction);
    this.resultSorter = builder.resultSorter;
    this.description = builder.description == null ? createDescription() : builder.description;
    this.singleSelection.set(builder.singleSelection);
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
  public void setEntity(Entity entity) {
    setEntities(entity != null ? singletonList(entity) : null);
  }

  @Override
  public Optional<Entity> getEntity() {
    return entities.get().isEmpty() ? Optional.empty() : Optional.of(entities.get().get(0));
  }

  @Override
  public void setEntities(List<Entity> entities) {
    if (nullOrEmpty(entities) && this.entities.get().isEmpty()) {
      return;//no change
    }
    if (entities != null) {
      if (entities.size() > 1 && singleSelection.get()) {
        throw new IllegalArgumentException("This EntitySearchModel does not allow the selection of multiple entities");
      }
      entities.forEach(this::validateType);
    }
    //todo handle non-loaded entities, select from db?
    this.entities.set(null);
    if (entities != null) {
      this.entities.set(unmodifiableList(entities));
    }
    resetSearchString();
    selectionEmpty.set(this.entities.get().isEmpty());
  }

  @Override
  public List<Entity> getEntities() {
    return entities.get();
  }

  @Override
  public Map<Column<String>, SearchSettings> columnSearchSettings() {
    return columnSearchSettings;
  }

  @Override
  public Value<Character> wildcard() {
    return wildcard;
  }

  @Override
  public Value<Supplier<Condition>> additionalConditionSupplier() {
    return additionalConditionSupplier;
  }

  @Override
  public Value<Function<Entity, String>> toStringFunction() {
    return toStringFunction;
  }

  @Override
  public void resetSearchString() {
    searchString.set(selectedEntitiesToString());
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
  public Value<String> searchString() {
    return searchString;
  }

  @Override
  public Value<String> multipleItemSeparator() {
    return multipleItemSeparator;
  }

  @Override
  public State singleSelection() {
    return singleSelection;
  }

  @Override
  public void addListener(Consumer<List<Entity>> listener) {
    entities.addDataListener(listener);
  }

  @Override
  public void removeListener(Consumer<List<Entity>> listener) {
    entities.removeDataListener(listener);
  }

  @Override
  public StateObserver searchStringModified() {
    return searchStringModified.observer();
  }

  @Override
  public StateObserver selectionEmpty() {
    return selectionEmpty.observer();
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
    String[] searchStrings = singleSelection.get() ?
            new String[] {searchString.get()} : searchString.get().split(multipleItemSeparator.get());
    for (Column<String> column : searchColumns) {
      SearchSettings searchSettings = columnSearchSettings.get(column);
      for (String rawSearchString : searchStrings) {
        String preparedSearchString = prepareSearchString(rawSearchString, searchSettings);
        boolean containsWildcards = containsWildcards(preparedSearchString);
        if (searchSettings.caseSensitive().get()) {
          conditions.add(containsWildcards ? column.like(preparedSearchString) : column.equalTo(preparedSearchString));
        }
        else {
          conditions.add(containsWildcards ? column.likeIgnoreCase(preparedSearchString) : column.equalToIgnoreCase(preparedSearchString));
        }
      }
    }
    Condition conditionCombination = or(conditions);
    Condition additionalCondition = additionalConditionSupplier.get().get();
    Select.Builder selectBuilder = additionalCondition == null ?
            Select.where(conditionCombination) :
            Select.where(and(additionalCondition, conditionCombination));

    return selectBuilder
            .orderBy(connectionProvider.entities().definition(entityType).orderBy())
            .build();
  }

  private String prepareSearchString(String rawSearchString, SearchSettings searchSettings) {
    boolean wildcardPrefix = searchSettings.wildcardPrefix().get();
    boolean wildcardPostfix = searchSettings.wildcardPostfix().get();

    return rawSearchString.equals(String.valueOf(wildcard.get())) ? String.valueOf(wildcard.get()) :
            ((wildcardPrefix ? wildcard.get() : "") + rawSearchString.trim() + (wildcardPostfix ? wildcard.get() : ""));
  }

  private void bindEventsInternal() {
    searchString.addListener(() ->
            searchStringModified.set(!searchStringRepresentSelectedEntities()));
    multipleItemSeparator.addListener(this::resetSearchString);
  } 
  
  private boolean searchStringRepresentSelectedEntities() {
    String selectedAsString = selectedEntitiesToString();
    return (entities.get().isEmpty() && nullOrEmpty(searchString.get()))
            || !entities.get().isEmpty() && selectedAsString.equals(searchString.get());
  }

  private String createDescription() {
    EntityDefinition definition = connectionProvider.entities().definition(entityType);

    return searchColumns.stream()
            .map(column -> definition.columnDefinition(column).caption())
            .collect(joining(", "));
  }

  private String selectedEntitiesToString() {
    return entities.get().stream()
            .map(toStringFunction.get())
            .collect(joining(multipleItemSeparator.get()));
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
    public State wildcardPrefix() {
      return wildcardPrefixState;
    }

    @Override
    public State wildcardPostfix() {
      return wildcardPostfixState;
    }

    @Override
    public State caseSensitive() {
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
    private Function<Entity, String> toStringFunction = DEFAULT_TO_STRING;
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
    public Builder toStringFunction(Function<Entity, String> toStringFunction) {
      this.toStringFunction = requireNonNull(toStringFunction);
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
