/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByAttribute> orderByAttributes = new ArrayList<>(1);

  @Override
  public OrderBy ascending(final Attribute<?>... attributes) {
    add(true, attributes);
    return this;
  }

  @Override
  public OrderBy descending(final Attribute<?>... attributes) {
    add(false, attributes);
    return this;
  }

  @Override
  public List<OrderByAttribute> getOrderByAttributes() {
    return orderByAttributes;
  }

  private void add(final boolean ascending, final Attribute<?>... attributes) {
    requireNonNull(attributes, "attributes");
    for (final Attribute<?> attribute : attributes) {
      final DefaultOrderByAttribute property = new DefaultOrderByAttribute(attribute, ascending);
      if (orderByAttributes.contains(property)) {
        throw new IllegalArgumentException("Order by already contains property: " + attribute);
      }
      orderByAttributes.add(property);
    }
  }

  private static final class DefaultOrderByAttribute implements OrderByAttribute, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<?> attribute;
    private final boolean ascending;

    private DefaultOrderByAttribute(final Attribute<?> attribute, final boolean ascending) {
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
    public boolean equals(final Object object) {
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
