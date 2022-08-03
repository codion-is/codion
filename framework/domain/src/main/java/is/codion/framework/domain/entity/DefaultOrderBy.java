/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByAttribute> orderByAttributes;

  private DefaultOrderBy(DefaultOrderByBuilder builder) {
    this.orderByAttributes = unmodifiableList(builder.orderByAttributes);
  }

  @Override
  public List<OrderByAttribute> getOrderByAttributes() {
    return orderByAttributes;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultOrderBy)) {
      return false;
    }
    DefaultOrderBy that = (DefaultOrderBy) object;
    return orderByAttributes.equals(that.orderByAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderByAttributes);
  }

  private static final class DefaultOrderByAttribute implements OrderByAttribute, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<?> attribute;
    private final boolean ascending;

    private DefaultOrderByAttribute(Attribute<?> attribute, boolean ascending) {
      this.attribute = requireNonNull(attribute, "attribute");
      this.ascending = ascending;
    }

    @Override
    public Attribute<?> getAttribute() {
      return attribute;
    }

    @Override
    public boolean isAscending() {
      return ascending;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      DefaultOrderByAttribute that = (DefaultOrderByAttribute) object;
      return attribute.equals(that.attribute) &&
              ascending == that.ascending;
    }

    @Override
    public int hashCode() {
      return Objects.hash(attribute, ascending);
    }
  }

  static final class DefaultOrderByBuilder implements Builder {

    private final List<OrderByAttribute> orderByAttributes = new ArrayList<>(1);

    @Override
    public Builder ascending(Attribute<?>... attributes) {
      add(true, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder descending(Attribute<?>... attributes) {
      add(false, requireNonNull(attributes));
      return this;
    }

    @Override
    public OrderBy build() {
      return new DefaultOrderBy(this);
    }

    private void add(boolean ascending, Attribute<?>... attributes) {
      if (attributes.length == 0) {
        throw new IllegalArgumentException("One or more attributes required for order by");
      }
      for (Attribute<?> attribute : attributes) {
        for (OrderByAttribute orderByAttribute : orderByAttributes) {
          if (requireNonNull(attribute).equals(orderByAttribute.getAttribute())) {
            throw new IllegalArgumentException("Order by already contains attribute: " + attribute);
          }
        }
        orderByAttributes.add(new DefaultOrderByAttribute(attribute, ascending));
      }
    }
  }
}
