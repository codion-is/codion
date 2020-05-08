/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.operation.DatabaseFunction;
import org.jminor.common.db.operation.DatabaseOperation;
import org.jminor.common.db.operation.DatabaseProcedure;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DomainOperations {

  private final Map<String, DatabaseOperation> operations = new HashMap<>();

  DomainOperations() {}

  /**
   * Retrieves the procedure with the given id.
   * @param <C> the type of the database connection this procedure requires
   * @param procedureId the procedure id
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  <C> DatabaseProcedure<C> getProcedure(final String procedureId) {
    requireNonNull(procedureId, "procedureId");
    final DatabaseOperation operation = operations.get(procedureId);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureId);
    }

    return (DatabaseProcedure<C>) operation;
  }

  /**
   * Retrieves the function with the given id.
   * @param <C> the type of the database connection this function requires
   * @param <T> the result type
   * @param functionId the function id
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  <C, T> DatabaseFunction<C, T> getFunction(final String functionId) {
    requireNonNull(functionId, "functionId");
    final DatabaseOperation operation = operations.get(functionId);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionId);
    }

    return (DatabaseFunction<C, T>) operation;
  }

  Map<String, DatabaseOperation> getOperations() {
    return unmodifiableMap(operations);
  }

  void addOperation(final DatabaseOperation operation) {
    requireNonNull(operation, "operation");
    if (operations.containsKey(operation.getId())) {
      throw new IllegalArgumentException("Operation already defined: " + operations.get(operation.getId()).getName());
    }

    operations.put(operation.getId(), operation);
  }

  void putAll(final DomainOperations operations) {
    requireNonNull(operations, "operations");
    this.operations.putAll(operations.getOperations());
  }
}
