/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.User;

import java.sql.SQLException;
import java.util.List;


/**
 * Specifies a database connection, providing basic transaction control and helper functions for querying and manipulating data.
 */
public interface DatabaseConnection extends PoolableConnection {

  /**
   * @return the connection user
   */
  User getUser();

  /**
   * @return the database implementation this connection is based on
   */
  Database getDatabase();

  /**
   * @param enabled true to enable logging on this connection, false to disable
   */
  void setLoggingEnabled(final boolean enabled);

  /**
   * @return true if logging is enabled, false otherwise
   */
  boolean isLoggingEnabled();

  /**
   * Performs the given sql query and returns the result in a List
   * @param sql the query
   * @param resultPacker a ResultPacker instance for creating the return List
   * @param fetchCount the number of records to retrieve, use -1 to retrieve all
   * @return the query result in a List
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException;

  /**
   * @return true if the connection is connected
   */
  boolean isConnected();

  /**
   * @param sql the query
   * @param fetchCount the maximum number of records to return, -1 for all
   * @return the result of this query, in a List of rows represented as Lists
   * @throws SQLException thrown if anything goes wrong during the query execution
   */
  List<List> queryObjects(final String sql, final int fetchCount) throws SQLException;

  /**
   * Performs the given query and returns the result as an integer
   * @param sql the query must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws org.jminor.common.db.exception.DatabaseException thrown if no record is found
   */
  int queryInteger(final String sql) throws SQLException, DatabaseException;

  /**
   * Performs the given query and returns the result as a List of Integers
   * @param sql the query, it must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return a List of Integers representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  List<Integer> queryIntegers(final String sql) throws SQLException;

  /**
   * Performs the given query and returns the result as a List of Strings
   * @param sql the query, it must select at least a single string column, any other
   * subsequent columns are disregarded
   * @return a List of Strings representing the query result
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  List<String> queryStrings(final String sql) throws SQLException;

  /**
   * Returns the contents of the given blob field.
   * @param tableName the table name
   * @param columnName the name of the blob column
   * @param whereClause the where clause
   * @return the blob contents
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  byte[] readBlobField(final String tableName, final String columnName, final String whereClause) throws SQLException;

  /**
   * Writes the given blob data into the given column.
   * @param blobData the blob data
   * @param tableName the table name
   * @param columnName the blob column name
   * @param whereClause the where clause
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  void writeBlobField(final byte[] blobData, final String tableName, final String columnName,
                      final String whereClause) throws SQLException;

  /**
   * Executes the given statement, which can be anything except a select query.
   * @param sql the statement to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  void execute(final String sql) throws SQLException;

  /**
   * Executes the statement.
   * @param sqlStatement the statement to execute
   * @param outParameterType the type of the out parameter, -1 if no out parameter, java.sql.Types.*
   * @return the out parameter, null if none is specified
   * @throws SQLException thrown if anything goes wrong during execution
   */
  Object executeCallableStatement(final String sqlStatement, final int outParameterType) throws SQLException;

  /**
   * Executes the given statements, in a batch if possible, which can be anything except a select query.
   * @param statements the statements to execute
   * @throws SQLException thrown if anything goes wrong during execution
   */
  void execute(final List<String> statements) throws SQLException;

  /**
   * Executes the function with the given id
   * @param functionID the function ID
   * @param arguments the arguments, if any
   * @return the procedure return arguments, if any
   * @throws org.jminor.common.db.exception.DatabaseException in case anyhing goes wrong during the execution
   */
  List<?> executeFunction(final String functionID, final List<?> arguments) throws DatabaseException;

  /**
   * Executes the procedure with the given id
   * @param procedureID the procedure ID
   * @param arguments the arguments, if any
   * @throws org.jminor.common.db.exception.DatabaseException in case anyhing goes wrong during the execution
   */
  void executeProcedure(final String procedureID, final List<?> arguments) throws DatabaseException;

  /**
   * @return the log entries
   */
  List<LogEntry> getLogEntries();

  /**
   * A database operation
   */
  interface Operation {

    /**
     * @return this operation's unique ID
     */
    String getID();

    /**
     * @return the name of this operation
     */
    String getName();
  }

  /**
   * A database procedure
   */
  interface Procedure extends Operation {

    /**
     * Executes this procedure with the given connection
     * @param connection the db connection to use when executing
     * @param arguments the procedure arguments, if any
     * @throws org.jminor.common.db.exception.DatabaseException in case of an exception during the execution
     */
    void execute(final DatabaseConnection connection, final List<?> arguments) throws DatabaseException;
  }

  /**
   * A database function
   */
  interface Function extends Operation {

    /**
     * Executes this function with the given connection
     * @param connection the db connection to use when executing
     * @param arguments the function arguments, if any
     * @return the function return arguments
     * @throws org.jminor.common.db.exception.DatabaseException in case of an exception during the execution
     */
    List<Object> execute(final DatabaseConnection connection, final List<?> arguments) throws DatabaseException;
  }
}