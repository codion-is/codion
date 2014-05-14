/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.table.DefaultColumnSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Collection;

/**
 * A class for searching a set of entities based on a property.
 */
public class DefaultPropertySearchModel extends DefaultColumnSearchModel<Property.ColumnProperty>
        implements PropertySearchModel<Property.ColumnProperty> {

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultPropertySearchModel(final Property.ColumnProperty property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER),
            property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getColumnIdentifier().getPropertyID());
    if (isEnabled()) {
      stringBuilder.append(getSearchType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getCriteria() {
    return getValueCount(getSearchType()) == 1 ?
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), getUpperBound()) :
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), Arrays.asList(getLowerBound(), getUpperBound()));
  }

  private String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : ((Collection) object)) {
        stringBuilder.append(toString(obj));
      }
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }
}
