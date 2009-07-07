/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.ICriteria;
import org.jminor.framework.db.EntityDbUtil;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class encapsulating a query criteria with EntityKey objects as values
 */
public class EntityKeyCriteria implements ICriteria, Serializable {

  private static final long serialVersionUID = 1;

  private final List<EntityKey> keys;
  private final List<Property> properties;

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys
   * @param keys the keys
   */
  public EntityKeyCriteria(final EntityKey... keys) {
    this(null, keys);
  }

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys
   * @param keys the keys
   */
  public EntityKeyCriteria(final Collection<EntityKey> keys) {
    this(null, keys.toArray(new EntityKey[keys.size()]));
  }

  /**
   * Instantiates a new EntityKeyCriteria comprised of the given keys which uses the given properties
   * as column names when constructing the criteria string
   * @param properties the properties to use for column names when constructing the criteria string
   * @param keys the keys
   */
  public EntityKeyCriteria(final List<Property> properties, final EntityKey... keys) {
    if (keys == null || keys.length == 0)
      throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
    if (properties != null && properties.size() != keys[0].getPropertyCount())
      throw new IllegalArgumentException("Reference property count mismatch");

    this.properties = properties;
    this.keys = new ArrayList<EntityKey>(Arrays.asList(keys));
  }

  /**
   * @return the keys
   */
  public List<EntityKey> getKeys() {
    return keys;
  }

  /**
   * @return the entityID
   */
  public String getEntityID() {
    return keys.get(0).getEntityID();
  }

  /**
   * @return the properties used for column names when constructing the criteria string
   */
  public List<Property> getProperties() {
    return properties;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getConditionString();
  }

  /**
   * @return the condition string, i.e. "pkcol1 = value and pkcol2 = value2"
   */
  public String getConditionString() {
    final StringBuffer ret = new StringBuffer();
    if (keys.get(0).getPropertyCount() > 1) {//multi column key
      //(a = b and c = d) or (a = g and c = d)
      for (int i = 0; i < keys.size(); i++) {
        ret.append(getQueryConditionString(keys.get(i), getColumnNames()));
        if (i < keys.size() - 1)
          ret.append(" or ");
      }
    }
    else {
      //a = b
      if (keys.size() == 1)
        ret.append(getQueryConditionString(keys.get(0), getColumnNames()));
      else //a in (c, v, d, s)
        appendInCondition(properties != null ? properties.get(0).propertyID
                : keys.get(0).getKeyColumnNames()[0], ret, keys);
    }

    return ret.toString();
  }

  private List<String> getColumnNames() {
    if (properties == null)
      return null;

    final List<String> ret = new ArrayList<String>(properties.size());
    for (final Property property : properties)
      ret.add(property.propertyID);

    return ret;
  }

  /**
   * Constructs a query condition string from the given EntityKey, using the column names
   * provided or if none are provided, the column names from the key
   * @param key the EntityKey instance
   * @param columnNames the column names to use in the criteria
   * @return a query condition string based on the given key and column names
   */
  private static String getQueryConditionString(final EntityKey key, final List<String> columnNames) {
    final StringBuffer ret = new StringBuffer("(");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : key.getProperties()) {
      ret.append(EntityDbUtil.getQueryString(columnNames == null ? property.propertyID : columnNames.get(i),
              EntityDbUtil.getSQLStringValue(property, key.getValue(property.propertyID))));
      if (i++ < key.getPropertyCount() -1)
        ret.append(" and ");
    }

    return ret.append(")").toString();
  }

  private static void appendInCondition(final String whereColumn, final StringBuffer ret, final List<EntityKey> keys) {
    ret.append(whereColumn).append(" in (");
    final Property property = keys.get(0).getFirstKeyProperty();
    for (int i = 0, cnt = 1; i < keys.size(); i++, cnt++) {
      ret.append(EntityDbUtil.getSQLStringValue(property, keys.get(i).getFirstKeyValue()));
      if (cnt == 1000 && i < keys.size()-1) {//Oracle limit
        ret.append(") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < keys.size() - 1)
        ret.append(",");
    }
    ret.append(")");
  }
}
