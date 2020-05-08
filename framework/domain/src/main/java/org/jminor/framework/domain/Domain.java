/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.operation.DatabaseFunction;
import org.jminor.common.db.operation.DatabaseOperation;
import org.jminor.common.db.operation.DatabaseProcedure;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.entity.EntityDefinitions;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 */
public class Domain implements DomainEntities {

  private static final long serialVersionUID = 1;

  private static final Map<String, Domain> REGISTERED_DOMAINS = new HashMap<>();

  private final String domainId;
  private final DefaultDomainEntities domainEntities = new DefaultDomainEntities();
  private final transient DomainReports domainReports = new DomainReports();
  private final transient DomainOperations domainOperations = new DomainOperations();

  /**
   * Instantiates a new Domain with the simple name of the class as domain id
   * @see Class#getSimpleName()
   */
  public Domain() {
    this.domainId = getClass().getSimpleName();
  }

  /**
   * Instantiates a new Domain
   * @param domainId the domain identifier
   */
  public Domain(final String domainId) {
    this.domainId = requireNonNull(domainId, "domainId");
  }

  /**
   * Instantiates a new domain and copies all the entity definitions
   * and database operations from {@code domain}
   * @param domain the domain to copy
   */
  public Domain(final Domain domain) {
    this.domainId = requireNonNull(domain).domainId;
    this.domainEntities.putAll(domain.domainEntities);
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
    return domainId;
  }

  @Override
  public final Entity entity(final String entityId) {
    return domainEntities.entity(entityId);
  }

  @Override
  public final Entity entity(final Entity.Key key) {
    return domainEntities.entity(key);
  }

  @Override
  public final Entity.Key key(final String entityId) {
    return domainEntities.key(entityId);
  }

  @Override
  public final Entity.Key key(final String entityId, final Integer value) {
    return domainEntities.key(entityId, value);
  }

  @Override
  public final Entity.Key key(final String entityId, final Long value) {
    return domainEntities.key(entityId, value);
  }

  @Override
  public final List<Entity.Key> keys(final String entityId, final Integer... values) {
    return domainEntities.keys(entityId, values);
  }

  @Override
  public final List<Entity.Key> keys(final String entityId, final Long... values) {
    return domainEntities.keys(entityId, values);
  }

  @Override
  public final Entity defaultEntity(final String entityId, final Function<Property, Object> valueProvider) {
    return domainEntities.defaultEntity(entityId, valueProvider);
  }

  @Override
  public final <V> List<V> toBeans(final List<Entity> entities) {
    return domainEntities.toBeans(entities);
  }

  @Override
  public final <V> V toBean(final Entity entity) {
    return domainEntities.toBean(entity);
  }

  @Override
  public final List<Entity> fromBeans(final List beans) {
    return domainEntities.fromBeans(beans);
  }

  @Override
  public final <V> Entity fromBean(final V bean) {
    return domainEntities.fromBean(bean);
  }

  @Override
  public final List<Entity> deepCopyEntities(final List<Entity> entities) {
    return domainEntities.deepCopyEntities(entities);
  }

  @Override
  public final Entity copyEntity(final Entity entity) {
    return domainEntities.copyEntity(entity);
  }

  @Override
  public final Entity deepCopyEntity(final Entity entity) {
    return domainEntities.deepCopyEntity(entity);
  }

  @Override
  public final Entity.Key copyKey(final Entity.Key key) {
    return domainEntities.copyKey(key);
  }

  @Override
  public final EntityDefinition getDefinition(final String entityId) {
    return domainEntities.getDefinition(entityId);
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return domainEntities.getDefinitions();
  }

  @Override
  public final Entity createToStringEntity(final String entityId, final String toStringValue) {
    return domainEntities.createToStringEntity(entityId, toStringValue);
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
   * Registers this instance for lookup via {@link Domain#getDomain(String)}, required for serialization
   * of domain objects, entities and related classes.
   * @return this Domain instance
   * @see #getDomainId()
   */
  public final Domain registerDomain() {
    return registerDomain(domainId, this);
  }

  /**
   * Retrievs the Domain with the given id.
   * @param domainId the id of the domain for which to retrieve the entity definitions
   * @return the domain instance registered for the given domainId
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerDomain()
   */
  public static Domain getDomain(final String domainId) {
    final Domain domain = REGISTERED_DOMAINS.get(domainId);
    if (domain == null) {
      throw new IllegalArgumentException("Domain '" + domainId + "' has not been registered");
    }

    return domain;
  }

  /**
   * @return all domains that have been registered via {@link #registerDomain()}
   */
  public static Collection<Domain> getRegisteredDomains() {
    return Collections.unmodifiableCollection(REGISTERED_DOMAINS.values());
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
    domainEntities.addDefinition(definitionBuilder.domainId(domainId).get());

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
    domainEntities.setStrictForeignKeys(strictForeignKeys);
  }

  /**
   * {@link #domainOperations} is transient and only null after deserialization.
   */
  private void checkIfDeserialized() {
    if (domainOperations == null) {
      throw new IllegalStateException("Operations and reports are not available in a deserialized Domain model");
    }
  }

  private static Domain registerDomain(final String domainId, final Domain domain) {
    REGISTERED_DOMAINS.put(domainId, domain);

    return domain;
  }
}
