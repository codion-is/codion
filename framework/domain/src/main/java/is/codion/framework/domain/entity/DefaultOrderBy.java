/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy {

  private static final long serialVersionUID = 1;

  private final List<OrderByProperty> orderByProperties = new ArrayList<>(1);

  @Override
  public OrderBy ascending(final String... propertyIds) {
    add(true, propertyIds);
    return this;
  }

  @Override
  public OrderBy descending(final String... propertyIds) {
    add(false, propertyIds);
    return this;
  }

  @Override
  public List<OrderByProperty> getOrderByProperties() {
    return orderByProperties;
  }

  private void add(final boolean ascending, final String... propertyIds) {
    requireNonNull(propertyIds, "propertyIds");
    for (final String propertyId : propertyIds) {
      final DefaultOrderByProperty property = new DefaultOrderByProperty(propertyId, ascending);
      if (orderByProperties.contains(property)) {
        throw new IllegalArgumentException("Order by already contains property: " + propertyId);
      }
      orderByProperties.add(property);
    }
  }

  private static final class DefaultOrderByProperty implements OrderBy.OrderByProperty {

    private static final long serialVersionUID = 1;

    private final String propertyId;
    private final boolean ascending;

    private DefaultOrderByProperty(final String propertyId, final boolean ascending) {
      this.propertyId = requireNonNull(propertyId, "propertyId");
      this.ascending = ascending;
    }

    @Override
    public String getPropertyId() {
      return propertyId;
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

      return propertyId.equals(((DefaultOrderByProperty) object).propertyId);
    }

    @Override
    public int hashCode() {
      return propertyId.hashCode();
    }
  }
}
