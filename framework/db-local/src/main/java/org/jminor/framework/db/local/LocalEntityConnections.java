/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

/**
 * A factory class providing a LocalEntityConnection instances.
 */
public final class LocalEntityConnections {

  private LocalEntityConnections() {}

  /**
   * Constructs a new EntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new LocalEntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  public static LocalEntityConnection createConnection(final Domain domain, final Database database, final User user)
          throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, user,
            DatabaseConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get())
            .setOptimisticLocking(LocalEntityConnection.USE_OPTIMISTIC_LOCKING.get())
            .setLimitForeignKeyFetchDepth(LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get());
  }

  /**
   * Constructs a new EntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new LocalEntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  public static LocalEntityConnection createConnection(final Domain domain, final Database database, final Connection connection)
          throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, connection,
            DatabaseConnection.CONNECTION_VALIDITY_CHECK_TIMEOUT.get())
            .setOptimisticLocking(LocalEntityConnection.USE_OPTIMISTIC_LOCKING.get())
            .setLimitForeignKeyFetchDepth(LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get());
  }

  /**
   * @param definitionProvider the Entity.Definition provider
   * @return A {@link MethodLogger} implementation tailored for LocalEntityConnections
   */
  public static MethodLogger createLogger(final EntityDefinition.Provider definitionProvider) {
    return new MethodLogger(LocalEntityConnection.CONNECTION_LOG_SIZE.get(),
            false, new EntityArgumentStringProvider(definitionProvider));
  }

  /**
   * A {@link MethodLogger.ArgumentStringProvider} implementation tailored for EntityConnections
   */
  private static final class EntityArgumentStringProvider extends MethodLogger.DefaultArgumentStringProvider {

    private final EntityDefinition.Provider definitionProvider;

    private EntityArgumentStringProvider(final EntityDefinition.Provider definitionProvider) {
      this.definitionProvider = definitionProvider;
    }

    @Override
    public String toString(final Object argument) {
      if (argument == null) {
        return "";
      }

      final StringBuilder builder = new StringBuilder();
      if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
        builder.append("[").append(toString((Object[]) argument)).append("]");
      }
      else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
        builder.append("[").append(toString(((Collection) argument).toArray())).append("]");
      }
      else if (argument instanceof Entity) {
        builder.append(getEntityParameterString((Entity) argument));
      }
      else if (argument instanceof Entity.Key) {
        builder.append(getEntityKeyParameterString((Entity.Key) argument));
      }
      else {
        builder.append(argument.toString());
      }

      return builder.toString();
    }

    private String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder(entity.getEntityId()).append(" {");
      final List<ColumnProperty> columnProperties = definitionProvider.getDefinition(entity.getEntityId()).getColumnProperties();
      for (int i = 0; i < columnProperties.size(); i++) {
        final ColumnProperty property = columnProperties.get(i);
        final boolean modified = entity.isModified(property);
        if (property.isPrimaryKeyProperty() || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property)).append("->");
          }
          valueString.append(entity.get(property.getPropertyId()));
          builder.append(property.getPropertyId()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String getEntityKeyParameterString(final Entity.Key argument) {
      return argument.getEntityId() + " {" + argument.toString() + "}";
    }
  }
}
