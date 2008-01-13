/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.util.ArrayList;
import java.util.List;

/**
   * A set of ICriteria objects
 */
public class CriteriaSet implements ICriteria {

  public enum Conjunction {
    AND, OR
  }

  final Conjunction conjunction;
  final List<ICriteria> criterias = new ArrayList<ICriteria>();

  public CriteriaSet(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  public CriteriaSet(final Conjunction conjunction, final ICriteria... criterias) {
    this.conjunction = conjunction;
    for (final ICriteria criteria : criterias)
      if (criteria != null)
        addCriteria(criteria);
  }

  public void addCriteria(final ICriteria criteria) {
    this.criterias.add(criteria);
  }

  /**
   * @return Value for property 'criteriaCount'.
   */
  public int getCriteriaCount() {
    return criterias.size();
  }

  /** {@inheritDoc} */
  public String toString() {
    if (criterias.size() == 0)
      return "";

    final StringBuffer ret = new StringBuffer(criterias.size() > 1 ? "(" : "");
    int i = 0;
    for (final ICriteria criteria : criterias) {
      ret.append(criteria.toString());
      if (i++ < criterias.size()-1)
        ret.append(conjunction == Conjunction.AND ? " and " : " or ");
    }

    return ret.append(criterias.size() > 1 ? ")" : "").toString();
  }
}
