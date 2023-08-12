/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.ForeignKey.Reference;

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

final class DefaultForeignKeyCriteriaBuilder implements ForeignKeyCriteria.Builder {

  private static final String VALUES_PARAMETER = "values";

  private final ForeignKey foreignKey;

  DefaultForeignKeyCriteriaBuilder(ForeignKey foreignKey) {
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public Criteria equalTo(Entity value) {
    if (value == null) {
      return isNull();
    }
    List<Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      Reference<Object> reference = (Reference<Object>) references.get(0);

      return new DefaultColumnCriteriaBuilder<>(reference.column()).equalTo(value.get(reference.referencedColumn()));
    }

    return new DefaultCriteriaCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnCriteriaBuilder<>(reference.column()).equalTo(value.get(reference.referencedColumn())))
            .collect(toList()));
  }

  @Override
  public Criteria notEqualTo(Entity value) {
    if (value == null) {
      return isNotNull();
    }

    List<Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      Reference<Object> reference = (Reference<Object>) references.get(0);

      return new DefaultColumnCriteriaBuilder<>(reference.column()).notEqualTo(value.get(reference.referencedColumn()));
    }

    return new DefaultCriteriaCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultColumnCriteriaBuilder<>(reference.column()).notEqualTo(value.get(reference.referencedColumn())))
            .collect(toList()));
  }

  @Override
  public Criteria in(Entity... values) {
    return in(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public Criteria notIn(Entity... values) {
    return notIn(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public Criteria in(Collection<? extends Entity> values) {
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedColumn)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Criteria notIn(Collection<? extends Entity> values) {
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedColumn)
            .collect(toList());

    return foreignKeyCondition(foreignKey, NOT_EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Criteria isNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnCriteriaBuilder<>(column).isNull();
    }

    return new DefaultCriteriaCombination(AND, columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnCriteriaBuilder<>(column).isNull())
            .collect(toList()));
  }

  @Override
  public Criteria isNotNull() {
    List<Column<?>> columns = foreignKey.references().stream()
            .map(Reference::column)
            .collect(toList());
    if (columns.size() == 1) {
      Column<Object> column = (Column<Object>) columns.get(0);

      return new DefaultColumnCriteriaBuilder<>(column).isNotNull();
    }

    return new DefaultCriteriaCombination(AND, columns.stream()
            .map(column -> (Column<Object>) column)
            .map(column -> new DefaultColumnCriteriaBuilder<>(column).isNotNull())
            .collect(toList()));
  }

  static Criteria compositeKeyCriteria(Map<Column<?>, Column<?>> attributes, Operator operator,
                                       List<Map<Column<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeEqualCriteria(attributes, operator, valueMaps.get(0));
    }

    return new DefaultCriteriaCombination(OR, valueMaps.stream()
            .map(valueMap -> compositeEqualCriteria(attributes, operator, valueMap))
            .collect(toList()));
  }

  static Criteria compositeEqualCriteria(Map<Column<?>, Column<?>> attributes,
                                         Operator operator, Map<Column<?>, ?> valueMap) {
    return new DefaultCriteriaCombination(AND, attributes.entrySet().stream()
            .map(entry -> equalCriteria(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Criteria foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                              List<Map<Column<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCriteria(attributeMap(foreignKey.references()), operator, valueMaps);
    }

    return inCriteria(foreignKey.references().get(0), operator, valueMaps.stream()
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

  private static ColumnCriteria<Object> inCriteria(Reference<?> reference, Operator operator, List<Object> values) {
    ColumnCriteria.Builder<Object> criteriaBuilder = new DefaultColumnCriteriaBuilder<>((Column<Object>) reference.column());
    switch (operator) {
      case EQUAL:
        return criteriaBuilder.in(values);
      case NOT_EQUAL:
        return criteriaBuilder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Criteria equalCriteria(Column<?> conditionColumn, Operator operator, Object value) {
    ColumnCriteria.Builder<Object> criteriaBuilder = new DefaultColumnCriteriaBuilder<>((Column<Object>) conditionColumn);
    switch (operator) {
      case EQUAL:
        return criteriaBuilder.equalTo(value);
      case NOT_EQUAL:
        return criteriaBuilder.notEqualTo(value);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }
}
