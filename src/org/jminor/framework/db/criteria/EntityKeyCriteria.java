/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class encapsulating a query criteria with Entity.Key objects as values.
 */
public class EntityKeyCriteria extends CriteriaSet {

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
    super(Conjunction.OR);
    if (keys == null || keys.size() == 0)
      throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
    if (properties != null && properties.size() != keys.get(0).getPropertyCount())
      throw new IllegalArgumentException("Reference property count mismatch");

    this.keys = keys;
    this.properties = properties;
    setupCriteria();
  }

  private void setupCriteria() {
    if (keys.get(0).getPropertyCount() > 1) {//multiple column key
      final List<? extends Property> propertyList = properties == null ? keys.get(0).getProperties() : properties;
      final List<Property.PrimaryKeyProperty> pkProperties = keys.get(0).getProperties();
      //(a = b and c = d) or (a = g and c = d)
      for (final Entity.Key key : keys) {
        final CriteriaSet andSet = new CriteriaSet(Conjunction.AND);
        int i = 0;
        for (final Property property : propertyList)
          andSet.addCriteria(new PropertyCriteria(property, SearchType.LIKE,
                  key.getValue(pkProperties.get(i++).getPropertyID())));
 
        addCriteria(andSet);
      }
    }
    else {
      final Property property = properties == null ? keys.get(0).getFirstKeyProperty() : properties.get(0);
      //a = b
      if (keys.size() == 1)
        addCriteria(new PropertyCriteria(property, SearchType.LIKE, keys.get(0).getFirstKeyValue()));
      else //a in (c, v, d, s)
        addCriteria(new PropertyCriteria(property, SearchType.LIKE, EntityUtil.getPropertyValues(keys)));
    }
  }

  public List<String> getColumnNames() {
    if (properties == null)
      return null;

    final List<String> columnNames = new ArrayList<String>(properties.size());
    for (final Property property : properties)
      columnNames.add(property.getColumnName());

    return columnNames;
  }

  /**
   * @return the entityID
   */
  public String getEntityID() {
    return keys.get(0).getEntityID();
  }
}
