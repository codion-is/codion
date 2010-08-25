/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class encapsulating a set of Criteria objects, that should be either AND'ed
 * or OR'ed together in a query context
 */
public final class CriteriaSet<T> implements Criteria<T>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The conjunction used by this CriteriaSet
   */
  private Conjunction conjunction;

  /**
   * The criteria in this set
   */
  private List<Criteria<T>> criteriaList = new ArrayList<Criteria<T>>();

  /**
   * Used for serialization, not for general use
   */
  CriteriaSet() {}

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   */
  public CriteriaSet(final Conjunction conjunction) {
    this.conjunction = conjunction;
    criteriaList = new ArrayList<Criteria<T>>();
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the conjunction to use
   * @param criteria the Criteria objects to be included in this set
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T>... criteria) {
    this.conjunction = conjunction;
    criteriaList = new ArrayList<Criteria<T>>();
    for (final Criteria<T> criterion : criteria) {
      add(criterion);
    }
  }

  /**
   * Adds a new Criteria object to this set
   * @param criteria the Criteria to add
   */
  public void add(final Criteria<T> criteria) {
    if (criteria != null) {
      this.criteriaList.add(criteria);
    }
  }

  /**
   * @return the number of criteria in this set
   */
  public int getCriteriaCount() {
    return criteriaList.size();
  }

  /** {@inheritDoc} */
  public String asString() {
    if (criteriaList.isEmpty()) {
      return "";
    }

    final StringBuilder criteriaString = new StringBuilder(criteriaList.size() > 1 ? "(" : "");
    int i = 0;
    for (final Criteria criteria : criteriaList) {
      criteriaString.append(criteria.asString());
      if (i++ < criteriaList.size() - 1) {
        criteriaString.append(conjunction.toString());
      }
    }

    return criteriaString.append(criteriaList.size() > 1 ? ")" : "").toString();
  }

  /** {@inheritDoc} */
  public List<Object> getValues() {
    final List<Object> values = new ArrayList<Object>();
    for (final Criteria<T> criteria : criteriaList) {
      values.addAll(criteria.getValues());
    }

    return values;
  }

  /** {@inheritDoc} */
  public List<T> getValueKeys() {
    final List<T> types = new ArrayList<T>();
    for (final Criteria<T> criteria : criteriaList) {
      types.addAll(criteria.getValueKeys());
    }

    return types;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(conjunction);
    stream.writeObject(criteriaList);
  }

  @SuppressWarnings({"unchecked"})
  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    conjunction = (Conjunction) stream.readObject();
    criteriaList = (ArrayList<Criteria<T>>) stream.readObject();
  }
}
