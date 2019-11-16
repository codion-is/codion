/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultConditionSet implements Condition.Set {

  private static final long serialVersionUID = 1;

  private ArrayList<Condition> conditions = new ArrayList<>();
  private Conjunction conjunction;

  DefaultConditionSet(final Conjunction conjunction, final Collection<Condition> conditions) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
    for (final Condition condition : requireNonNull(conditions, "conditions")) {
      add(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void add(final Condition condition) {
    if (condition != null && !(condition instanceof EmptyCondition)) {
      conditions.add(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Condition> getConditions() {
    return conditions;
  }

  /** {@inheritDoc} */
  @Override
  public Conjunction getConjunction() {
    return conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public List getValues() {
    final List values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return values;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getPropertyIds() {
    final List<String> propertyIds = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      propertyIds.addAll(conditions.get(i).getPropertyIds());
    }

    return propertyIds;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(conjunction);
    stream.writeObject(conditions);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    conjunction = (Conjunction) stream.readObject();
    conditions = (ArrayList) stream.readObject();
  }
}
