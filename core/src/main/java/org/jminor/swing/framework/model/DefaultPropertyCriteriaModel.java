/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Collection;

/**
 * A class for searching a set of entities based on a property.
 */
public class DefaultPropertyCriteriaModel extends DefaultColumnCriteriaModel<Property.ColumnProperty>
        implements PropertyCriteriaModel<Property.ColumnProperty> {

  /**
   * Constructs a DefaultPropertyCriteriaModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultPropertyCriteriaModel(final Property.ColumnProperty property) {
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
    return getSearchType().getValues().equals(SearchType.Values.TWO) ?
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), Arrays.asList(getLowerBound(), getUpperBound())) :
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), getUpperBound());
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
