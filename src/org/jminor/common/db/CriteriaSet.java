/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class encapsulating a set of ICriteria objects, that should be either AND'ed
 * or OR'ed together in a query context
 */
public class CriteriaSet implements ICriteria, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Represents two possible conjunctions, AND and OR
   */
  public enum Conjunction {
    AND, OR;

    /** {@inheritDoc} */
    @Override
    public String toString() {
      switch (this) {
        case AND:
          return " and ";
        case OR:
          return " or ";
        default:
          throw new IllegalArgumentException("Unknown CriteriaSet.Conjunction enum");
      }
    }
  }

  /**
   * The conjunction used by this CriteriaSet
   */
  private final Conjunction conjunction;

  /**
   * The criterias in this set
   */
  private final List<ICriteria> criterias = new ArrayList<ICriteria>();

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
   * @param criterias the ICriteria objects to be included in this set
   */
  public CriteriaSet(final Conjunction conjunction, final ICriteria... criterias) {
    this.conjunction = conjunction;
    for (final ICriteria criteria : criterias)
      addCriteria(criteria);
  }

  /**
   * Adds a new ICriteria object to this set
   * @param criteria the ICriteria to add
   */
  public void addCriteria(final ICriteria criteria) {
    if (criteria != null)
      this.criterias.add(criteria);
  }

  /**
   * @return the number of criterias in this set
   */
  public int getCriteriaCount() {
    return criterias.size();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    if (criterias.size() == 0)
      return "";

    final StringBuilder ret = new StringBuilder(criterias.size() > 1 ? "(" : "");
    int i = 0;
    for (final ICriteria criteria : criterias) {
      ret.append(criteria.toString());
      if (i++ < criterias.size()-1)
        ret.append(conjunction.toString());
    }

    return ret.append(criterias.size() > 1 ? ")" : "").toString();
  }
}
