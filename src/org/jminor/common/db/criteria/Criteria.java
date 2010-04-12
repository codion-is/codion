/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;

/**
 * A generic interface for objects serving as where conditions in database queries
 */
public interface Criteria {
  /**
   * @return a SQL where condition string without the 'where' keyword  @param database the Database instance
   * @param database the underlying Database
   * @param valueProvider responsible for providing the actual sql string values
   */
  String asString(final Database database, final CriteriaValueProvider valueProvider);
}
