/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaValueProvider;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A static utility class for constructing query criteria implementations
 */
public class CriteriaUtil {

  private static CriteriaValueProvider valueProvider;

  public static CriteriaValueProvider getCriteriaValueProvider() {
    if (valueProvider == null)
      valueProvider = new CriteriaValueProvider() {
        public String getSQLStringValue(final Database database, final Object columnKey, final Object value) {
          final Property property = (Property) columnKey;
          if (isValueNull(property, value))
            return "null";

          switch (property.getPropertyType()) {
            case INT:
            case DOUBLE:
              return value.toString();//localize?
            case TIMESTAMP:
              if (!(value instanceof Date))
                throw new IllegalArgumentException("Date value expected for: " + columnKey + ", got: " + value.getClass());
              return database.getSQLDateString((Date) value, true);
            case DATE:
              if (!(value instanceof Date))
                throw new IllegalArgumentException("Date value expected for: " + columnKey + ", got: " + value.getClass());
              return database.getSQLDateString((Date) value, false);
            case CHAR:
              return "'" + value + "'";
            case STRING:
              if (!(value instanceof String))
                throw new IllegalArgumentException("String value expected for: " + columnKey + ", got: " + value.getClass());
              return "'" + Util.sqlEscapeString((String) value) + "'";
            case BOOLEAN:
              if (!(value instanceof Boolean))
                throw new IllegalArgumentException("Boolean value expected for property: " + property + ", got: " + value.getClass());
              if (property instanceof Property.BooleanProperty)
                return ((Property.BooleanProperty) property).toSQLString((Boolean) value);
              else
                return getBooleanSQLString((Boolean) value);
            case ENTITY:
              return value instanceof Entity ? getSQLStringValue(database, columnKey, ((Entity) value).getPrimaryKey().getFirstKeyValue())
                      : getSQLStringValue(database, ((Entity.Key) value).getFirstKeyProperty(), ((Entity.Key) value).getFirstKeyValue());
            default:
              throw new IllegalArgumentException("Undefined property type: " + property.getPropertyType());
          }
        }

        public boolean isValueNull(final Object columnKey, final Object value) {
          return Entity.isValueNull(((Property) columnKey).getPropertyType(), value);
        }
      };

    return valueProvider;
  }

  public static String getBooleanSQLString(final Boolean value) {
    if (value == null)
      return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_NULL).toString();
    else if (value)
      return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE).toString();
    else
      return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE).toString();
  }

  public static SelectCriteria selectCriteria(final Entity.Key key) {
    return selectCriteria(Arrays.asList(key));
  }

  public static SelectCriteria selectCriteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new SelectCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values));
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final int fetchCount,
                                              final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values), fetchCount);
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final String orderByClause,
                                              final int fetchCount, final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values), orderByClause, fetchCount);
  }

  public static EntityCriteria criteria(final Entity.Key key) {
    return criteria(Arrays.asList(key));
  }

  public static EntityCriteria criteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new EntityCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static EntityCriteria criteria(final String entityID, final String propertyID,
                                        final SearchType searchType, final Object... values) {
    return new EntityCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values));
  }
}
