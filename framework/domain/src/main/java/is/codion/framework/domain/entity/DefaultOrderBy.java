/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByAttribute> orderByAttributes = new ArrayList<>(1);

  @Override
  public OrderBy ascending(Attribute<?>... attributes) {
    add(true, requireNonNull(attributes));
    return this;
  }

  @Override
  public OrderBy descending(Attribute<?>... attributes) {
    add(false, requireNonNull(attributes));
    return this;
  }

  @Override
  public List<OrderByAttribute> getOrderByAttributes() {
    return unmodifiableList(orderByAttributes);
  }

  private void add(boolean ascending, Attribute<?>... attributes) {
    if (attributes.length == 0) {
      throw new IllegalArgumentException("One or more attributes required for order by");
    }
    for (Attribute<?> attribute : attributes) {
      DefaultOrderByAttribute orderByAttribute = new DefaultOrderByAttribute(attribute, ascending);
      if (orderByAttributes.contains(orderByAttribute)) {
        throw new IllegalArgumentException("Order by already contains attribute: " + attribute);
      }
      orderByAttributes.add(orderByAttribute);
    }
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

      return attribute.equals(((DefaultOrderByAttribute) object).attribute);
    }

    @Override
    public int hashCode() {
      return attribute.hashCode();
    }
  }
}
