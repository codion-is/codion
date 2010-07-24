/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.AbstractSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * A class for searching a set of entities based on a property.
 */
public class DefaultPropertySearchModel extends AbstractSearchModel<Property.ColumnProperty>
        implements PropertySearchModel<Property.ColumnProperty> {

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultPropertySearchModel(final Property.ColumnProperty property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
  }

  public boolean include(final Object object) {
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getSearchKey().getPropertyID());
    if (isSearchEnabled()) {
      stringBuilder.append(getSearchType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  public final Criteria<Property.ColumnProperty> getCriteria() {
    return getValueCount(getSearchType()) == 1 ?
            EntityCriteriaUtil.propertyCriteria(getSearchKey(), isCaseSensitive(), getSearchType(), getUpperBound()) :
            EntityCriteriaUtil.propertyCriteria(getSearchKey(), isCaseSensitive(), getSearchType(), getLowerBound(), getUpperBound());
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
