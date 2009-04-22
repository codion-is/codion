/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.IdSource;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * User: Bj�rn Darri
 * Date: 30.3.2009
 * Time: 20:22:52
 */
public class EntityDbUtil {

  private EntityDbUtil() {}

  /**
   * Returns a SQL string version of the given value
   * @param property the property
   * @param value the value
   * @return a SQL string version of value
   */
  public static String getSQLStringValue(final Property property, final Object value) {
    if (Entity.isValueNull(property.propertyType, value))
      return "null";

    switch (property.propertyType) {
      case INT :
      case DOUBLE :
        return value.toString();//localize?
      case LONG_DATE :
      case SHORT_DATE :
        if (!(value instanceof Date))
          throw new IllegalArgumentException("Date value expected for property: " + property + ", got: " + value.getClass());
        return Database.getSQLDateString((Date) value, property.propertyType == Type.LONG_DATE);
      case CHAR :
        return "'" + value + "'";
      case STRING :
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for property: " + property + ", got: " + value.getClass());
        return "'" + Util.sqlEscapeString((String) value) + "'";
      case BOOLEAN :
        if (!(value instanceof Type.Boolean))
          throw new IllegalArgumentException("Type.Boolean value expected for property: " + property + ", got: " + value.getClass());
        return getBooleanSQLString(property, (Type.Boolean) value);
      case ENTITY :
        return value instanceof Entity ? getSQLStringValue(property, ((Entity)value).getPrimaryKey().getFirstKeyValue())
                : getSQLStringValue(((EntityKey)value).getFirstKeyProperty(), ((EntityKey)value).getFirstKeyValue());
      default :
        throw new IllegalArgumentException("Undefined property type: " + property.propertyType);
    }
  }

  public static String getBooleanSQLString(final Property property, final Type.Boolean value) {
    if (property instanceof Property.BooleanProperty)
      return ((Property.BooleanProperty) property).toSQLString(value);
    else {
      switch(value) {
        case FALSE : return FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_FALSE) + "";
        case TRUE: return FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_TRUE) + "";
        case NULL: return FrameworkSettings.get().getProperty(FrameworkSettings.SQL_BOOLEAN_VALUE_NULL) + "";
        default : throw new RuntimeException("Unknown boolean value: " + value);
      }
    }
  }

  /**
   * @param entity the Entity instance
   * @return a where clause specifying this entity instance,
   * e.g. " where (idCol = 42)", " where (idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Entity entity) {
    final StringBuffer ret = new StringBuffer(" where (");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : entity.getPrimaryKey().getProperties()) {
      ret.append(getQueryString(property.propertyID, getSQLStringValue(property, entity.getOriginalValue(property.propertyID))));
      if (i++ < entity.getPrimaryKey().getPropertyCount() - 1)
        ret.append(" and ");
    }

    return ret.append(")").toString();
  }

  /**
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string, e.g. "columnName = sqlStringValue"
   * or "columnName is null" in case sqlStringValue is 'null'
   */
  public static String getQueryString(final String columnName, final String sqlStringValue) {
    return new StringBuffer(columnName).append(sqlStringValue.toUpperCase().equals("NULL") ?
            " is " : " = ").append(sqlStringValue).toString();
  }

  /**
   * Returns the insert properties for this entityID
   * @param entityID the entityID
   * @return the properties used to insert the given entity type
   */
  public static List<Property> getInsertProperties(final String entityID) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.get().getDatabaseProperties(entityID,
            EntityRepository.get().getIdSource(entityID) != IdSource.ID_AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.EntityProperty))
        ret.add(property);
    }

    return ret;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  public static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.get().getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.propertyID) && !(property instanceof Property.EntityProperty))
        ret.add(property);

    return ret;
  }
}
