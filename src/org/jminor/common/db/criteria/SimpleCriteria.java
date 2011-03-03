/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Util;

import java.io.Serializable;
import java.util.Collections;
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
   * @throws IllegalArgumentException in case the criteria string is null
   */
  public SimpleCriteria(final String criteriaString) {
    this(criteriaString, Collections.<Object>emptyList(), Collections.<T>emptyList());
  }

  /**
   * Instantiates a new SimpleCriteria
   * @param criteriaString the criteria string
   * @param values the values required by this criteria string
   * @param keys the keys required by this criteria string, in the same order as their respective values
   * @throws IllegalArgumentException in case any of the parameters are null
   */
  public SimpleCriteria(final String criteriaString, final List<Object> values, final List<T> keys) {
    this.criteriaString = Util.rejectNullValue(criteriaString, "criteriaString");
    this.values = Util.rejectNullValue(values, "values");
    this.keys = Util.rejectNullValue(keys, "keys");
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
