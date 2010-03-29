/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;

public class SimpleCriteria implements Criteria {

  private final String criteriaString;

  public SimpleCriteria(final String criteriaString) {
    this.criteriaString = criteriaString;
  }

  public String asString(final Database database) {
    return criteriaString;
  }
}
