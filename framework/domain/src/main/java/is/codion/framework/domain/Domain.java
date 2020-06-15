/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.Report;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportType;
import is.codion.framework.domain.entity.DefaultEntities;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Represents an application domain model, entities, reports and database operations.
 * Override to define a domain model.
 * @see #define(EntityType, Property.Builder[])
 * @see #defineReport(ReportType, Report)
 * @see #defineProcedure(ProcedureType, DatabaseProcedure)
 * @see #defineFunction(FunctionType, DatabaseFunction)
 */
public abstract class Domain implements EntityDefinition.Provider {

  private final DomainType domainType;
  private final DomainEntities entities;
  private final DomainReports reports = new DomainReports();
  private final DomainProcedures procedures = new DomainProcedures();
  private final DomainFunctions functions = new DomainFunctions();

  /**
   * Instantiates a new Domain identified by the given {@link DomainType}.
   * @param domainType the Domain model type to associate with this domain model
   */
  protected Domain(final DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    this.entities = new DomainEntities(domainType);
  }

  /**
   * @return the domain type
   */
  public final DomainType getDomainType() {
    return domainType;
  }

  /**
   * @return the Domain entities
   */
  public final Entities getEntities() {
    return entities;
  }

  @Override
  public final EntityDefinition getDefinition(final EntityType<? extends Entity> entityType) {
    return entities.getDefinition(entityType);
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return entities.getDefinitions();
  }

  public final <T, R, P> Report<T, R, P> getReport(final ReportType<T, R, P> reportType) throws ReportException {
    final Report<T, R, P> report = reports.getReport(reportType);
    if (report == null) {
      throw new ReportException("Undefined report: " + reportType);
    }

    return report;
  }

  /**
   * Retrieves the procedure of the given type.
   * @param <C> the type of the database connection this procedure requires
   * @param <T> the argument type
   * @param procedureType the procedure type
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C, T> DatabaseProcedure<C, T> getProcedure(final ProcedureType<C, T> procedureType) {
    return procedures.getProcedure(procedureType);
  }

  /**
   * Retrieves the function of the given type.
   * @param <C> the type of the database connection this function requires
   * @param <T> the argument type
   * @param <R> the result type
   * @param functionType the function type
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C, T, R> DatabaseFunction<C, T, R> getFunction(final FunctionType<C, T, R> functionType) {
    return functions.getFunction(functionType);
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model, using the {@code entityType} as table name.
   * Returns the {@link EntityDefinition} instance for further configuration.
   * @param entityType the id uniquely identifying the entity type
   * @param propertyBuilders the {@link Property.Builder} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityType has already been used to define an entity type or if
   * no primary key property is specified
   */
  protected final EntityDefinition.Builder define(final EntityType<? extends Entity> entityType, final Property.Builder<?>... propertyBuilders) {
    return define(entityType, entityType.getName(), propertyBuilders);
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model.
   * Returns a {@link EntityDefinition.Builder} instance for further configuration.
   * @param entityType the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param propertyBuilders the {@link Property.Builder} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityType has already been used to define an entity type
   */
  protected final EntityDefinition.Builder define(final EntityType<? extends Entity> entityType, final String tableName,
                                                  final Property.Builder<?>... propertyBuilders) {
    if (!domainType.getName().equals(entityType.getDomainName())) {
      throw new IllegalArgumentException("Entity type '" + entityType + "' is not part of domain: " + domainType);
    }
    return entities.defineInternal(entityType, tableName, propertyBuilders);
  }

  /**
   * Adds a report to this domain model.
   * @param reportType the report to add
   * @param report the actual report to associate with the report type
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @throws RuntimeException in case loading the report failed
   * @throws IllegalArgumentException in case the report has already been added
   */
  protected final <T, R, P> void defineReport(final ReportType<T, R, P> reportType, final Report<T, R, P> report) {
    reports.addReport(reportType, report);
  }

  /**
   * Adds the given procedure to this domain
   * @param type the procedure type to identify the procedure
   * @param procedure the procedure to add
   * @param <C> the connection type
   * @param <T> the argument type
   * @throws IllegalArgumentException in case a procedure has already been associated with the given type
   */
  protected final <C, T> void defineProcedure(final ProcedureType<C, T> type, final DatabaseProcedure<C, T> procedure) {
    procedures.addProcedure(type, procedure);
  }

  /**
   * Adds the given function to this domain
   * @param type the function type to identify the function
   * @param function the function to add
   * @param <C> the connection type
   * @param <T> the argument type
   * @param <R> the result type
   * @throws IllegalArgumentException in case a function has already been associated with the given type
   */
  protected final <C, T, R> void defineFunction(final FunctionType<C, T, R> type, final DatabaseFunction<C, T, R> function) {
    functions.addFunction(type, function);
  }

  /**
   * Specifies whether it should be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in cases where entities have circular references.
   * @param strictForeignKeys true for strict foreign key validation
   */
  protected final void setStrictForeignKeys(final boolean strictForeignKeys) {
    entities.setStrictForeignKeysInternal(strictForeignKeys);
  }

  private static final class DomainEntities extends DefaultEntities {

    private static final long serialVersionUID = 1;

    private DomainEntities(final DomainType domainType) {
      super(domainType);
    }

    protected EntityDefinition.Builder defineInternal(final EntityType<? extends Entity> entityType, final String tableName,
                                                      final Property.Builder<?>... propertyBuilders) {
      return super.define(entityType, tableName, propertyBuilders);
    }

    private void setStrictForeignKeysInternal(final boolean strictForeignKeys) {
      super.setStrictForeignKeys(strictForeignKeys);
    }
  }

  private static final class DomainProcedures {

    private final Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures = new HashMap<>();

    private void addProcedure(final ProcedureType<?, ?> type, final DatabaseProcedure<?, ?> procedure) {
      requireNonNull(procedure, "procedure");
      if (procedures.containsKey(type)) {
        throw new IllegalArgumentException("Procedure already defined: " + type);
      }

      procedures.put(type, procedure);
    }

    private <C, T> DatabaseProcedure<C, T> getProcedure(final ProcedureType<C, T> procedureType) {
      requireNonNull(procedureType, "procedureType");
      final DatabaseProcedure<C, T> operation = (DatabaseProcedure<C, T>) procedures.get(procedureType);
      if (operation == null) {
        throw new IllegalArgumentException("Procedure not found: " + procedureType);
      }

      return operation;
    }
  }

  private static final class DomainFunctions {

    private final Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions = new HashMap<>();

    private void addFunction(final FunctionType<?, ?, ?> type, final DatabaseFunction<?, ?, ?> function) {
      requireNonNull(function, "function");
      if (functions.containsKey(type)) {
        throw new IllegalArgumentException("Function already defined: " + type);
      }

      functions.put(type, function);
    }

    private <C, T, R> DatabaseFunction<C, T, R> getFunction(final FunctionType<C, T, R> functionType) {
      requireNonNull(functionType, "functionType");
      final DatabaseFunction<C, T, R> operation = (DatabaseFunction<C, T, R>) functions.get(functionType);
      if (operation == null) {
        throw new IllegalArgumentException("Function not found: " + functionType);
      }

      return operation;
    }
  }

  private static final class DomainReports {

    private final Map<ReportType<?, ?, ?>, Report<?, ?, ?>> reports = new HashMap<>();

    private <T, R, P> void addReport(final ReportType<T, R, P> reportType, final Report<T, R, P> report) {
      requireNonNull(reportType, "report");
      requireNonNull(report, "report");
      if (reports.containsKey(reportType)) {
        throw new IllegalArgumentException("Report has already been added: " + reportType);
      }
      try {
        report.loadReport();
        reports.put(reportType, report);
      }
      catch (final ReportException e) {
        throw new RuntimeException(e);
      }
    }

    private <T, R, P> Report<T, R, P> getReport(final ReportType<T, R, P> reportType) {
      return (Report<T, R, P>) reports.get(requireNonNull(reportType, "report"));
    }
  }
}
