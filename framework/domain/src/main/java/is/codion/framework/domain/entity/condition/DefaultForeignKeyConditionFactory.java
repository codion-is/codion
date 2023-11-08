/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.NOT_EQUAL;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultForeignKeyConditionFactory implements ForeignKeyCondition.Factory {

  private static final String VALUES_PARAMETER = "values";

  private final ForeignKey foreignKey;

  DefaultForeignKeyConditionFactory(ForeignKey foreignKey) {
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public Condition equalTo(Entity value) {
    if (value == null) {
      return isNull();
    }
    List<Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      Reference<Object> reference = (Reference<Object>) references.get(0);

      return new DefaultColumnConditionFactory<>(reference.column()).equalTo(value.get(reference.foreign()));
    }

    List<Condition> conditions = references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnConditionFactory<>(reference.column()).equalTo(value.get(reference.foreign())))
            .collect(toList());

    return new DefaultConditionCombination(AND, conditions);
  }

  @Override
  public Condition notEqualTo(Entity value) {
    if (value == null) {
      return isNotNull();
    }

    List<Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      Reference<Object> reference = (Reference<Object>) references.get(0);

      return new DefaultColumnConditionFactory<>(reference.column()).notEqualTo(value.get(reference.foreign()));
    }

    List<Condition> conditions = references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnConditionFactory<>(reference.column()).notEqualTo(value.get(reference.foreign())))
            .collect(toList());

    return new DefaultConditionCombination(AND, conditions);
  }

  @Override
  public Condition in(Entity... values) {
    return in(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public Condition notIn(Entity... values) {
    return notIn(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public Condition in(Collection<? extends Entity> values) {
    return foreignKeyCondition(foreignKey, EQUAL, createValueMaps(values));
  }

  @Override
  public Condition notIn(Collection<? extends Entity> values) {
    return foreignKeyCondition(foreignKey, NOT_EQUAL, createValueMaps(values));
  }

  @Override
  public Condition isNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnConditionFactory<>(column).isNull();
    }

    List<Condition> conditions = columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnConditionFactory<>(column).isNull())
            .collect(toList());

    return new DefaultConditionCombination(AND, conditions);
  }

  @Override
  public Condition isNotNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnConditionFactory<>(column).isNotNull();
    }

    List<Condition> conditions = columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnConditionFactory<>(column).isNotNull())
            .collect(toList());

    return new DefaultConditionCombination(AND, conditions);
  }

  private List<Map<Column<?>, ?>> createValueMaps(Collection<? extends Entity> values) {
    List<Column<?>> referencedColumns = foreignKey.references().stream()
            .map(Reference::foreign)
            .collect(toList());

    return values.stream()
            .map(entity -> valueMap(entity, referencedColumns))
            .collect(toList());
  }

  static Condition compositeKeyCondition(Map<Column<?>, Column<?>> columnReferenceMap, Operator operator,
                                         List<Map<Column<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeEqualCondition(columnReferenceMap, operator, valueMaps.get(0));
    }

    List<Condition> conditions = valueMaps.stream()
            .map(valueMap -> compositeEqualCondition(columnReferenceMap, operator, valueMap))
            .collect(toList());

    return new DefaultConditionCombination(OR, conditions);
  }

  static Condition compositeEqualCondition(Map<Column<?>, Column<?>> columnReferenceMap,
                                           Operator operator, Map<Column<?>, ?> valueMap) {
    List<Condition> conditions = columnReferenceMap.entrySet().stream()
            .map(entry -> equalCondition(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList());

    return new DefaultConditionCombination(AND, conditions);
  }

  private static Condition foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                               List<Map<Column<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCondition(columnReferenceMap(foreignKey.references()), operator, valueMaps);
    }

    List<Object> values = valueMaps.stream()
            .map(map -> map.get(foreignKey.references().get(0).foreign()))
            .collect(toList());

    return inCondition(foreignKey.references().get(0), operator, values);
  }

  private static Map<Column<?>, Column<?>> columnReferenceMap(List<Reference<?>> references) {
    return references.stream()
            .collect(toMap(Reference::column, Reference::foreign, (column, column2) -> column, LinkedHashMap::new));
  }

  private static Map<Column<?>, Object> valueMap(Entity entity, List<Column<?>> columns) {
    return columns.stream()
            .collect(toMap(Function.identity(), entity::get));
  }

  private static ColumnCondition<Object> inCondition(Reference<?> reference, Operator operator, List<Object> values) {
    Column<Object> column = (Column<Object>) reference.column();
    switch (operator) {
      case EQUAL:
        return column.in(values);
      case NOT_EQUAL:
        return column.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Condition equalCondition(Column<?> conditionColumn, Operator operator, Object value) {
    Column<Object> column = (Column<Object>) conditionColumn;
    switch (operator) {
      case EQUAL:
        return column.equalTo(value);
      case NOT_EQUAL:
        return column.notEqualTo(value);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }
}
