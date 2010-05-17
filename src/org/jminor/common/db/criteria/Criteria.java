/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 */
public interface Criteria {
  /**
   * @return a SQL where condition string without the 'where' keyword  @param database the Database instance
   * @param database the underlying Database
   * @param valueProvider responsible for providing the actual sql string values
   */
  String asString(final Database database, final ValueProvider valueProvider);

  /**
   * @return a list of the values this criteria is based on
   */
  List<?> getValues();

  /**
   * @return the types of the values this criteria is based on, if any
   */
  List<Integer> getTypes();

  /**
   * An interface describing an object responsible for returning sql string representations
   * of column values.
   */
  public static interface ValueProvider {

    /**
     * Return a SQL string representation of <code>value</code>
     * @param database the underlying Database
     * @param columnKey an object identifying the column which value should be returned
     * @param value the value
     * @return a SQL String representation of <code>value</code>
     */
    String getSQLString(final Database database, final Object columnKey, final Object value);
  }
}
