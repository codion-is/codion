/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;

import java.util.List;

/**
 * Encapsulates a simple free-form query criteria.
 */
public class SimpleCriteria implements Criteria {

  private final String criteriaString;
  private List<Object> values;
  private List<Integer> types;

  public SimpleCriteria(final String criteriaString) {
    this(criteriaString, null, null);
  }

  public SimpleCriteria(final String criteriaString, final List<Object> values, final List<Integer> types) {
    this.criteriaString = criteriaString;
    this.values = values;
    this.types = types;
  }

  public String asString(final Database database, final ValueProvider valueProvider) {
    return criteriaString;
  }

  public List<?> getValues() {
    return values;
  }

  public List<Integer> getTypes() {
    return types;
  }
}
