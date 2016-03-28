/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Collection;

public final class PropertyCriteriaModel extends DefaultColumnCriteriaModel<Property.ColumnProperty> {

  public PropertyCriteriaModel(final Property.ColumnProperty property) {
    super(property, property.getColumnType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
  }

  public Criteria<Property.ColumnProperty> getCriteria() {
    return getSearchType().getValues().equals(SearchType.Values.TWO) ?
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), Arrays.asList(getLowerBound(), getUpperBound())) :
            EntityCriteriaUtil.propertyCriteria(getColumnIdentifier(), getSearchType(), isCaseSensitive(), getUpperBound());
  }

  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getColumnIdentifier().getPropertyID());
    if (isEnabled()) {
      stringBuilder.append(getSearchType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  private static String toString(final Object object) {
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
