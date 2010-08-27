/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.io.Serializable;
import java.util.List;

/**
 * Encapsulates a simple free-form query criteria.
 */
public final class SimpleCriteria<T> implements Criteria<T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String criteriaString;
  private List<Object> values;
  private List<T> keys;

  /**
   * Instantiates a new SimpleCriteria
   * @param criteriaString the criteria string
   */
  public SimpleCriteria(final String criteriaString) {
    this(criteriaString, null, null);
  }

  /**
   * Instantiates a new SimpleCriteria
   * @param criteriaString the criteria string
   * @param values the values required by this criteria string
   * @param keys the keys required by this criteria string, in the same order as their respective values
   */
  public SimpleCriteria(final String criteriaString, final List<Object> values, final List<T> keys) {
    this.criteriaString = criteriaString;
    this.values = values;
    this.keys = keys;
  }

  /** {@inheritDoc} */
  public String asString() {
    return criteriaString;
  }

  /** {@inheritDoc} */
  public List<Object> getValues() {
    return values;
  }

  /** {@inheritDoc} */
  public List<T> getValueKeys() {
    return keys;
  }
}
