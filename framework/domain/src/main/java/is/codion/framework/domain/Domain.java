/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static java.util.Collections.unmodifiableList;

/**
 * Represents an application domain model, entities, reports and database operations.
 */
public interface Domain {

  /**
   * @return the domain type identifying this domain model
   */
  DomainType type();

  /**
   * @return the Domain entities
   */
  Entities entities();

  /**
   * @return an unmodifiable view of this domain's reports
   */
  Map<ReportType<?, ?, ?>, Report<?, ?, ?>> reports();

  /**
   * @return an unmodifiable view of this domain's procedures
   */
  Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures();

  /**
   * @return an unmodifiable view of this domain's functions
   */
  Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions();

  /**
   * Retrieves the report of the given type.
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @param reportType the report type
   * @return the report
   * @throws IllegalArgumentException in case the report is not found
   */
  <T, R, P> Report<T, R, P> report(ReportType<T, R, P> reportType);

  /**
   * Retrieves the procedure of the given type.
   * @param <C> the type of the database connection this procedure requires
   * @param <T> the argument type
   * @param procedureType the procedure type
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  <C, T> DatabaseProcedure<C, T> procedure(ProcedureType<C, T> procedureType);

  /**
   * Retrieves the function of the given type.
   * @param <C> the type of the database connection this function requires
   * @param <T> the argument type
   * @param <R> the result type
   * @param functionType the function type
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  <C, T, R> DatabaseFunction<C, T, R> function(FunctionType<C, T, R> functionType);

  /**
   * Configures a database connection for applications using this domain model, for example adding extensions or properties.
   * Called each time a new connection based on this domain is created.
   * @param connection the connection to configure
   * @throws DatabaseException in case of an exception
   */
  default void configureConnection(DatabaseConnection connection) throws DatabaseException {}

  /**
   * Configures a database, for example run migration scripts. Only called once per database instance.
   * @param database the database
   * @throws DatabaseException in case of an exception
   */
  default void configureDatabase(Database database) throws DatabaseException {}

  /**
   * @return a list containing all the Domains registered with {@link ServiceLoader}.
   */
  static List<Domain> domains() {
    List<Domain> domains = new ArrayList<>();
    ServiceLoader<Domain> loader = ServiceLoader.load(Domain.class);
    for (Domain domain : loader) {
      domains.add(domain);
    }

    return unmodifiableList(domains);
  }
}
