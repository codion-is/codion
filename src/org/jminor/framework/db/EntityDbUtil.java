/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.IdSource;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A static utility class.
 * User: Bjorn Darri
 * Date: 30.3.2010
 * Time: 21:47:16
 */
public class EntityDbUtil {

  /**
   * Returns a SQL string version of the given value
   * @param database the Database instance
   * @param property the property
   * @param value the value
   * @return a SQL string version of value
   */
  public static String getSQLStringValue(final Database database, final Property property, final Object value) {
    if (Entity.isValueNull(property.getPropertyType(), value))
      return "null";

    switch (property.getPropertyType()) {
      case INT:
      case DOUBLE:
        return value.toString();//localize?
      case TIMESTAMP:
      case DATE:
        if (!(value instanceof Date))
          throw new IllegalArgumentException("Date value expected for property: " + property + ", got: " + value.getClass());
        return database.getSQLDateString((Date) value, property.getPropertyType() == Type.TIMESTAMP);
      case CHAR:
        return "'" + value + "'";
      case STRING:
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for property: " + property + ", got: " + value.getClass());
        return "'" + sqlEscapeString((String) value) + "'";
      case BOOLEAN:
        if (!(value instanceof Boolean))
          throw new IllegalArgumentException("Boolean value expected for property: " + property + ", got: " + value.getClass());
        return getBooleanSQLString(property, (Boolean) value);
      case ENTITY:
        return value instanceof Entity ? getSQLStringValue(database, property, ((Entity)value).getPrimaryKey().getFirstKeyValue())
                : getSQLStringValue(database, ((Entity.Key)value).getFirstKeyProperty(), ((Entity.Key)value).getFirstKeyValue());
      default:
        throw new IllegalArgumentException("Undefined property type: " + property.getPropertyType());
    }
  }

  public static String sqlEscapeString(final String val) {
    return val.replaceAll("'", "''");
  }

  public static String getBooleanSQLString(final Property property, final Boolean value) {
    if (property instanceof Property.BooleanProperty)
      return ((Property.BooleanProperty) property).toSQLString(value);
    else {
      if (value == null)
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_NULL) + "";
      else if (value)
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE) + "";
      else
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE) + "";
    }
  }

  /**
   * Constructs a where condition based on the given primary key
   * @param database the Database instance
   * @param entityKey the EntityKey instance
   * @return a where clause using this EntityKey instance, without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple column key "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final Entity.Key entityKey) {
    return getWhereCondition(database, entityKey.getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entityKey.getValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the primary key of the given entity, using the
   * original property values. This method should be used when updating an entity in case
   * a primary key property value has changed, hence using the original value.
   * @param database the Database instance
   * @param entity the Entity instance
   * @return a where clause specifying this entity instance, without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple column key "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final Entity entity) {
    return getWhereCondition(database, entity.getPrimaryKey().getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entity.getOriginalValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the given primary key properties and the values provide by <code>valueProvider</code>
   * @param database the Database instance
   * @param properties the properties to use when constructing the condition
   * @param valueProvider the value provider
   * @return a where clause according to the given properties and the values provided by <code>valueProvider</code>,
   * without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple properties "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final List<Property.PrimaryKeyProperty> properties,
                                         final ValueProvider valueProvider) {
    final StringBuilder stringBuilder = new StringBuilder("(");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      stringBuilder.append(getQueryString(property.getPropertyID(),
              getSQLStringValue(database, property, valueProvider.getValue(property.getPropertyID()))));
      if (i++ < properties.size() - 1)
        stringBuilder.append(" and ");
    }

    return stringBuilder.append(")").toString();
  }

  /**
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string, e.g. "columnName = sqlStringValue"
   * or "columnName is null" in case sqlStringValue is 'null'
   */
  public static String getQueryString(final String columnName, final String sqlStringValue) {
    return new StringBuilder(columnName).append(sqlStringValue.equalsIgnoreCase("null") ?
            " is " : " = ").append(sqlStringValue).toString();
  }

  /**
   * Returns the properties used when inserting an instance of this entity, leaving out properties with null values
   * @param entity the entity
   * @return the properties used to insert the given entity type
   */
  public static Collection<Property> getInsertProperties(final Entity entity) {
    final Collection<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(),
            EntityRepository.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.ForeignKeyProperty) && !entity.isValueNull(property.getPropertyID()))
        properties.add(property);
    }

    return properties;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  public static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.getPropertyID()) && !(property instanceof Property.ForeignKeyProperty))
        properties.add(property);

    return properties;
  }

  private interface ValueProvider {
    public Object getValue(final String propertyID);
  }
}
