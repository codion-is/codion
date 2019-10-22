/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultStringCondition implements Condition.StringCondition {

  private static final long serialVersionUID = 1;

  private String conditionString;
  private List values;
  private List<String> propertyIds;

  DefaultStringCondition(final String conditionString, final List values, final List<String> propertyIds) {
    this.conditionString = requireNonNull(conditionString, "conditionString");
    this.values = requireNonNull(values, "values");
    this.propertyIds = requireNonNull(propertyIds, "propertyIds");
  }

  /** {@inheritDoc} */
  @Override
  public String getConditionString() {
    return conditionString;
  }

  /** {@inheritDoc} */
  @Override
  public List getValues() {
    return values;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getPropertyIds() {
    return propertyIds;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(conditionString);
    stream.writeObject(values);
    stream.writeObject(propertyIds);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    conditionString = (String) stream.readObject();
    values = (List) stream.readObject();
    propertyIds = (List) stream.readObject();
  }
}
