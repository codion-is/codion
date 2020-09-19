/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.Report;
import is.codion.common.db.reports.ReportType;
import is.codion.framework.domain.entity.Entities;

/**
 * Represents an application domain model, entities, reports and database operations.
 */
public interface Domain {

  /**
   * @return the domain type
   */
  DomainType getDomainType();

  /**
   * @return the Domain entities
   */
  Entities getEntities();

  /**
   * Retrieves the report of the given type.
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @param reportType the report type
   * @return the report
   * @throws IllegalArgumentException in case the report is not found
   */
  <T, R, P> Report<T, R, P> getReport(ReportType<T, R, P> reportType);

  /**
   * Retrieves the procedure of the given type.
   * @param <C> the type of the database connection this procedure requires
   * @param <T> the argument type
   * @param procedureType the procedure type
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  <C, T> DatabaseProcedure<C, T> getProcedure(ProcedureType<C, T> procedureType);

  /**
   * Retrieves the function of the given type.
   * @param <C> the type of the database connection this function requires
   * @param <T> the argument type
   * @param <R> the result type
   * @param functionType the function type
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  <C, T, R> DatabaseFunction<C, T, R> getFunction(FunctionType<C, T, R> functionType);

  /**
   * Configures a database connection for this domain model, f.ex. adding extensions or properties.
   * @param connection the connection to configure
   */
  default void configureConnection(final DatabaseConnection connection) {};
}
