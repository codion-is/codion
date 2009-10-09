/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ResultPacker;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handles packing Entity query results.
 * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
 */
public class EntityResultPacker implements ResultPacker<Entity> {

  private final String entityID;
  private final Collection<Property> properties;

  public EntityResultPacker(final String entityID, final Collection<Property> properties) {
    if (entityID == null)
      throw new IllegalArgumentException("EntityResultPacker requires a non-null entityID");
    if (properties == null)
      throw new IllegalArgumentException("EntityResultPacker requires non-null properties");
    this.entityID = entityID;
    this.properties = properties;
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
    if (resultSet == null)
      throw new IllegalArgumentException("Can not pack result from a null ResultSet");
    final List<Entity> ret = new ArrayList<Entity>();
    int counter = 0;
    while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount))
      ret.add(loadEntity(resultSet));

    return ret;
  }

  private Entity loadEntity(final ResultSet resultSet) throws SQLException {
    final Entity entity = new Entity(entityID);
    for (final Property property : properties)
      if (!(property instanceof Property.ForeignKeyProperty)) {
        try {
          entity.initializeValue(property, getValue(resultSet, property));
        }
        catch (Exception e) {
          throw new SQLException("Unable to load property: " + property, e);
        }
      }

    return entity;
  }

  private Object getValue(final ResultSet resultSet, final Property property) throws SQLException {
    switch (property.getPropertyType()) {
      case ENTITY:
        throw new IllegalArgumentException("EntityResultPacker does not handle loading of reference properties");
      case BOOLEAN:
        return getBoolean(resultSet, property);
      default:
        return getValue(resultSet, property.getPropertyType(), property.getSelectIndex());
    }
  }

  private Boolean getBoolean(final ResultSet resultSet, final Property property) throws SQLException {
    if (property instanceof Property.BooleanProperty)
      return ((Property.BooleanProperty) property).toBoolean(
              getValue(resultSet, ((Property.BooleanProperty) property).getColumnType(), property.getSelectIndex()));
    else {
      final Integer result = getInteger(resultSet, property.getSelectIndex());
      if (result == null)
        return null;

      switch (result) {
        case 0: return false;
        case 1: return true;
        default: return null;
      }
    }
  }

  private Object getValue(final ResultSet resultSet, final Type propertyType, final int selectIndex) throws SQLException {
    switch (propertyType) {
      case INT:
        return getInteger(resultSet, selectIndex);
      case DOUBLE:
        return getDouble(resultSet, selectIndex);
      case DATE:
      case TIMESTAMP:
        return getTimestamp(resultSet, selectIndex);
      case STRING:
        return getString(resultSet, selectIndex);
      case CHAR:
        final String val = getString(resultSet, selectIndex);
        if (val != null && val.length() > 0)
          return val.charAt(0);
        else
          return null;
      default:
        throw new IllegalArgumentException("Unknown property type: " + propertyType);
    }
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
    final String ret = resultSet.getString(columnIndex);

    return ret == null ? "" : ret;
  }

  private Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getTimestamp(columnIndex);
  }
}
