/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;

public final class ForeignKeyCriteriaModel extends DefaultColumnCriteriaModel<Property.ForeignKeyProperty> {

  private final EntityConnectionProvider connectionProvider;

  public ForeignKeyCriteriaModel(final Property.ForeignKeyProperty property,
                                 final EntityConnectionProvider connectionProvider) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.connectionProvider = connectionProvider;
  }

  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public Criteria<Property.ColumnProperty> getCriteria() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Collection) {
      return EntityCriteriaUtil.foreignKeyCriteria(getColumnIdentifier(), getSearchType(), (Collection) upperBound);
    }

    return EntityCriteriaUtil.foreignKeyCriteria(getColumnIdentifier(), getSearchType(), Collections.singletonList(upperBound));
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
