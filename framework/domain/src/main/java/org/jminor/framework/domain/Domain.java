/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.operation.DatabaseFunction;
import org.jminor.common.db.operation.DatabaseOperation;
import org.jminor.common.db.operation.DatabaseProcedure;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.entity.EntityDefinitions;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 */
public class Domain implements EntityDefinition.Provider {

  private final DomainEntities entities;
  private final transient DomainReports domainReports = new DomainReports();
  private final transient DomainOperations domainOperations = new DomainOperations();

  /**
   * Instantiates a new Domain with the simple name of the class as domain id
   * @see Class#getSimpleName()
   */
  public Domain() {
    this.entities = new DomainEntities(getClass().getSimpleName());
  }

  /**
   * Instantiates a new Domain
   * @param domainId the domain identifier
   */
  public Domain(final String domainId) {
    this.entities = new DomainEntities(requireNonNull(domainId, "domainId"));
  }

  /**
   * Instantiates a new domain and copies all the entity definitions
   * and database operations from {@code domain}
   * @param domain the domain to copy
   */
  public Domain(final Domain domain) {
    this.entities = new DomainEntities(requireNonNull(domain).getDomainId());
    this.entities.putAll(domain.entities);
    if (domain.domainReports != null) {
      this.domainReports.addAll(domain.domainReports);
    }
    if (domain.domainOperations != null) {
      this.domainOperations.putAll(domain.domainOperations);
    }
  }

  /**
   * @return the domain Id
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

  @Override
  public EntityDefinition getDefinition(final String entityId) {
    return entities.getDefinition(entityId);
  }

  @Override
  public Collection<EntityDefinition> getDefinitions() {
    return entities.getDefinitions();
  }

  public final boolean containsReport(final ReportWrapper reportWrapper) {
   checkIfDeserialized();
   return domainReports.containsReport(reportWrapper);
  }

  public final <C> DatabaseProcedure<C> getProcedure(final String procedureId) {
    checkIfDeserialized();
    return domainOperations.getProcedure(procedureId);
  }

  public final <C, T> DatabaseFunction<C, T> getFunction(final String functionId) {
    checkIfDeserialized();
    return domainOperations.getFunction(functionId);
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
    return addDefinition(EntityDefinitions.definition(entityId, tableName, propertyBuilders));
  }

  /**
   * Adds the {@link EntityDefinition} supplied by the given {@link EntityDefinition.Builder} to this domain model.
   * @param definitionBuilder the {@link EntityDefinition.Builder}
   * @return the {@link EntityDefinition.Builder}
   */
  protected final EntityDefinition.Builder addDefinition(final EntityDefinition.Builder definitionBuilder) {
    entities.addDefinition(definitionBuilder.domainId(getDomainId()).get());

    return definitionBuilder;
  }

  /**
   * Adds a report to this domain model.
   * @param reportWrapper the report to add
   * @throws RuntimeException in case loading the report failed
   * @throws IllegalArgumentException in case the report has already been added
   */
  protected final void addReport(final ReportWrapper reportWrapper) {
    checkIfDeserialized();
    domainReports.addReport(reportWrapper);
  }

  /**
   * Adds the given Operation to this domain
   * @param operation the operation to add
   * @throws IllegalArgumentException in case an operation with the same id has already been added
   */
  protected final void addOperation(final DatabaseOperation operation) {
    checkIfDeserialized();
    domainOperations.addOperation(operation);
  }

  /**
   * Specifies whether it should be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in cases where entities have circular references.
   * @param strictForeignKeys true for strict foreign key validation
   */
  protected final void setStrictForeignKeys(final boolean strictForeignKeys) {
    entities.setStrictForeignKeys(strictForeignKeys);
  }

  /**
   * {@link #domainOperations} is transient and only null after deserialization.
   */
  private void checkIfDeserialized() {
    if (domainOperations == null) {
      throw new IllegalStateException("Operations and reports are not available in a deserialized Domain model");
    }
  }
}
