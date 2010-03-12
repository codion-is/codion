/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class encapsulating a query criteria with EntityKey objects as values
 */
public class EntityKeyCriteria implements Criteria, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The keys used in this criteria
   */
  private final List<Entity.Key> keys;

  /**
   * The properties to use for column names when constructing the criteria string
   */
  private final List<Property> properties;

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys
   * @param keys the keys
   */
  public EntityKeyCriteria(final Entity.Key... keys) {
    this(Arrays.asList(keys));
  }

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys
   * @param keys the keys
   */
  public EntityKeyCriteria(final List<Entity.Key> keys) {
    this(null, keys);
  }

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys which uses the given properties
   * as column names when constructing the criteria string
   * @param properties the properties to use for column names when constructing the criteria string
   * @param keys the keys
   */
  public EntityKeyCriteria(final List<Property> properties, final List<Entity.Key> keys) {
    if (keys == null || keys.size() == 0)
      throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
    if (properties != null && properties.size() != keys.get(0).getPropertyCount())
      throw new IllegalArgumentException("Reference property count mismatch");

    this.keys = keys;
    this.properties = properties;
  }

  /**
   * @return the entityID
   */
  public String getEntityID() {
    return keys.get(0).getEntityID();
  }

  /** {@inheritDoc} */
  public String asString(final Database database) {
    return getConditionString(database);
  }

  /**
   * @param database the Database instance
   * @return the condition string, i.e. "pkcol1 = value and pkcol2 = value2"
   */
  public String getConditionString(final Database database) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (keys.get(0).getPropertyCount() > 1) {//multiple column key
      //(a = b and c = d) or (a = g and c = d)
      for (int i = 0; i < keys.size(); i++) {
        stringBuilder.append(getQueryConditionString(database, keys.get(i), getColumnNames()));
        if (i < keys.size() - 1)
          stringBuilder.append(" or ");
      }
    }
    else {
      //a = b
      if (keys.size() == 1)
        stringBuilder.append(getQueryConditionString(database, keys.get(0), getColumnNames()));
      else //a in (c, v, d, s)
        appendInCondition(database, properties != null ? properties.get(0).getColumnName()
                : keys.get(0).getFirstKeyProperty().getColumnName(), stringBuilder, keys);
    }

    return stringBuilder.toString();
  }

  private List<String> getColumnNames() {
    if (properties == null)
      return null;

    final List<String> columnNames = new ArrayList<String>(properties.size());
    for (final Property property : properties)
      columnNames.add(property.getColumnName());

    return columnNames;
  }

  /**
   * Constructs a query condition string from the given EntityKey, using the column names
   * provided or if none are provided, the column names from the key
   * @param database the Database instance
   * @param key the EntityKey instance
   * @param columnNames the column names to use in the criteria
   * @return a query condition string based on the given key and column names
   */
  private static String getQueryConditionString(final Database database, final Entity.Key key, final List<String> columnNames) {
    final StringBuilder stringBuilder = new StringBuilder("(");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : key.getProperties()) {
      stringBuilder.append(EntityUtil.getQueryString(columnNames == null ? property.getColumnName() : columnNames.get(i),
              EntityUtil.getSQLStringValue(database, property, key.getValue(property.getPropertyID()))));
      if (i++ < key.getPropertyCount() -1)
        stringBuilder.append(" and ");
    }

    return stringBuilder.append(")").toString();
  }

  private static void appendInCondition(final Database database, final String whereColumn, final StringBuilder stringBuilder, final List<Entity.Key> keys) {
    stringBuilder.append(whereColumn).append(" in (");
    final Property property = keys.get(0).getFirstKeyProperty();
    for (int i = 0, cnt = 1; i < keys.size(); i++, cnt++) {
      stringBuilder.append(EntityUtil.getSQLStringValue(database, property, keys.get(i).getFirstKeyValue()));
      if (cnt == 1000 && i < keys.size()-1) {//Oracle limit
        stringBuilder.append(") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < keys.size() - 1)
        stringBuilder.append(",");
    }
    stringBuilder.append(")");
  }
}
