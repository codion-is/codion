/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ResultPacker;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Handles packing Entity query results.
 * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
 */
public class EntityResultPacker implements ResultPacker<Entity> {

  private final String entityID;
  private final Collection<? extends Property> properties;
  private final Collection<Property.TransientProperty> transientProperties;

  public EntityResultPacker(final String entityID, final Collection<? extends Property> properties,
                            final Collection<Property.TransientProperty> transientProperties) {
    if (entityID == null)
      throw new IllegalArgumentException("EntityResultPacker requires a non-null entityID");
    if (properties == null)
      throw new IllegalArgumentException("EntityResultPacker requires non-null properties");
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
    if (resultSet == null)
      throw new IllegalArgumentException("Can not pack result from a null ResultSet");
    final List<Entity> entities = new ArrayList<Entity>();
    int counter = 0;
    while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount))
      entities.add(loadEntity(resultSet));

    return entities;
  }

  protected Collection<? extends Property> getProperties() {
    return properties;
  }

  protected Entity loadEntity(final ResultSet resultSet) throws SQLException {
    final Entity entity = new Entity(entityID);
    entity.setLoaded(true);
    if (transientProperties != null && transientProperties.size() > 0) {
      for (final Property.TransientProperty transientProperty : transientProperties) {
        if (!(transientProperty instanceof Property.DenormalizedViewProperty))
          entity.setValue(transientProperty, null, true);
      }
    }
    for (final Property property : properties) {
      if (!(property instanceof Property.ForeignKeyProperty) && !property.isDenormalized()) {
        try {
          entity.setValue(property, getValue(resultSet, property), true);
        }
        catch (Exception e) {
          throw new SQLException("Unable to load property: " + property, e);
        }
      }
    }

    return entity;
  }

  protected Object getValue(final ResultSet resultSet, final Property property) throws SQLException {
    if (property.isValueClass(ValueMap.class))
      throw new IllegalArgumentException("EntityResultPacker does not handle loading of reference properties");
    else if (property.isValueClass(Boolean.class))
      return getBoolean(resultSet, property);
    else
      return getValue(resultSet, property.getValueClass(), property.getSelectIndex());
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

  private Object getValue(final ResultSet resultSet, final Class valueClass, final int selectIndex) throws SQLException {
    if (valueClass.equals(Integer.class))
      return getInteger(resultSet, selectIndex);
    else if (valueClass.equals(Double.class))
      return getDouble(resultSet, selectIndex);
    else if (valueClass.equals(Date.class))
      return getDate(resultSet, selectIndex);
    else if (valueClass.equals(Timestamp.class))
      return getTimestamp(resultSet, selectIndex);
    else if (valueClass.equals(String.class))
      return getString(resultSet, selectIndex);
    else if (valueClass.equals(Character.class)) {
      final String val = getString(resultSet, selectIndex);
      if (val != null && val.length() > 0)
        return val.charAt(0);
      else
        return null;
    }
    else
      throw new IllegalArgumentException("Unknown value class: " + valueClass);
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

    return string == null ? "" : string;
  }

  private Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getDate(columnIndex);
  }

  private Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getTimestamp(columnIndex);
  }
}
