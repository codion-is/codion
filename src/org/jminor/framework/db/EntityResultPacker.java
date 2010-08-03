/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ResultPacker;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Handles packing Entity query results.
 * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
 */
public final class EntityResultPacker implements ResultPacker<Entity> {

  private final String entityID;
  private final Collection<Property.ColumnProperty> properties;
  private final Collection<Property.TransientProperty> transientProperties;

  public EntityResultPacker(final String entityID, final Collection<Property.ColumnProperty> properties,
                            final Collection<Property.TransientProperty> transientProperties) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(properties, "properties");
    this.entityID = entityID;
    this.properties = properties;
    this.transientProperties = transientProperties;
  }

  /**
   * Packs the contents of <code>resultSet</code> into a List of Entity objects.
   * The resulting entities do not contain values for foreign key properties (Property.ForeignKeyProperty).
   * This method does not close the ResultSet object.
   * @param resultSet the ResultSet object
   * @param fetchCount the maximum number of records to retrieve from the result set
   * @return a List of Entity objects representing the contents of <code>resultSet</code>
   * @throws SQLException in case of an exception
   */
  public List<Entity> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
    Util.rejectNullValue(resultSet, "resultSet");
    final List<Entity> entities = new ArrayList<Entity>();
    int counter = 0;
    while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
      entities.add(loadEntity(resultSet));
    }

    return entities;
  }

  protected Collection<? extends Property> getProperties() {
    return properties;
  }

  protected Entity loadEntity(final ResultSet resultSet) throws SQLException {
    final Entity entity = Entities.entityInstance(entityID);
    if (transientProperties != null && !transientProperties.isEmpty()) {
      for (final Property.TransientProperty transientProperty : transientProperties) {
        if (!(transientProperty instanceof Property.DenormalizedViewProperty)
                && !(transientProperty instanceof Property.DerivedProperty)) {
          entity.initializeValue(transientProperty, null);
        }
      }
    }
    for (final Property.ColumnProperty property : properties) {
      try {
        entity.initializeValue(property, getValue(resultSet, property));
      }
      catch (Exception e) {
        throw new SQLException("Unable to load property: " + property, e);
      }
    }

    return entity;
  }

  protected Object getValue(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
    if (property.isBoolean()) {
      return getBoolean(resultSet, property);
    }
    else {
      return getValue(resultSet, property.getType(), property.getSelectIndex());
    }
  }

  private Boolean getBoolean(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
    if (property instanceof Property.BooleanProperty) {
      return ((Property.BooleanProperty) property).toBoolean(
              getValue(resultSet, ((Property.BooleanProperty) property).getColumnType(), property.getSelectIndex()));
    }
    else {
      final Integer result = getInteger(resultSet, property.getSelectIndex());
      if (result == null) {
        return null;
      }

      switch (result) {
        case 0: return false;
        case 1: return true;
        default: return null;
      }
    }
  }

  private Object getValue(final ResultSet resultSet, final int sqlType, final int selectIndex) throws SQLException {
    switch (sqlType) {
      case Types.INTEGER:
        return getInteger(resultSet, selectIndex);
      case Types.DOUBLE:
        return getDouble(resultSet, selectIndex);
      case Types.DATE:
        return getDate(resultSet, selectIndex);
      case Types.TIMESTAMP:
        return getTimestamp(resultSet, selectIndex);
      case Types.VARCHAR:
        return getString(resultSet, selectIndex);
      case Types.BOOLEAN:
        return getBoolean(resultSet, selectIndex);
      case Types.CHAR: {
        final String val = getString(resultSet, selectIndex);
        if (!Util.nullOrEmpty(val)) {
          return val.charAt(0);
        }
        else {
          return null;
        }
      }
    }

    throw new IllegalArgumentException("Unknown value type: " + sqlType);
  }

  private Integer getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final int value = resultSet.getInt(columnIndex);

    return resultSet.wasNull() ? null : value;
  }

  private Double getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final double value = resultSet.getDouble(columnIndex);

    return resultSet.wasNull() ? null : value;
  }

  private String getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final String string = resultSet.getString(columnIndex);

    return resultSet.wasNull() ? null : string;
  }

  private Boolean getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getBoolean(columnIndex);
  }

  private Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getDate(columnIndex);
  }

  private Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getTimestamp(columnIndex);
  }
}
