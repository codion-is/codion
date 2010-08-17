/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class encapsulating a set of Criteria objects, that should be either AND'ed
 * or OR'ed together in a query context
 */
public class CriteriaSet<T> implements Criteria<T>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The conjunction used by this CriteriaSet
   */
  private final Conjunction conjunction;

  /**
   * The criteria in this set
   */
  private final List<Criteria<T>> criteriaList = new ArrayList<Criteria<T>>();

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   */
  public CriteriaSet(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the conjunction to use
   * @param criteria the Criteria objects to be included in this set
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T>... criteria) {
    this.conjunction = conjunction;
    for (final Criteria<T> criterion : criteria) {
      add(criterion);
    }
  }

  /**
   * Adds a new Criteria object to this set
   * @param criteria the Criteria to add
   */
  public final void add(final Criteria<T> criteria) {
    if (criteria != null) {
      this.criteriaList.add(criteria);
    }
  }

  /**
   * @return the number of criteria in this set
   */
  public final int getCriteriaCount() {
    return criteriaList.size();
  }

  /** {@inheritDoc} */
  public final String asString() {
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
  public final List<Object> getValues() {
    final List<Object> values = new ArrayList<Object>();
    for (final Criteria<T> criteria : criteriaList) {
      values.addAll(criteria.getValues());
    }

    return values;
  }

  /** {@inheritDoc} */
  public final List<T> getValueKeys() {
    final List<T> types = new ArrayList<T>();
    for (final Criteria<T> criteria : criteriaList) {
      types.addAll(criteria.getValueKeys());
    }

    return types;
  }
}
