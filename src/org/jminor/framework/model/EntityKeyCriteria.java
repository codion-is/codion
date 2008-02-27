/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.ICriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class encapsulating a query criteria with EntityKey objects as values
 */
public class EntityKeyCriteria implements ICriteria {

  private final List<EntityKey> keys;
  private List<Property> properties;

  public EntityKeyCriteria(final EntityKey... keys) {
    this(null, keys);
  }

  public EntityKeyCriteria(final Collection<EntityKey> keys) {
    this(null, keys.toArray(new EntityKey[keys.size()]));
  }

  public EntityKeyCriteria(final List<Property> properties, final EntityKey... keys) {
    if (keys == null || keys.length == 0)
      throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
    if (properties != null && properties.size() != keys[0].getPropertyCount())
      throw new IllegalArgumentException("Reference property count mismatch");

    this.properties = properties;
    this.keys = new ArrayList<EntityKey>(Arrays.asList(keys));
  }

  public List<EntityKey> getKeys() {
    return keys;
  }

  public String getEntityID() {
    return keys.get(0).entityID;
  }

  public List<Property> getProperties() {
    return properties;
  }

  /** {@inheritDoc} */
  public String toString() {
    return getConditionString();
  }

  public String getConditionString() {
    final StringBuffer ret = new StringBuffer();
    final boolean multiColumnPk = keys.get(0).getPropertyCount() > 1;
    if (multiColumnPk) {
      //(a = b and c = d) or (a = g and c = d)
      for (int i = 0; i < keys.size(); i++) {
        ret.append(EntityUtil.getQueryConditionString(keys.get(i), getColumnNames()));
        if (i < keys.size() - 1)
          ret.append(" or ");
      }
    }
    else {
      //a = b
      if (keys.size() == 1)
        ret.append(EntityUtil.getQueryConditionString(keys.get(0), getColumnNames()));
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

  private void appendInCondition(final String whereColumn, final StringBuffer ret, final List<EntityKey> keys) {
    ret.append(whereColumn).append(" in (");
    final Property property = keys.get(0).getFirstKeyProperty();
    for (int i = 0, cnt = 1; i < keys.size(); i++, cnt++) {
      ret.append(EntityUtil.getSQLStringValue(property, keys.get(i).getFirstKeyValue()));
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
