/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Collection;

/**
 * A class for searching a set of entities based on a property.
 */
public class DefaultPropertyConditionModel extends DefaultColumnConditionModel<Property.ColumnProperty>
        implements PropertyConditionModel<Property.ColumnProperty> {

  /**
   * Constructs a DefaultPropertyConditionModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultPropertyConditionModel(final Property.ColumnProperty property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER),
            property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getColumnIdentifier().getPropertyID());
    if (isEnabled()) {
      stringBuilder.append(getConditionType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public final Condition<Property.ColumnProperty> getCondition() {
    return getConditionType().getValues().equals(Condition.Type.Values.TWO) ?
            EntityConditions.propertyCondition(getColumnIdentifier(), getConditionType(), isCaseSensitive(), Arrays.asList(getLowerBound(), getUpperBound())) :
            EntityConditions.propertyCondition(getColumnIdentifier(), getConditionType(), isCaseSensitive(), getUpperBound());
  }

  private static String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : (Collection) object) {
        stringBuilder.append(toString(obj));
      }
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }
}
