/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A object for encapsulating a query condition based on a single property with one or more values.
 */
final class DefaultPropertyCondition implements Condition.PropertyCondition {

  private static final long serialVersionUID = 1;

  /**
   * The property used in this condition
   */
  private String propertyId;

  /**
   * The values used in this condition
   */
  private List values;

  /**
   * True if this condition tests for null
   */
  private boolean isNullCondition;

  /**
   * The search type used in this condition
   */
  private ConditionType conditionType;

  /**
   * True if this condition should be case sensitive, only applies to condition based on string properties
   */
  private boolean caseSensitive;

  /**
   * Instantiates a new PropertyCondition instance
   * @param propertyId the id of the property
   * @param conditionType the condition type
   * @param value the value, can be a Collection
   */
  DefaultPropertyCondition(final String propertyId, final ConditionType conditionType, final Object value,
                           final boolean caseSensitive) {
    requireNonNull(propertyId, "propertyId");
    requireNonNull(conditionType, "conditionType");
    this.propertyId = propertyId;
    this.conditionType = conditionType;
    this.isNullCondition = value == null;
    this.caseSensitive = caseSensitive;
    this.values = initializeValues(value);
    if (this.values.isEmpty()) {
      throw new IllegalArgumentException("No values specified for PropertyCondition: " + propertyId);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List getValues() {
    if (isNullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return values;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getPropertyIds() {
    if (isNullCondition) {
      return emptyList();
    }//null condition, uses 'x is null', not 'x = ?'

    return Collections.nCopies(values.size(), propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public String getPropertyId() {
    return propertyId;
  }

  /** {@inheritDoc} */
  @Override
  public ConditionType getConditionType() {
    return conditionType;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNullCondition() {
    return isNullCondition;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(propertyId);
    stream.writeObject(conditionType);
    stream.writeBoolean(isNullCondition);
    stream.writeBoolean(caseSensitive);
    stream.writeObject(values);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    propertyId = (String) stream.readObject();
    conditionType = (ConditionType) stream.readObject();
    isNullCondition = stream.readBoolean();
    caseSensitive = stream.readBoolean();
    values = (List) stream.readObject();
  }

  private static List initializeValues(final Object value) {
    final List values = new ArrayList();
    if (value instanceof Collection) {
      values.addAll((Collection) value);
    }
    else {
      values.add(value);
    }
    //replace Entity with Entity.Key
    for (int i = 0; i < values.size(); i++) {
      final Object val = values.get(i);
      if (val instanceof Entity) {
        values.set(i, ((Entity) val).getKey());
      }
      else {//assume it's all or nothing
        break;
      }
    }

    return values;
  }
}
