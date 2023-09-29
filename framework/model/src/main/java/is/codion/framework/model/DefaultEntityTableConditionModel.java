/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnCondition;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.model.table.TableConditionModel.tableConditionModel;
import static is.codion.framework.domain.entity.attribute.Condition.all;
import static is.codion.framework.domain.entity.attribute.Condition.combination;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel<C extends Attribute<?>> implements EntityTableConditionModel<C> {

  private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final TableConditionModel<C> conditionModel;
  private final Event<Condition> conditionChangedEvent = Event.event();
  private final Value<Supplier<Condition>> additionalCondition = Value.value(NULL_CONDITION_SUPPLIER, NULL_CONDITION_SUPPLIER);
  private final Value<Conjunction> conjunction = Value.value(Conjunction.AND, Conjunction.AND);

  DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                   ColumnConditionModel.Factory<C> conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.conditionModel = tableConditionModel(createConditionModels(entityType, conditionModelFactory));
    bindEvents();
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values) {
    Condition condition = condition();
    ColumnConditionModel<Attribute<T>, T> columnConditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModel.conditionModels().get(attribute);
    if (columnConditionModel != null) {
      columnConditionModel.operator().set(Operator.EQUAL);
      columnConditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      columnConditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      columnConditionModel.enabled().set(!nullOrEmpty(values));
    }
    return !condition.equals(condition());
  }

  @Override
  public Condition condition() {
    Collection<Condition> conditions = conditionModel.conditionModels().values().stream()
            .filter(model -> model.enabled().get())
            .map(DefaultEntityTableConditionModel::condition)
            .collect(Collectors.toCollection(ArrayList::new));
    Condition additionalCondition = this.additionalCondition.get().get();
    if (additionalCondition != null) {
      conditions.add(additionalCondition);
    }

    return conditions.isEmpty() ? all(entityType) : combination(conjunction.get(), conditions);
  }

  @Override
  public Value<Supplier<Condition>> additionalCondition() {
    return additionalCondition;
  }

  @Override
  public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
    return conditionModel.conditionModels();
  }

  @Override
  public <T> ColumnConditionModel<? extends C, T> conditionModel(C columnIdentifier) {
    return conditionModel.conditionModel(columnIdentifier);
  }

  @Override
  public <A extends Attribute<T>, T> ColumnConditionModel<A, T> attributeModel(A columnIdentifier) {
    return (ColumnConditionModel<A, T>) conditionModel((C) columnIdentifier);
  }

  @Override
  public boolean isEnabled() {
    return conditionModel.isEnabled();
  }

  @Override
  public boolean isEnabled(C columnIdentifier) {
    return conditionModel.isEnabled(columnIdentifier);
  }

  @Override
  public void addChangeListener(Runnable listener) {
    conditionModel.addChangeListener(listener);
  }

  @Override
  public void removeChangeListener(Runnable listener) {
    conditionModel.removeChangeListener(listener);
  }

  @Override
  public void clear() {
    conditionModel.clear();
  }

  @Override
  public Value<Conjunction> conjunction() {
    return conjunction;
  }

  @Override
  public void addChangeListener(Consumer<Condition> listener) {
    conditionChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeChangeListener(Consumer<Condition> listener) {
    conditionChangedEvent.removeDataListener(listener);
  }

  private void bindEvents() {
    Runnable listener = () -> conditionChangedEvent.accept(condition());
    conditionModel.conditionModels().values().forEach(columnConditionModel ->
            columnConditionModel.addChangeListener(listener));
    additionalCondition.addListener(listener);
    conjunction.addListener(listener);
  }

  private Collection<ColumnConditionModel<C, ?>> createConditionModels(EntityType entityType,
                                                                       ColumnConditionModel.Factory<C> conditionModelFactory) {
    Collection<ColumnConditionModel<? extends C, ?>> models = new ArrayList<>();
    EntityDefinition definition = connectionProvider.entities().definition(entityType);
    definition.columns().definitions().forEach(columnDefinition ->
            conditionModelFactory.createConditionModel((C) columnDefinition.attribute()).ifPresent(models::add));
    definition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
            conditionModelFactory.createConditionModel((C) foreignKeyDefinition.attribute()).ifPresent(models::add));

    return models.stream()
            .map(model -> (ColumnConditionModel<C, ?>) model)
            .collect(Collectors.toList());
  }

  private static Condition condition(ColumnConditionModel<?, ?> conditionModel) {
    if (conditionModel.columnIdentifier() instanceof ForeignKey) {
      return foreignKeyCondition((ColumnConditionModel<?, Entity>) conditionModel);
    }

    return columnCondition(conditionModel);
  }

  private static Condition foreignKeyCondition(ColumnConditionModel<?, Entity> conditionModel) {
    ForeignKey foreignKey = (ForeignKey) conditionModel.columnIdentifier();
    Collection<Entity> values = conditionModel.equalValues().get();
    switch (conditionModel.operator().get()) {
      case EQUAL:
        return values.isEmpty() ? foreignKey.isNull() : foreignKey.in(values);
      case NOT_EQUAL:
        return values.isEmpty() ? foreignKey.isNotNull() : foreignKey.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.operator().get() + " for foreign key condition");
    }
  }

  private static <T> ColumnCondition<T> columnCondition(ColumnConditionModel<?, T> conditionModel) {
    Column<T> column = (Column<T>) conditionModel.columnIdentifier();
    switch (conditionModel.operator().get()) {
      case EQUAL:
        return equalCondition(conditionModel, column);
      case NOT_EQUAL:
        return notEqualCondition(conditionModel, column);
      case LESS_THAN:
        return column.lessThan(conditionModel.getUpperBound());
      case LESS_THAN_OR_EQUAL:
        return column.lessThanOrEqualTo(conditionModel.getUpperBound());
      case GREATER_THAN:
        return column.greaterThan(conditionModel.getLowerBound());
      case GREATER_THAN_OR_EQUAL:
        return column.greaterThanOrEqualTo(conditionModel.getLowerBound());
      case BETWEEN_EXCLUSIVE:
        return column.betweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case BETWEEN:
        return column.between(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case NOT_BETWEEN_EXCLUSIVE:
        return column.notBetweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case NOT_BETWEEN:
        return column.notBetween(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.operator().get());
    }
  }

  private static <T> ColumnCondition<T> equalCondition(ColumnConditionModel<?, T> conditionModel,
                                                       Column<T> column) {
    Collection<T> equalToValues = conditionModel.getEqualValues();
    if (equalToValues.isEmpty()) {
      return column.isNull();
    }
    if (column.type().isString() && equalToValues.size() == 1) {
      return singleStringEqualCondition(conditionModel, column, (String) equalToValues.iterator().next());
    }

    return column.in(equalToValues);
  }

  private static <T> ColumnCondition<T> notEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                          Column<T> column) {
    Collection<T> equalToValues = conditionModel.getEqualValues();
    if (equalToValues.isEmpty()) {
      return column.isNotNull();
    }
    if (column.type().isString() && equalToValues.size() == 1) {
      return singleStringNotEqualCondition(conditionModel, column, (String) equalToValues.iterator().next());
    }

    return column.notIn(equalToValues);
  }

  private static <T> ColumnCondition<T> singleStringEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                                   Column<T> column, String value) {
    boolean caseSensitive = conditionModel.caseSensitive().get();
    if (containsWildcards(value)) {
      return (ColumnCondition<T>) (caseSensitive ? column.like(value) : column.likeIgnoreCase(value));
    }

    return caseSensitive ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
  }

  private static <T> ColumnCondition<T> singleStringNotEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                                      Column<T> column, String value) {
    boolean caseSensitive = conditionModel.caseSensitive().get();
    if (containsWildcards(value)) {
      return (ColumnCondition<T>) (caseSensitive ? column.notLike(value) : column.notLikeIgnoreCase(value));
    }

    return caseSensitive ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
  }

  private static boolean containsWildcards(String value) {
    return value != null && (value.contains("%") || value.contains("_"));
  }
}
