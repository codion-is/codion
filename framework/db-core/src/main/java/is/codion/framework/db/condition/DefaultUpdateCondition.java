/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Attribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdateCondition extends DefaultCondition implements UpdateCondition {

  private static final long serialVersionUID = 1;

  private final Map<Attribute<?>, Object> propertyValues;

  private DefaultUpdateCondition(DefaultUpdateCondition.DefaultBuilder builder) {
    super(builder.criteria);
    this.propertyValues = builder.propertyValues;
  }

  @Override
  public Map<Attribute<?>, Object> attributeValues() {
    return unmodifiableMap(propertyValues);
  }

  @Override
  public boolean equals(Object object) {
    return this == object;
  }

  @Override
  public int hashCode() {
    return Objects.hash(criteria(), propertyValues);
  }

  static final class DefaultBuilder implements UpdateCondition.Builder {

    private final Criteria criteria;
    private final Map<Attribute<?>, Object> propertyValues;

    DefaultBuilder(Condition condition) {
      this.criteria = requireNonNull(condition).criteria();
      if (condition instanceof DefaultUpdateCondition) {
        DefaultUpdateCondition updateCondition = (DefaultUpdateCondition) condition;
        this.propertyValues = updateCondition.propertyValues;
      }
      else {
        this.propertyValues = new LinkedHashMap<>();
      }
    }

    @Override
    public <T> Builder set(Attribute<?> attribute, T value) {
      requireNonNull(attribute, "attribute");
      if (propertyValues.containsKey(attribute)) {
        throw new IllegalArgumentException("Update condition already contains a value for attribute: " + attribute);
      }
      propertyValues.put(attribute, value);

      return this;
    }

    @Override
    public UpdateCondition build() {
      return new DefaultUpdateCondition(this);
    }
  }
}
