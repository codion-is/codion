/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.List;

/**
 * A MethodLogger implementation tailored for EntityConnections
 */
public final class EntityConnectionLogger extends MethodLogger {

  private static final long serialVersionUID = 1;

  private static final String IS_CONNECTED = "isConnected";
  private static final String IS_VALID = "isValid";

  /**
   * Instatiates a new EntityConnectionLogger
   */
  public EntityConnectionLogger() {
    super(Configuration.getIntValue(Configuration.SERVER_CONNECTION_LOG_SIZE));
  }

  /**
   * Appends  the given log entries to the log
   * @param log the log
   * @param entry the log entry to append
   * @param indentation the indentation to use for the given log entries
   */
  public static void appendLogEntry(final StringBuilder log, final Entry entry, final int indentation) {
    if (entry != null) {
      log.append(entry.toString(indentation)).append("\n");
      final List<Entry> subLog = entry.getSubLog();
      appendLogEntries(log, subLog, indentation + 1);
    }
  }

  /**
   * Appends  the given log entries to the log
   * @param log the log
   * @param logger the log containing the entries to append
   * @param indentation the indentation to use for the given log entries
   */
  public static void appendLogEntries(final StringBuilder log, final List<Entry> logger, final int indentation) {
    if (logger != null) {
      for (final MethodLogger.Entry logEntry : logger) {
        log.append(logEntry.toString(indentation)).append("\n");
        final List<Entry> subLog = logEntry.getSubLog();
        appendLogEntries(log, subLog, indentation + 1);
      }
    }
  }

  /**
   * @param methodName the method name
   * @return true if this method logger should log the given method
   */
  @Override
  protected boolean shouldMethodBeLogged(final String methodName) {
    return !(methodName.equals(IS_CONNECTED) || methodName.equals(IS_VALID));
  }

  /** {@inheritDoc} */
  @Override
  protected String getMethodArgumentAsString(final Object argument) {
    if (argument == null) {
      return "";
    }

    final StringBuilder builder = new StringBuilder();
    if (argument instanceof EntityCriteria) {
      builder.append(appendEntityCriteria((EntityCriteria) argument));
    }
    else if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
      builder.append("[").append(argumentArrayToString((Object[]) argument)).append("]");
    }
    else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
      builder.append("[").append(argumentArrayToString(((Collection) argument).toArray())).append("]");
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

  private String appendEntityCriteria(final EntityCriteria criteria) {
    final StringBuilder builder = new StringBuilder();
    builder.append(criteria.getEntityID());
    final String whereClause = criteria.getWhereClause(true);
    if (!Util.nullOrEmpty(whereClause)) {
      builder.append(", ").append(whereClause);
    }
    final List<?> values = criteria.getValues();
    if (values != null) {
      builder.append(", ").append(getMethodArgumentAsString(values));
    }

    return builder.toString();
  }

  private static String getEntityParameterString(final Entity entity) {
    final StringBuilder builder = new StringBuilder();
    builder.append(entity.getEntityID()).append(" {");
    for (final Property property : Entities.getColumnProperties(entity.getEntityID(), true, true, true)) {
      final boolean modified = entity.isModified(property.getPropertyID());
      if (property instanceof Property.PrimaryKeyProperty || modified) {
        final StringBuilder valueString = new StringBuilder();
        if (modified) {
          valueString.append(entity.getOriginalValue(property.getPropertyID())).append("->");
        }
        valueString.append(entity.getValue(property.getPropertyID()));
        builder.append(property.getPropertyID()).append(":").append(valueString).append(",");
      }
    }
    builder.deleteCharAt(builder.length() - 1);

    return builder.append("}").toString();
  }

  private static String getEntityKeyParameterString(final Entity.Key argument) {
    return argument.getEntityID() + ", " + argument.toString();
  }
}
