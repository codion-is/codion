/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a order by clause
 */
public final class OrderBy implements Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByProperty> orderByProperties = new LinkedList<>();

  OrderBy() {}

  /**
   * Adds an 'ascending' order by for the given properties
   * @param propertyIds the property ids
   * @return this OrderBy instance
   */
  public OrderBy ascending(final String... propertyIds) {
    add(false, propertyIds);
    return this;
  }

  /**
   * Adds a 'descending' order by for the given properties
   * @param propertyIds the property ids
   * @return this OrderBy instance
   */
  public OrderBy descending(final String... propertyIds) {
    add(true, propertyIds);
    return this;
  }

  /**
   * Returns a order by string without the ORDER BY keywords
   * @param entityDefinition the entity definition
   * @return a order by string without the ORDER BY keywords
   */
  public String getOrderByString(final Entity.Definition entityDefinition) {
    final List<String> orderBys = new LinkedList<>();
    for (final OrderByProperty property : orderByProperties) {
      orderBys.add(entityDefinition.getColumnProperty(property.propertyId).getColumnName() +
              (property.descending ? " desc" : ""));
    }

    return String.join(", ", orderBys);
  }

  private void add(final boolean descending, final String... propertyIds) {
    requireNonNull(propertyIds, "propertyIds");
    for (final String propertyId : propertyIds) {
      final OrderByProperty property = new OrderByProperty(propertyId, descending);
      if (orderByProperties.contains(property)) {
        throw new IllegalArgumentException("Order by already contains property: " + propertyId);
      }
      orderByProperties.add(property);
    }
  }

  private static final class OrderByProperty implements Serializable {

    private static final long serialVersionUID = 1;

    private final String propertyId;
    private final boolean descending;

    private OrderByProperty(final String propertyId, final boolean descending) {
      this.propertyId = requireNonNull(propertyId, "propertyId");
      this.descending = descending;
    }

    @Override
    public boolean equals(final Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      return propertyId.equals(((OrderByProperty) object).propertyId);
    }

    @Override
    public int hashCode() {
      return propertyId.hashCode();
    }
  }
}
