/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
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

      return new DefaultAttributeCriteriaBuilder<>(reference.attribute()).equalTo(value.get(reference.referencedAttribute()));
    }

    return new DefaultCriteriaCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultAttributeCriteriaBuilder<>(reference.attribute()).equalTo(value.get(reference.referencedAttribute())))
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

      return new DefaultAttributeCriteriaBuilder<>(reference.attribute()).notEqualTo(value.get(reference.referencedAttribute()));
    }

    return new DefaultCriteriaCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultAttributeCriteriaBuilder<>(reference.attribute()).notEqualTo(value.get(reference.referencedAttribute())))
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
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Criteria notIn(Collection<? extends Entity> values) {
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, NOT_EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Criteria isNull() {
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::attribute)
            .collect(toList());
    if (attributes.size() == 1) {
      Column<Object> attribute = (Column<Object>) attributes.get(0);

      return new DefaultAttributeCriteriaBuilder<>(attribute).isNull();
    }

    return new DefaultCriteriaCombination(AND, attributes.stream()
            .map(attribute -> (Column<Object>) attribute)
            .map(attribute -> new DefaultAttributeCriteriaBuilder<>(attribute).isNull())
            .collect(toList()));
  }

  @Override
  public Criteria isNotNull() {
    List<Column<?>> attributes = foreignKey.references().stream()
            .map(Reference::attribute)
            .collect(toList());
    if (attributes.size() == 1) {
      Column<Object> attribute = (Column<Object>) attributes.get(0);

      return new DefaultAttributeCriteriaBuilder<>(attribute).isNotNull();
    }

    return new DefaultCriteriaCombination(AND, attributes.stream()
            .map(attribute -> (Column<Object>) attribute)
            .map(attribute -> new DefaultAttributeCriteriaBuilder<>(attribute).isNotNull())
            .collect(toList()));
  }

  static Criteria compositeKeyCriteria(Map<Attribute<?>, Attribute<?>> attributes, Operator operator,
                                       List<Map<Attribute<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeEqualCriteria(attributes, operator, valueMaps.get(0));
    }

    return new DefaultCriteriaCombination(OR, valueMaps.stream()
            .map(valueMap -> compositeEqualCriteria(attributes, operator, valueMap))
            .collect(toList()));
  }

  static Criteria compositeEqualCriteria(Map<Attribute<?>, Attribute<?>> attributes,
                                         Operator operator, Map<Attribute<?>, ?> valueMap) {
    return new DefaultCriteriaCombination(AND, attributes.entrySet().stream()
            .map(entry -> equalCriteria(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Criteria foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                              List<Map<Attribute<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCriteria(attributeMap(foreignKey.references()), operator, valueMaps);
    }

    return inCriteria(foreignKey.references().get(0), operator, valueMaps.stream()
            .map(map -> map.get(foreignKey.references().get(0).referencedAttribute()))
            .collect(toList()));
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(List<Reference<?>> references) {
    Map<Attribute<?>, Attribute<?>> map = new LinkedHashMap<>(references.size());
    references.forEach(reference -> map.put(reference.attribute(), reference.referencedAttribute()));

    return map;
  }

  private static Map<Attribute<?>, Object> valueMap(Entity entity, List<Attribute<?>> attributes) {
    Map<Attribute<?>, Object> values = new HashMap<>();
    attributes.forEach(attribute -> values.put(attribute, entity.get(attribute)));

    return values;
  }

  private static AttributeCriteria<Object> inCriteria(Reference<?> reference, Operator operator, List<Object> values) {
    AttributeCriteria.Builder<Object> criteriaBuilder = new DefaultAttributeCriteriaBuilder<>((Column<Object>) reference.attribute());
    switch (operator) {
      case EQUAL:
        return criteriaBuilder.in(values);
      case NOT_EQUAL:
        return criteriaBuilder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Criteria equalCriteria(Attribute<?> conditionAttribute, Operator operator, Object value) {
    AttributeCriteria.Builder<Object> criteriaBuilder = new DefaultAttributeCriteriaBuilder<>((Column<Object>) conditionAttribute);
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
