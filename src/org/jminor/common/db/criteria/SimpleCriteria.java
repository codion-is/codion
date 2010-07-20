/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.util.List;

/**
 * Encapsulates a simple free-form query criteria.
 */
public final class SimpleCriteria<T> implements Criteria<T> {

  private final String criteriaString;
  private List<Object> values;
  private List<T> keys;

  public SimpleCriteria(final String criteriaString) {
    this(criteriaString, null, null);
  }

  public SimpleCriteria(final String criteriaString, final List<Object> values, final List<T> keys) {
    this.criteriaString = criteriaString;
    this.values = values;
    this.keys = keys;
  }

  public String asString() {
    return criteriaString;
  }

  public List<Object> getValues() {
    return values;
  }

  public List<T> getValueKeys() {
    return keys;
  }
}
