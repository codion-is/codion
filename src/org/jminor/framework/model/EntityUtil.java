/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Constants;
import org.jminor.common.db.DbUtil;
import org.jminor.common.db.IdSource;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A static utility class
 */
public class EntityUtil {

  private EntityUtil() {}

  public static boolean activeDependencies(final Map<String, List<Entity>> entities) {
    for (final List<Entity> ents : entities.values()) {
      if (ents.size() > 0)
        return true;
    }

    return false;
  }

  public static boolean equal(final Type type, final Object one, final Object two) {
    final boolean oneNull = isValueNull(type, one);
    final boolean twoNull = isValueNull(type, two);

    return oneNull && twoNull || !(oneNull ^ twoNull) && one.equals(two);
  }

  public static boolean isValueNull(final Type propertyType, final Object value) {
    if (value == null)
      return true;

    switch (propertyType) {
      case INT :
        return value.equals(Constants.INTEGER_NULL_VALUE);
      case DOUBLE :
        return value.equals(Constants.DOUBLE_NULL_VALUE);
      case CHAR :
        if (value instanceof String)
          return ((String)value).length() == 0 || ((String)value).charAt(0) == Constants.CHAR_NULL_VALUE;
        else if (value instanceof Character)
          return value.equals(Constants.CHAR_NULL_VALUE);
      case BOOLEAN :
        return value == Type.Boolean.NULL;
      case SHORT_DATE :
      case LONG_DATE :
        return value.equals(Constants.TIMESTAMP_NULL_VALUE);
      case STRING :
        return value.equals("");
      case ENTITY :
        return value instanceof Entity ? ((Entity) value).isNull() : ((EntityKey) value).isNull();
      default :
        return false;
    }
  }

  public static String getSQLStringValue(final Property property, final Object value) {
    if (isValueNull(property.propertyType, value))
      return "null";

    switch (property.propertyType) {
      case INT :
      case DOUBLE :
        return value.toString();//localize?
      case LONG_DATE :
      case SHORT_DATE :
        return DbUtil.getSQLDateString((Date) value, property.propertyType == Type.LONG_DATE);
      case CHAR :
        return "'" + value + "'";
      case STRING :
        return "'" + Util.sqlEscapeString((String) value) + "'";
      case BOOLEAN :
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
        case FALSE : return FrameworkSettings.get().sqlBooleanValueFalse + "";
        case TRUE: return FrameworkSettings.get().sqlBooleanValueTrue + "";
        case NULL: return FrameworkSettings.get().sqlBooleanValueNull + "";
        default : throw new RuntimeException("Unknown boolean value: " + value);
      }
    }
  }

  public static HashMap<EntityKey, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final HashMap<EntityKey, Entity> ret = new HashMap<EntityKey, Entity>();
    for (final Entity entity : entities)
      ret.put(entity.getPrimaryKey(), entity);

    return ret;
  }

  public static List<EntityKey> toList(final Set<EntityKey> keys) {
    final ArrayList<EntityKey> ret = new ArrayList<EntityKey>(keys.size());
    for (final EntityKey key : keys)
      ret.add(key);

    return ret;
  }

  public static String getValueString(final Property property, final Object value) {
    final boolean valueIsNull = isValueNull(property.propertyType, value);
    final StringBuffer ret = new StringBuffer("[").append(valueIsNull ? (value == null ? "null" : "null value") : value).append("]");
    if (value instanceof Entity)
      ret.append(" PK{").append(((Entity)value).getPrimaryKey()).append("}");

    return ret.toString();
  }

  public static String getPropertyChangeDebugString(final String entityID, final Property property,
                                                    final Object oldValue, final Object newValue,
                                                    final boolean isInitialization) {
    final StringBuffer ret = new StringBuffer();
    if (isInitialization)
      ret.append("INIT ");
    else
      ret.append("SET").append(Util.equal(oldValue, newValue) ? " == " : " <> ");
    ret.append(entityID).append(" -> ").append(property).append("; ");
    if (!isInitialization) {
      if (oldValue != null)
        ret.append(oldValue.getClass().getSimpleName()).append(" ");
      ret.append(getValueString(property, oldValue));
    }
    if (!isInitialization)
      ret.append(" : ");
    if (newValue != null)
      ret.append(newValue.getClass().getSimpleName()).append(" ");
    ret.append(getValueString(property, newValue));

    return ret.toString();
  }

  public static List<EntityKey> getPrimaryKeys(final Collection<Entity> entities) {
    final List<EntityKey> ret = new ArrayList<EntityKey>(entities.size());
    for (final Entity entity : entities)
      ret.add(entity.getPrimaryKey());

    return ret;
  }

  /**
   * @param entity the Entity instance
   * @return a query for inserting this entity instance
   */
  public static String getInsertSQL(final Entity entity) {
    final StringBuffer sql = new StringBuffer("insert into ");
    sql.append(Entity.repository.getTableName(entity.getEntityID())).append("(");
    final StringBuffer columnValues = new StringBuffer(") values(");
    final List<Property> insertColumns = getInsertProperties(entity.getEntityID());
    int columnIndex = 0;
    for (final Property property : insertColumns) {
      sql.append(property.propertyID);
      columnValues.append(getSQLStringValue(entity.getEntityID(), property.propertyID,
              entity.getValue(property.propertyID)));
      if (columnIndex++ < insertColumns.size()-1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * @param entity the Entity instance
   * @return a query for updating this entity instance
   * @throws RuntimeException in case the entity is unmodified
   */
  public static String getUpdateSQL(final Entity entity) {
    if (!entity.isModified())
      throw new RuntimeException("Can not get update sql since the entity is unmodified");

    final StringBuffer sql = new StringBuffer("update ");
    sql.append(Entity.repository.getTableName(entity.getEntityID())).append(" set ");
    final Collection<Property> properties = getUpdateProperties(entity);
    int columnIndex = 0;
    for (final Property property : properties) {
      final String columnName = property.propertyID;
      sql.append(columnName).append(" = ").append(getSQLStringValue(entity.getEntityID(), columnName, entity.getValue(columnName)));
      if (columnIndex++ < properties.size() - 1)
        sql.append(", ");
    }

    return sql.append(getWhereCondition(entity)).toString();
  }

  /**
   * @param entity the Entity instance
   * @return a query for deleting this entity instance
   */
  public static String getDeleteSQL(final Entity entity) {
    return "delete from " + Entity.repository.getTableName(entity.getEntityID()) + getWhereCondition(entity);
  }

  /**
   * @param entity the Entity instance
   * @return a where clause specifying this entity instance
   */
  public static String getWhereCondition(final Entity entity) {
    final StringBuffer ret = new StringBuffer(" where (");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : entity.getPrimaryKey().getProperties()) {
      ret.append(getQueryString(property, null, getSQLStringValue(property, entity.getOriginalValue(property.propertyID))));
      if (i++ < entity.getPrimaryKey().getColumnCount()-1)
        ret.append(" and ");
    }

    return ret.append(")").toString();
  }

  /**
   *
   * @param property the property
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string
   */
  public static String getQueryString(final Property property, final String columnName, final String sqlStringValue) {
    return new StringBuffer(columnName == null ? property.propertyID : columnName).append(" = ").append(
            sqlStringValue).toString();
  }

  public static Object[] getPropertyValue(final String propertyID, final List<Entity> entities) {
    return getPropertyValue(propertyID, entities, true);
  }

  public static Object[] getPropertyValue(final String propertyID, final List<Entity> entities,
                                          final boolean includeNullValues) {
    final List<Object> ret = new ArrayList<Object>(entities.size());
    for (Entity entity : entities) {
      if (includeNullValues)
        ret.add(entity.getValue(propertyID));
      else if (!entity.isValueNull(propertyID))
        ret.add(entity.getValue(propertyID));
    }

    return ret.toArray();
  }

  public static Object[] setPropertyValue(final String propertyID, final Object value,
                                          final List<Entity> entities) {
    final Object[] oldValues = getPropertyValue(propertyID, entities);
    for (final Entity entity : entities)
      entity.setValue(propertyID, value);

    return oldValues;
  }

  public static Object[] setPropertyValue(final String propertyID, final Object[] values,
                                          final List<Entity> entities) {
    final Object[] oldValues = getPropertyValue(propertyID, entities);
    for (int i = 0; i < entities.size(); i++)
      entities.get(i).setValue(propertyID, values[i]);

    return oldValues;
  }

  public static void printPropertyValues(final Entity entity) {
    final Collection<Property> properties = Entity.repository.getProperties(entity.getEntityID(), true);
    System.out.println("*********************[" + entity + "]***********************");
    for (final Property property : properties) {
      final Object value = entity.getValue(property.propertyID);
      System.out.println(property + " = " + getValueString(property, value));
    }
    System.out.println("********************************************");
  }

  public static HashMap<Class, List<Entity>> hashByType(final List<Entity> entities) {
    final HashMap<Class, List<Entity>> ret = new LinkedHashMap<Class, List<Entity>>();
    for (final Entity entity : entities) {
      List<Entity> entityList = ret.get(entity.getClass());
      if (entityList == null)
        ret.put(entity.getClass(), entityList = new ArrayList<Entity>());

      entityList.add(entity);
    }

    return ret;
  }

  public static HashMap<Object, List<Entity>> hashByPropertyValue(final List<Entity> entities, final String propertyID) {
    final HashMap<Object, List<Entity>> ret = new HashMap<Object, List<Entity>>(entities.size());
    for (final Entity entity : entities) {
      final Object key = entity.getValue(propertyID);
      if (ret.containsKey(key))
        ret.get(key).add(entity);
      else {
        final List<Entity> list = new ArrayList<Entity>();
        list.add(entity);
        ret.put(key, list);
      }
    }

    return ret;
  }

  public static HashMap<Property, List<Object>> hashPropertiesByName(final Property[] properties, final Object[] values) {
    final HashMap<Property, List<Object>> ret = new HashMap<Property, List<Object>>();
    for (int i = 0; i < values.length; i++) {
      List<Object> propList = ret.get(properties[i]);
      if (propList == null)
        ret.put(properties[i], propList = new ArrayList<Object>());

      propList.add(values[i]);
    }

    return ret;
  }

  /**
   * Returns a SQL string version of the given value
   * @param entityID the entityID
   * @param propertyID the property identifier
   * @param value the value
   * @return a SQL string version of value
   */
  private static String getSQLStringValue(final String entityID, final String propertyID, final Object value) {
    return getSQLStringValue(Entity.repository.getProperties(entityID).get(propertyID), value);
  }

  /**
   * Returns the insert properties for this entityID
   * @param entityID the entityID
   * @return the properties used to insert the given entity type
   */
  private static List<Property> getInsertProperties(final String entityID) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : Entity.repository.getDatabaseProperties(entityID,
            Entity.repository.getIdSource(entityID) != IdSource.ID_AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.EntityProperty))
        ret.add(property);
    }

    return ret;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  private static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : Entity.repository.getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.propertyID) && !(property instanceof Property.EntityProperty))
        ret.add(property);

    return ret;
  }
}
