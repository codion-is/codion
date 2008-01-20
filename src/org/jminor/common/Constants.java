/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.sql.Timestamp;

/**
 * Some constants
 */
public class Constants {

  /**
   * Specifies the database type
   * @see #DATABASE_TYPE_MYSQL
   * @see #DATABASE_TYPE_ORACLE
   */
  public static final String DATABASE_TYPE_PROPERTY = "jminor.db.type";

  /**
   * Specifies the machine hosting the database
   */
  public static final String DATABASE_HOST_PROPERTY = "jminor.db.host";

  /**
   * Specifies the datbase sid (used for initial for MySQL connections)
   */
  public static final String DATABASE_SID_PROPERTY = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  public static final String DATABASE_PORT_PROPERTY = "jminor.db.port";

  /**
   * Oracle database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_ORACLE = "oracle";

  /**
   * MySQL database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_MYSQL = "mysql";

  /**
   * The driver class name use for Oracle connections
   */
  public static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";

  /**
   * The driver class name used for MySQL connections
   */
  public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

  /**
   * The String used as wildcard in String filters
   */
  public static final String WILDCARD = "%";
  public static final int INT_NULL_VALUE = -Integer.MAX_VALUE;
  public static final Character CHAR_NULL_VALUE = Character.MIN_VALUE;
  public static final Double DOUBLE_NULL_VALUE = Double.NEGATIVE_INFINITY;
  public static final Character CHARACTER_NULL_VALUE = CHAR_NULL_VALUE;
  public static final Integer INTEGER_NULL_VALUE = INT_NULL_VALUE;
  public static final Timestamp TIMESTAMP_NULL_VALUE = new Timestamp(Long.MIN_VALUE);
}
