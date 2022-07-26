/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.connection.DatabaseConnection;
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
import java.util.Optional;
import java.util.ServiceLoader;

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
   * @return an unmodifiable view of this domain's reports
   */
  Map<ReportType<?, ?, ?>, Report<?, ?, ?>> getReports();

  /**
   * @return an unmodifiable view of this domain's procedures
   */
  Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> getProcedures();

  /**
   * @return an unmodifiable view of this domain's functions
   */
  Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> getFunctions();

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
   * Configures a database connection for applications using this domain model, for example adding extensions or properties.
   * @param connection the connection to configure
   * @throws DatabaseException in case of an exception
   */
  default void configureConnection(DatabaseConnection connection) throws DatabaseException {}

  /**
   * @return a list containing all the Domains registered with {@link ServiceLoader}.
   */
  static List<Domain> getDomains() {
    List<Domain> domains = new ArrayList<>();
    ServiceLoader<Domain> loader = ServiceLoader.load(Domain.class);
    for (Domain domain : loader) {
      domains.add(domain);
    }

    return domains;
  }

  /**
   * @param name the domain name
   * @return a {@link Domain} implementation with the given name, if found
   * @see DomainType#getName()
   */
  static Optional<Domain> getInstanceByName(String name) {
    for (Domain domain : getDomains()) {
      if (domain.getDomainType().getName().equals(name)) {
        return Optional.of(domain);
      }
    }

    return Optional.empty();
  }

  /**
   * @param className the domain classname
   * @return a {@link Domain} implementation of the given type, if found
   * @see DomainType#getName()
   */
  static Optional<Domain> getInstanceByClassName(String className) {
    for (Domain domain : getDomains()) {
      if (domain.getClass().getName().equals(className)) {
        return Optional.of(domain);
      }
    }

    return Optional.empty();
  }
}
