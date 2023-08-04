/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
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

final class DefaultForeignKeyConditionBuilder implements ForeignKeyConditionBuilder {

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

      return new DefaultAttributeConditionBuilder<>(reference.attribute()).equalTo(value.get(reference.referencedAttribute()));
    }

    return new DefaultConditionCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultAttributeConditionBuilder<>(reference.attribute()).equalTo(value.get(reference.referencedAttribute())))
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

      return new DefaultAttributeConditionBuilder<>(reference.attribute()).notEqualTo(value.get(reference.referencedAttribute()));
    }

    return new DefaultConditionCombination(AND, references.stream()
            .map(reference -> (Reference<Object>) reference)
            .map(reference -> new DefaultAttributeConditionBuilder<>(reference.attribute()).notEqualTo(value.get(reference.referencedAttribute())))
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
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition notIn(Collection<? extends Entity> values) {
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::referencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, NOT_EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition isNull() {
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::attribute)
            .collect(toList());
    if (attributes.size() == 1) {
      Attribute<Object> attribute = (Attribute<Object>) attributes.get(0);

      return new DefaultAttributeConditionBuilder<>(attribute).isNull();
    }

    return new DefaultConditionCombination(AND, attributes.stream()
            .map(attribute -> (Attribute<Object>) attribute)
            .map(attribute -> new DefaultAttributeConditionBuilder<>(attribute).isNull())
            .collect(toList()));
  }

  @Override
  public Condition isNotNull() {
    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(Reference::attribute)
            .collect(toList());
    if (attributes.size() == 1) {
      Attribute<Object> attribute = (Attribute<Object>) attributes.get(0);

      return new DefaultAttributeConditionBuilder<>(attribute).isNotNull();
    }

    return new DefaultConditionCombination(AND, attributes.stream()
            .map(attribute -> (Attribute<Object>) attribute)
            .map(attribute -> new DefaultAttributeConditionBuilder<>(attribute).isNotNull())
            .collect(toList()));
  }

  static Condition compositeKeyCondition(Map<Attribute<?>, Attribute<?>> attributes, Operator operator,
                                         List<Map<Attribute<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeEqualCondition(attributes, operator, valueMaps.get(0));
    }

    return new DefaultConditionCombination(OR, valueMaps.stream()
            .map(valueMap -> compositeEqualCondition(attributes, operator, valueMap))
            .collect(toList()));
  }

  static Condition compositeEqualCondition(Map<Attribute<?>, Attribute<?>> attributes,
                                           Operator operator, Map<Attribute<?>, ?> valueMap) {
    return new DefaultConditionCombination(AND, attributes.entrySet().stream()
            .map(entry -> equalCondition(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Condition foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                               List<Map<Attribute<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCondition(attributeMap(foreignKey.references()), operator, valueMaps);
    }

    return inCondition(foreignKey.references().get(0), operator, valueMaps.stream()
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

  private static AttributeCondition<Object> inCondition(Reference<?> reference, Operator operator, List<Object> values) {
    AttributeCondition.Builder<Object> conditionBuilder = new DefaultAttributeConditionBuilder<>((Attribute<Object>) reference.attribute());
    switch (operator) {
      case EQUAL:
        return conditionBuilder.in(values);
      case NOT_EQUAL:
        return conditionBuilder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Condition equalCondition(Attribute<?> conditionAttribute, Operator operator, Object value) {
    AttributeCondition.Builder<Object> conditionBuilder = new DefaultAttributeConditionBuilder<>((Attribute<Object>) conditionAttribute);
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
