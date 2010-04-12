/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.ValueProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A static utility class for constructing query criteria implementations and constructing
 * sql strings from criterias and property values
 */
public class CriteriaUtil {

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

  /**
   * @param database the Database instance
   * @return the condition string, i.e. "pkcol1 = value and pkcol2 = value2"
   */
  public static String getConditionString(final EntityKeyCriteria criteria, final Database database) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (criteria.getKeys().get(0).getPropertyCount() > 1) {//multiple column key
      //(a = b and c = d) or (a = g and c = d)
      for (int i = 0; i < criteria.getKeyCount(); i++) {
        stringBuilder.append(getQueryConditionString(database, criteria.getKeys().get(i), criteria.getColumnNames()));
        if (i < criteria.getKeyCount() - 1)
          stringBuilder.append(" or ");
      }
    }
    else {
      //a = b
      if (criteria.getKeyCount() == 1)
        stringBuilder.append(getQueryConditionString(database, criteria.getKeys().get(0), criteria.getColumnNames()));
      else //a in (c, v, d, s)
        appendInCondition(database, criteria.getProperties() != null ? criteria.getProperties().get(0).getColumnName()
                : criteria.getKeys().get(0).getFirstKeyProperty().getColumnName(), stringBuilder, criteria.getKeys());
    }

    return stringBuilder.toString();
  }

  /**
   * Constructs a query condition string from the given EntityKey, using the column names
   * provided or if none are provided, the column names from the key
   * @param database the Database instance
   * @param key the EntityKey instance
   * @param columnNames the column names to use in the criteria
   * @return a query condition string based on the given key and column names
   */
  public static String getQueryConditionString(final Database database, final Entity.Key key, final List<String> columnNames) {
    final StringBuilder stringBuilder = new StringBuilder("(");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : key.getProperties()) {
      stringBuilder.append(Util.getQueryString(columnNames == null ? property.getColumnName() : columnNames.get(i),
              getSQLStringValue(database, property, key.getValue(property.getPropertyID()))));
      if (i++ < key.getPropertyCount() - 1)
        stringBuilder.append(" and ");
    }

    return stringBuilder.append(")").toString();
  }

  public static void appendInCondition(final Database database, final String whereColumn, final StringBuilder stringBuilder, final List<Entity.Key> keys) {
    stringBuilder.append(whereColumn).append(" in (");
    final Property property = keys.get(0).getFirstKeyProperty();
    for (int i = 0, cnt = 1; i < keys.size(); i++, cnt++) {
      stringBuilder.append(getSQLStringValue(database, property, keys.get(i).getFirstKeyValue()));
      if (cnt == 1000 && i < keys.size() - 1) {//Oracle limit
        stringBuilder.append(") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < keys.size() - 1)
        stringBuilder.append(",");
    }
    stringBuilder.append(")");
  }

  /** {@inheritDoc} */
  public static String getConditionString(final PropertyCriteria criteria, final Database database) {
    if (criteria.getProperty() instanceof Property.ForeignKeyProperty)
      return getForeignKeyCriteriaString(criteria, database);

    final boolean isNullCriteria = criteria.getValueCount() == 1 &&
            Entity.isValueNull(criteria.getProperty().getPropertyType(), criteria.getValues().get(0));
    final String columnIdentifier = initializeColumnIdentifier(criteria, isNullCriteria);
    if (isNullCriteria)
      return columnIdentifier + (criteria.getSearchType() == SearchType.LIKE ? " is null" : " is not null");

    final String sqlValue = criteria.getSqlValue(getSQLStringValue(database, criteria.getProperty(), criteria.getValues().get(0)));
    final String sqlValue2 = criteria.getValueCount() == 2 ? criteria.getSqlValue(getSQLStringValue(database, criteria.getProperty(),
            criteria.getValues().get(1))) : null;

    switch(criteria.getSearchType()) {
      case LIKE:
        return getLikeCondition(criteria, database, columnIdentifier, sqlValue);
      case NOT_LIKE:
        return getNotLikeCondition(criteria, database, columnIdentifier, sqlValue);
      case AT_LEAST:
        return columnIdentifier + " <= " + sqlValue;
      case AT_MOST:
        return columnIdentifier + " >= " + sqlValue;
      case WITHIN_RANGE:
        return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
      case OUTSIDE_RANGE:
        return "(" + columnIdentifier + " <= " + sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
    }

    throw new IllegalArgumentException("Unknown search type" + criteria.getSearchType());
  }

  public static String getForeignKeyCriteriaString(final PropertyCriteria criteria, final Database database) {
    if (criteria.getValueCount() > 1)
      return getMultipleColumnForeignKeyCriteriaString(criteria, database);

    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
    final Entity.Key entityKey = (Entity.Key) criteria.getValues().get(0);
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) criteria.getProperty()).getReferencedEntityID());
    for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
      set.addCriteria(new PropertyCriteria(
              ((Property.ForeignKeyProperty) criteria.getProperty()).getReferenceProperties().get(keyProperty.getIndex()),
              criteria.getSearchType(), entityKey == null ? null : entityKey.getValue(keyProperty.getPropertyID())));

    return set.asString(database);
  }

  static String getMultipleColumnForeignKeyCriteriaString(final PropertyCriteria criteria, final Database database) {
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) criteria.getProperty()).getReferencedEntityID());
    if (primaryKeyProperties.size() > 1) {
      final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR);
      for (final Object entityKey : criteria.getValues()) {
        final CriteriaSet pkSet = new CriteriaSet(CriteriaSet.Conjunction.AND);
        for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
          pkSet.addCriteria(new PropertyCriteria(
                  ((Property.ForeignKeyProperty) criteria.getProperty()).getReferenceProperties().get(keyProperty.getIndex()),
                  criteria.getSearchType(), ((Entity.Key) entityKey).getValue(keyProperty.getPropertyID())));

        set.addCriteria(pkSet);
      }

      return set.asString(database);
    }
    else
      return getInList(criteria, database, ((Property.ForeignKeyProperty) criteria.getProperty()).getReferenceProperties().get(0).getColumnName(),
              criteria.getSearchType() == SearchType.NOT_LIKE);
  }

  public static String getNotLikeCondition(final PropertyCriteria criteria, final Database database, final String columnIdentifier, final String sqlValue) {
    return criteria.getValueCount() > 1 ? getInList(criteria, database, columnIdentifier, true) :
            columnIdentifier + (criteria.getProperty().getPropertyType() == Type.STRING && containsWildcard(criteria, sqlValue)
            ? " not like " + sqlValue : " <> " + sqlValue);
  }

  public static String getLikeCondition(final PropertyCriteria criteria, final Database database, final String columnIdentifier, final String sqlValue) {
    return criteria.getValueCount() > 1 ? getInList(criteria, database, columnIdentifier, false) :
            columnIdentifier + (criteria.getProperty().getPropertyType() == Type.STRING && containsWildcard(criteria, sqlValue)
            ? " like " + sqlValue : " = " + sqlValue);
  }

  static boolean containsWildcard(final PropertyCriteria criteria, final String val) {
    return val != null && val.length() > 0 && val.indexOf(criteria.getWildcard()) > -1;
  }

  public static String getInList(final PropertyCriteria criteria, final Database database, final String whereColumn, final boolean notIn) {
    final StringBuilder stringBuilder = new StringBuilder("(").append(whereColumn).append((notIn ? " not in (" : " in ("));
    int cnt = 1;
    for (int i = 0; i < criteria.getValues().size(); i++) {
      final String sqlValue = getSQLStringValue(database, criteria.getProperty(), criteria.getValues().get(i));
      if (criteria.getProperty().getPropertyType() == Type.STRING && !criteria.isCaseSensitive())
        stringBuilder.append("upper(").append(sqlValue).append(")");
      else
        stringBuilder.append(sqlValue);
      if (cnt++ == 1000 && i < criteria.getValueCount() - 1) {//Oracle limit
        stringBuilder.append(notIn ? ") and " : ") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < criteria.getValueCount() - 1)
        stringBuilder.append(", ");
    }
    stringBuilder.append("))");

    return stringBuilder.toString();
  }

  public static String initializeColumnIdentifier(final PropertyCriteria criteria, final boolean isNullCriteria) {
    String columnName;
    if (criteria.getProperty() instanceof Property.SubqueryProperty)
      columnName = "(" + ((Property.SubqueryProperty) criteria.getProperty()).getSubQuery() + ")";
    else
      columnName = criteria.getProperty().getColumnName();

    if (!isNullCriteria && criteria.getProperty().getPropertyType() == Type.STRING && !criteria.isCaseSensitive())
      columnName = "upper(" + columnName + ")";

    return columnName;
  }

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
      public Object getValue(final Object propertyID) {
        return entityKey.getValue((String) propertyID);
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
      public Object getValue(final Object propertyID) {
        return entity.getOriginalValue((String) propertyID);
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
      stringBuilder.append(Util.getQueryString(property.getPropertyID(),
              getSQLStringValue(database, property, valueProvider.getValue(property.getPropertyID()))));
      if (i++ < properties.size() - 1)
        stringBuilder.append(" and ");
    }

    return stringBuilder.append(")").toString();
  }
}
