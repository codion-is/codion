/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain;

import dev.codion.common.db.operation.DatabaseFunction;
import dev.codion.common.db.operation.DatabaseOperation;
import dev.codion.common.db.operation.DatabaseProcedure;
import dev.codion.common.db.reports.ReportException;
import dev.codion.common.db.reports.ReportWrapper;
import dev.codion.framework.domain.entity.DefaultEntities;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.property.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Represents an application domain model, entities, reports and database operations.
 * Override to define a domain model.
 * @see #define(String, Property.Builder...)
 * @see #addReport(ReportWrapper)
 * @see #addOperation(DatabaseOperation)
 */
public abstract class Domain implements EntityDefinition.Provider {

  private final DomainEntities entities;
  private final DomainReports reports = new DomainReports();
  private final DomainOperations operations = new DomainOperations();

  /**
   * Instantiates a new Domain with the simple name of the class as domain id
   * @see Class#getSimpleName()
   */
  protected Domain() {
    this.entities = new DomainEntities(getClass().getSimpleName());
  }

  /**
   * Instantiates a new Domain
   * @param domainId the domain identifier
   */
  protected Domain(final String domainId) {
    this.entities = new DomainEntities(requireNonNull(domainId, "domainId"));
  }

  /**
   * @return the domainId
   */
  public final String getDomainId() {
    return entities.getDomainId();
  }

  /**
   * @return the Domain entities
   */
  public final Entities getEntities() {
    return entities;
  }

  /**
   * Registers the domain entities for serialization.
   * @return the Entities instance just registered.
   * @see Entities#register()
   */
  public final Entities registerEntities() {
    entities.register();

    return entities;
  }

  @Override
  public final EntityDefinition getDefinition(final String entityId) {
    return entities.getDefinition(entityId);
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return entities.getDefinitions();
  }

  public final boolean containsReport(final ReportWrapper reportWrapper) {
    return reports.containsReport(reportWrapper);
  }

  /**
   * Retrieves the procedure with the given id.
   * @param <C> the type of the database connection this procedure requires
   * @param procedureId the procedure id
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C> DatabaseProcedure<C> getProcedure(final String procedureId) {
    return operations.getProcedure(procedureId);
  }

  /**
   * Retrieves the function with the given id.
   * @param <C> the type of the database connection this function requires
   * @param <T> the result type
   * @param functionId the function id
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C, T> DatabaseFunction<C, T> getFunction(final String functionId) {
    return operations.getFunction(functionId);
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model, using the {@code entityId} as table name.
   * Returns the {@link EntityDefinition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param propertyBuilders the {@link Property.Builder} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  protected final EntityDefinition.Builder define(final String entityId, final Property.Builder... propertyBuilders) {
    return define(entityId, entityId, propertyBuilders);
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model.
   * Returns a {@link EntityDefinition.Builder} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param propertyBuilders the {@link Property.Builder} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type
   */
  protected final EntityDefinition.Builder define(final String entityId, final String tableName,
                                                  final Property.Builder... propertyBuilders) {
    return entities.defineInternal(entityId, tableName, propertyBuilders);
  }

  /**
   * Adds a report to this domain model.
   * @param reportWrapper the report to add
   * @throws RuntimeException in case loading the report failed
   * @throws IllegalArgumentException in case the report has already been added
   */
  protected final void addReport(final ReportWrapper reportWrapper) {
    reports.addReport(reportWrapper);
  }

  /**
   * Adds the given Operation to this domain
   * @param operation the operation to add
   * @throws IllegalArgumentException in case an operation with the same id has already been added
   */
  protected final void addOperation(final DatabaseOperation operation) {
    operations.addOperation(operation);
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

    private DomainEntities(final String domainId) {
      super(domainId);
    }

    protected EntityDefinition.Builder defineInternal(final String entityId, final String tableName,
                                                      final Property.Builder... propertyBuilders) {
      return super.define(entityId, tableName, propertyBuilders);
    }

    private void setStrictForeignKeysInternal(final boolean strictForeignKeys) {
      super.setStrictForeignKeys(strictForeignKeys);
    }
  }

  private static final class DomainOperations {

    private final Map<String, DatabaseOperation> operations = new HashMap<>();

    private void addOperation(final DatabaseOperation operation) {
      requireNonNull(operation, "operation");
      if (operations.containsKey(operation.getId())) {
        throw new IllegalArgumentException("Operation already defined: " + operations.get(operation.getId()).getName());
      }

      operations.put(operation.getId(), operation);
    }

    private <C> DatabaseProcedure<C> getProcedure(final String procedureId) {
      requireNonNull(procedureId, "procedureId");
      final DatabaseOperation operation = operations.get(procedureId);
      if (operation == null) {
        throw new IllegalArgumentException("Procedure not found: " + procedureId);
      }

      return (DatabaseProcedure<C>) operation;
    }

    private <C, T> DatabaseFunction<C, T> getFunction(final String functionId) {
      requireNonNull(functionId, "functionId");
      final DatabaseOperation operation = operations.get(functionId);
      if (operation == null) {
        throw new IllegalArgumentException("Function not found: " + functionId);
      }

      return (DatabaseFunction<C, T>) operation;
    }
  }

  private static final class DomainReports {

    private final Set<ReportWrapper> reports = new HashSet<>();

    private void addReport(final ReportWrapper report) {
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

    private boolean containsReport(final ReportWrapper reportWrapper) {
      return reports.contains(requireNonNull(reportWrapper, "reportWrapper"));
    }
  }
}
