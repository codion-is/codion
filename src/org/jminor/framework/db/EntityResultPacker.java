/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.IResultPacker;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handles packing Entity query results.
 * Loads all database property values except for reference properties (Property.EntityProperty).
 */
public class EntityResultPacker implements IResultPacker<Entity> {

  private final String entityID;
  private final Collection<Property> properties;

  private ResultSet resultSet;

  public EntityResultPacker(final String entityID, final Collection<Property> properties) {
    this.entityID = entityID;
    this.properties = properties;
  }

  /**
   * Packs the contents of <code>resultSet</code> into a List of Entity objects.
   * The resulting entities do not contain values for reference properties (Property.EntityProperty).
   * This method does not close the ResultSet object.
   * @param resultSet the ResultSet object
   * @param recordCount the maximum number of records to retrieve from the result set
   * @return a List of Entity objects representing the contents of <code>resultSet</code>
   * @throws SQLException in case of an exception
   */
  public synchronized List<Entity> pack(final ResultSet resultSet, final int recordCount) throws SQLException {
    try {
      this.resultSet = resultSet;
      final List<Entity> ret = new ArrayList<Entity>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount))
        ret.add(loadEntity());

      return ret;
    }
    finally {
      this.resultSet = null;
    }
  }

  private Integer getInteger(final int columnIndex) throws SQLException {
    final int value = resultSet.getInt(columnIndex);

    return resultSet.wasNull() ? null : value;
  }

  private Type.Boolean getBoolean(final Property property) throws SQLException {
    if (property instanceof Property.BooleanProperty)
      return ((Property.BooleanProperty) property).toBoolean(
              getValue(((Property.BooleanProperty) property).columnType, property.selectIndex));
    else {
      final Integer result = getInteger(property.selectIndex);
      if (result == null)
        return null;

      switch (result) {
        case 0 : return Type.Boolean.FALSE;
        case 1 : return Type.Boolean.TRUE;
        default : return null;
      }
    }
  }

  private Double getDouble(final int columnIndex) throws SQLException {
    final double value = resultSet.getDouble(columnIndex);

    return resultSet.wasNull() ? null : value;
  }

  private String getString(final int columnIndex) throws SQLException {
    final String ret = resultSet.getString(columnIndex);

    return ret == null ? "" : ret;
  }

  private Timestamp getTimestamp(final int columnIndex) throws SQLException {
    return resultSet.getTimestamp(columnIndex);
  }

  private Entity loadEntity() throws SQLException {
    final Entity entity = new Entity(entityID);
    for (final Property property : properties)
      if (!(property instanceof Property.EntityProperty)) {
        try {
          entity.initializeValue(property, getValue(property));
        }
        catch (SQLException e) {
          throw new SQLException("Unable to load property: " + property, e);
        }
      }

    return entity;
  }

  private Object getValue(final Property property) throws SQLException {
    switch (property.propertyType) {
      case ENTITY :
        throw new IllegalArgumentException("EntityResultPacker does not handle loading of reference properties");
      case BOOLEAN :
        return getBoolean(property);
      default:
        return getValue(property.propertyType, property.selectIndex);
    }
  }

  private Object getValue(final Type propertyType, final int selectIndex) throws SQLException {
    switch (propertyType) {
      case INT :
        return getInteger(selectIndex);
      case DOUBLE :
        return getDouble(selectIndex);
      case SHORT_DATE :
      case LONG_DATE :
        return getTimestamp(selectIndex);
      case STRING :
        return getString(selectIndex);
      case CHAR :
        final String val = getString(selectIndex);
        if (val != null && val.length() > 0)
          return val.charAt(0);
        else
          return null;
      default :
        throw new IllegalArgumentException("Unknown property type: " + propertyType);
    }
  }
}
