/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class encapsulating a query criteria with EntityKey objects as values.
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

  public int getKeyCount() {
    return getKeys().size();
  }

  public List<String> getColumnNames() {
    if (properties == null)
      return null;

    final List<String> columnNames = new ArrayList<String>(properties.size());
    for (final Property property : properties)
      columnNames.add(property.getColumnName());

    return columnNames;
  }

  public List<Entity.Key> getKeys() {
    return keys;
  }

  public List<Property> getProperties() {
    return properties;
  }

  /**
   * @return the entityID
   */
  public String getEntityID() {
    return keys.get(0).getEntityID();
  }

  /** {@inheritDoc} */
  public String asString(final Database database) {
    return CriteriaUtil.getConditionString(this, database);
  }
}
