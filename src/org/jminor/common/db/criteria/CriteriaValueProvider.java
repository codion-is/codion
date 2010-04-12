package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;

/**
 * An interface describing an object responsible for returning sql string representations
 * of column values.
 */
public interface CriteriaValueProvider {

  /**
   * Return a SQL string representation of <code>value</code>
   * @param database the underlying Database
   * @param columnKey an object identifying the column which value should be returned
   * @param value the value
   * @return a SQL String representation of <code>value</code>
   */
  String getSQLStringValue(final Database database, final Object columnKey, final Object value);

  /**
   * @param columnKey an object identifying the column which value should be returned
   * @param value the value
   * @return true if the given value is null
   */
  boolean isValueNull(final Object columnKey, final Object value);
}
