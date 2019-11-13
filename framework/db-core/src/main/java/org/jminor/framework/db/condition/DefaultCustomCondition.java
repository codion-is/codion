/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Domain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition implements Condition.CustomCondition {

  private static final long serialVersionUID = 1;

  private String conditionId;
  private ArrayList<String> propertyIds;
  private ArrayList values;

  DefaultCustomCondition(final String conditionId, final List<String> propertyIds, final List values) {
    this.conditionId = requireNonNull(conditionId, "conditionId");
    this.propertyIds = new ArrayList<>(requireNonNull(propertyIds, "propertyIds"));
    this.values = new ArrayList(requireNonNull(values, "values"));
  }

  /** {@inheritDoc} */
  @Override
  public String getConditionId() {
    return conditionId;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getPropertyIds() {
    return propertyIds;
  }

  /** {@inheritDoc} */
  @Override
  public List getValues() {
    return values;
  }

  /** {@inheritDoc} */
  @Override
  public String getConditionString(final Domain domain, final String entityId) {
    return domain.getDefinition(entityId).getConditionProvider(getConditionId()).getConditionString(getValues());
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(conditionId);
    stream.writeObject(propertyIds);
    stream.writeObject(values);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    conditionId = (String) stream.readObject();
    propertyIds = (ArrayList) stream.readObject();
    values = (ArrayList) stream.readObject();
  }
}
