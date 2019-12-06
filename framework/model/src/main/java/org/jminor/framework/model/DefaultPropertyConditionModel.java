/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.jminor.framework.db.condition.Conditions.propertyCondition;

/**
 * A class for searching a set of entities based on a property.
 */
public final class DefaultPropertyConditionModel extends DefaultColumnConditionModel<ColumnProperty>
        implements PropertyConditionModel<ColumnProperty> {

  /**
   * Constructs a DefaultPropertyConditionModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultPropertyConditionModel(final ColumnProperty property) {
    super(property, property.getTypeClass(), Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimeFormatPattern());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getColumnIdentifier().getPropertyId());
    if (isEnabled()) {
      stringBuilder.append(getConditionType());
      stringBuilder.append(toString(getUpperBound()));
      stringBuilder.append(toString(getLowerBound()));
    }

    return stringBuilder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public Condition getCondition() {
    final Object conditionValue = getConditionType().getValues().equals(ConditionType.Values.TWO) ?
            asList(getLowerBound(), getUpperBound()) : getUpperBound();

    return propertyCondition(getColumnIdentifier().getPropertyId(), getConditionType(), conditionValue)
            .setCaseSensitive(isCaseSensitive());
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
