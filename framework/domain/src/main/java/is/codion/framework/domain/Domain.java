/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.framework.domain.entity.DefaultEntities;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Represents an application domain model, entities, reports and database operations.
 * Override to define a domain model.
 * @see #define(EntityType, Property.Builder[])
 * @see #addReport(ReportWrapper)
 * @see #defineProcedure(ProcedureType, DatabaseProcedure)
 * @see #defineFunction(FunctionType, DatabaseFunction)
 */
public abstract class Domain implements EntityDefinition.Provider {

  private final String domainName;
  private final DomainEntities entities;
  private final DomainReports reports = new DomainReports();
  private final DomainProcedures procedures = new DomainProcedures();
  private final DomainFunctions functions = new DomainFunctions();

  /**
   * Instantiates a new Domain with the simple name of the class as domain name
   * @see Class#getSimpleName()
   */
  protected Domain() {
    this.domainName = getClass().getSimpleName();
    this.entities = new DomainEntities(domainName);
  }

  /**
   * Instantiates a new Domain
   * @param domainType the domain type
   */
  protected Domain(final String domainName) {
    this.domainName = requireNonNull(domainName, "domainName");
    this.entities = new DomainEntities(domainName);
  }

  /**
   * @return the domain name
   */
  public final String getDomainName() {
    return domainName;
  }

  /**
   * @return the Domain entities
   */
  public final Entities getEntities() {
    return entities;
  }

  @Override
  public final EntityDefinition getDefinition(final EntityType entityType) {
    return entities.getDefinition(entityType);
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return entities.getDefinitions();
  }

  public final boolean containsReport(final ReportWrapper<?, ?, ?> reportWrapper) {
    return reports.containsReport(reportWrapper);
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
  protected final EntityDefinition.Builder define(final EntityType entityType, final Property.Builder<?>... propertyBuilders) {
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
  protected final EntityDefinition.Builder define(final EntityType entityType, final String tableName,
                                                  final Property.Builder<?>... propertyBuilders) {
    return entities.defineInternal(entityType, tableName, propertyBuilders);
  }

  /**
   * Adds a report to this domain model.
   * @param reportWrapper the report to add
   * @throws RuntimeException in case loading the report failed
   * @throws IllegalArgumentException in case the report has already been added
   */
  protected final void addReport(final ReportWrapper<?, ?, ?> reportWrapper) {
    reports.addReport(reportWrapper);
  }

  /**
   * Adds the given procedure to this domain
   * @param type the procedure type to identify the procedure
   * @param procedure the procedure to add
   * @param <C> the connection type
   * @param <T> the argument type
   * @throws IllegalArgumentException in case an procedure with the same id has already been added
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
   * @throws IllegalArgumentException in case an function with the same id has already been added
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

    private DomainEntities(final String domainName) {
      super(domainName);
    }

    protected EntityDefinition.Builder defineInternal(final EntityType entityType, final String tableName,
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

    private final Set<ReportWrapper<?, ?, ?>> reports = new HashSet<>();

    private void addReport(final ReportWrapper<?, ?, ?> report) {
      if (containsReport(report)) {
        throw new IllegalArgumentException("Report has already been added: " + report);
      }
      try {
        report.loadReport();
        reports.add(report);
      }
      catch (final ReportException e) {
        throw new RuntimeException(e);
      }
    }

    private boolean containsReport(final ReportWrapper<?, ?, ?> reportWrapper) {
      return reports.contains(requireNonNull(reportWrapper, "reportWrapper"));
    }
  }
}
