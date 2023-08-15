/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.NOT_EQUAL;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyConditionBuilder implements ForeignKeyCondition.Builder {

  private static final String VALUES_PARAMETER = "values";

  private final ForeignKey foreignKey;

  DefaultForeignKeyConditionBuilder(ForeignKey foreignKey) {
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

      return new DefaultColumnConditionBuilder<>(reference.column()).equalTo(value.get(reference.referencedColumn()));
    }

    return new DefaultConditionCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnConditionBuilder<>(reference.column()).equalTo(value.get(reference.referencedColumn())))
            .collect(toList()));
  }

  @Override
  public Condition notEqualTo(Entity value) {
    if (value == null) {
      return isNotNull();
    }

    List<Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      Reference<Object> reference = (Reference<Object>) references.get(0);

      return new DefaultColumnConditionBuilder<>(reference.column()).notEqualTo(value.get(reference.referencedColumn()));
    }

    return new DefaultConditionCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnConditionBuilder<>(reference.column()).notEqualTo(value.get(reference.referencedColumn())))
            .collect(toList()));
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
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedColumn)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition notIn(Collection<? extends Entity> values) {
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedColumn)
            .collect(toList());

    return foreignKeyCondition(foreignKey, NOT_EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition isNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnConditionBuilder<>(column).isNull();
    }

    return new DefaultConditionCombination(AND, columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnConditionBuilder<>(column).isNull())
            .collect(toList()));
  }

  @Override
  public Condition isNotNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnConditionBuilder<>(column).isNotNull();
    }

    return new DefaultConditionCombination(AND, columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnConditionBuilder<>(column).isNotNull())
            .collect(toList()));
  }

  static Condition compositeKeyCondition(Map<Column<?>, Column<?>> attributes, Operator operator,
                                         List<Map<Column<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeEqualCondition(attributes, operator, valueMaps.get(0));
    }

    return new DefaultConditionCombination(OR, valueMaps.stream()
            .map(valueMap -> compositeEqualCondition(attributes, operator, valueMap))
            .collect(toList()));
  }

  static Condition compositeEqualCondition(Map<Column<?>, Column<?>> attributes,
                                           Operator operator, Map<Column<?>, ?> valueMap) {
    return new DefaultConditionCombination(AND, attributes.entrySet().stream()
            .map(entry -> equalCondition(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Condition foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                               List<Map<Column<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCondition(attributeMap(foreignKey.references()), operator, valueMaps);
    }

    return inCondition(foreignKey.references().get(0), operator, valueMaps.stream()
            .map(map -> map.get(foreignKey.references().get(0).referencedColumn()))
            .collect(toList()));
  }

  private static Map<Column<?>, Column<?>> attributeMap(List<Reference<?>> references) {
    Map<Column<?>, Column<?>> map = new LinkedHashMap<>(references.size());
    references.forEach(reference -> map.put(reference.column(), reference.referencedColumn()));

    return map;
  }

  private static Map<Column<?>, Object> valueMap(Entity entity, List<Column<?>> attributes) {
    Map<Column<?>, Object> values = new HashMap<>();
    attributes.forEach(attribute -> values.put(attribute, entity.get(attribute)));

    return values;
  }

  private static ColumnCondition<Object> inCondition(Reference<?> reference, Operator operator, List<Object> values) {
    ColumnCondition.Builder<Object> conditionBuilder = new DefaultColumnConditionBuilder<>((Column<Object>) reference.column());
    switch (operator) {
      case EQUAL:
        return conditionBuilder.in(values);
      case NOT_EQUAL:
        return conditionBuilder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Condition equalCondition(Column<?> conditionColumn, Operator operator, Object value) {
    ColumnCondition.Builder<Object> conditionBuilder = new DefaultColumnConditionBuilder<>((Column<Object>) conditionColumn);
    switch (operator) {
      case EQUAL:
        return conditionBuilder.equalTo(value);
      case NOT_EQUAL:
        return conditionBuilder.notEqualTo(value);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }
}
