/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.DefaultEntities;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link Domain} implementation. Extend to define a domain model.
 * @see #define(EntityType, Property.Builder[])
 * @see #defineReport(ReportType, Report)
 * @see #defineProcedure(ProcedureType, DatabaseProcedure)
 * @see #defineFunction(FunctionType, DatabaseFunction)
 */
public abstract class DefaultDomain implements Domain {

  private final DomainType domainType;
  private final DomainEntities entities;
  private final DomainReports reports = new DomainReports();
  private final DomainProcedures procedures = new DomainProcedures();
  private final DomainFunctions functions = new DomainFunctions();

  /**
   * Instantiates a new DefaultDomain identified by the given {@link DomainType}.
   * @param domainType the Domain model type to associate with this domain model
   */
  protected DefaultDomain(DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    this.entities = new DomainEntities(domainType);
  }

  @Override
  public final DomainType getDomainType() {
    return domainType;
  }

  @Override
  public final Entities getEntities() {
    return entities;
  }

  @Override
  public final Map<ReportType<?, ?, ?>, Report<?, ?, ?>> getReports() {
    return unmodifiableMap(reports.reports);
  }

  @Override
  public final Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> getProcedures() {
    return unmodifiableMap(procedures.procedures);
  }

  @Override
  public final Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> getFunctions() {
    return unmodifiableMap(functions.functions);
  }

  @Override
  public final <T, R, P> Report<T, R, P> getReport(ReportType<T, R, P> reportType) {
    Report<T, R, P> report = reports.getReport(reportType);
    if (report == null) {
      throw new IllegalArgumentException("Undefined report: " + reportType);
    }

    return report;
  }

  @Override
  public final <C, T> DatabaseProcedure<C, T> getProcedure(ProcedureType<C, T> procedureType) {
    return procedures.getProcedure(procedureType);
  }

  @Override
  public final <C, T, R> DatabaseFunction<C, T, R> getFunction(FunctionType<C, T, R> functionType) {
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
  protected final EntityDefinition.Builder define(EntityType entityType, Property.Builder<?, ?>... propertyBuilders) {
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
   * @throws IllegalArgumentException in case no properties are specified
   */
  protected final EntityDefinition.Builder define(EntityType entityType, String tableName,
                                                  Property.Builder<?, ?>... propertyBuilders) {
    requireNonNull(entityType, "entityType");
    if (!domainType.contains(entityType)) {
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
  protected final <T, R, P> void defineReport(ReportType<T, R, P> reportType, Report<T, R, P> report) {
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
  protected final <C, T> void defineProcedure(ProcedureType<C, T> type, DatabaseProcedure<C, T> procedure) {
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
  protected final <C, T, R> void defineFunction(FunctionType<C, T, R> type, DatabaseFunction<C, T, R> function) {
    functions.addFunction(type, function);
  }

  /**
   * Specifies whether it should be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in cases where entities have circular references.
   * @param strictForeignKeys true for strict foreign key validation
   */
  protected final void setStrictForeignKeys(boolean strictForeignKeys) {
    entities.setStrictForeignKeysInternal(strictForeignKeys);
  }

  /**
   * Adds all entities, procedures, functions and reports from the given domain model.
   * @param domain the domain model to copy from
   * @see #addEntities(Domain)
   * @see #addProcedures(Domain)
   * @see #addFunctions(Domain)
   * @see #addReports(Domain)
   */
  protected final void addAll(Domain domain) {
    addEntities(domain);
    addProcedures(domain);
    addFunctions(domain);
    addReports(domain);
  }

  /**
   * Adds all the entities from the given domain to this domain.
   * Note that the entity type names must be unique.
   * @param domain the domain model which entities to add
   * @throws IllegalArgumentException in case a non-unique entity type name is encountered
   * @see EntityType#getName()
   */
  protected final void addEntities(Domain domain) {
    requireNonNull(domain).getEntities().getDefinitions().forEach(definition -> {
      if (!entities.contains(definition.getEntityType())) {
        entities.addDefinitionInternal(definition);
      }
    });
  }

  /**
   * Adds all the procedures from the given domain to this domain.
   * @param domain the domain model which procedures to add
   */
  protected final void addProcedures(Domain domain) {
    requireNonNull(domain).getProcedures().forEach((procedureType, procedure) -> {
      if (!procedures.procedures.containsKey(procedureType)) {
        procedures.procedures.put(procedureType, procedure);
      }
    });
  }

  /**
   * Adds all the functions from the given domain to this domain.
   * @param domain the domain model which functions to add
   */
  protected final void addFunctions(Domain domain) {
    requireNonNull(domain).getFunctions().forEach((functionType, function) -> {
      if (!functions.functions.containsKey(functionType)) {
        functions.functions.put(functionType, function);
      }
    });
  }

  /**
   * Adds all the reports from the given domain to this domain.
   * @param domain the domain model which reports to add
   */
  protected final void addReports(Domain domain) {
    requireNonNull(domain).getReports().forEach((reportType, report) -> {
      if (!reports.reports.containsKey(reportType)) {
        reports.reports.put(reportType, report);
      }
    });
  }

  private static final class DomainEntities extends DefaultEntities {

    private static final long serialVersionUID = 1;

    private DomainEntities(DomainType domainType) {
      super(domainType);
    }

    private EntityDefinition.Builder defineInternal(EntityType entityType, String tableName,
                                                    Property.Builder<?, ?>... propertyBuilders) {
      return super.define(entityType, tableName, propertyBuilders);
    }

    private void addDefinitionInternal(EntityDefinition definition) {
      super.addDefinition(definition);
    }

    private void setStrictForeignKeysInternal(boolean strictForeignKeys) {
      super.setStrictForeignKeys(strictForeignKeys);
    }
  }

  private static final class DomainProcedures {

    private final Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures = new HashMap<>();

    private void addProcedure(ProcedureType<?, ?> type, DatabaseProcedure<?, ?> procedure) {
      requireNonNull(procedure, "procedure");
      if (procedures.containsKey(type)) {
        throw new IllegalArgumentException("Procedure already defined: " + type);
      }

      procedures.put(type, procedure);
    }

    private <C, T> DatabaseProcedure<C, T> getProcedure(ProcedureType<C, T> procedureType) {
      requireNonNull(procedureType, "procedureType");
      DatabaseProcedure<C, T> operation = (DatabaseProcedure<C, T>) procedures.get(procedureType);
      if (operation == null) {
        throw new IllegalArgumentException("Procedure not found: " + procedureType);
      }

      return operation;
    }
  }

  private static final class DomainFunctions {

    private final Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions = new HashMap<>();

    private void addFunction(FunctionType<?, ?, ?> type, DatabaseFunction<?, ?, ?> function) {
      requireNonNull(function, "function");
      if (functions.containsKey(type)) {
        throw new IllegalArgumentException("Function already defined: " + type);
      }

      functions.put(type, function);
    }

    private <C, T, R> DatabaseFunction<C, T, R> getFunction(FunctionType<C, T, R> functionType) {
      requireNonNull(functionType, "functionType");
      DatabaseFunction<C, T, R> operation = (DatabaseFunction<C, T, R>) functions.get(functionType);
      if (operation == null) {
        throw new IllegalArgumentException("Function not found: " + functionType);
      }

      return operation;
    }
  }

  private static final class DomainReports {

    private static final String REPORT = "report";

    private final Map<ReportType<?, ?, ?>, Report<?, ?, ?>> reports = new HashMap<>();

    private <T, R, P> void addReport(ReportType<T, R, P> reportType, Report<T, R, P> report) {
      requireNonNull(reportType, "reportType");
      requireNonNull(report, REPORT);
      if (reports.containsKey(reportType)) {
        throw new IllegalArgumentException("Report has already been defined: " + reportType);
      }
      try {
        report.loadReport();
        reports.put(reportType, report);
      }
      catch (ReportException e) {
        throw new RuntimeException(e);
      }
    }

    private <T, R, P> Report<T, R, P> getReport(ReportType<T, R, P> reportType) {
      return (Report<T, R, P>) reports.get(requireNonNull(reportType, REPORT));
    }
  }
}
