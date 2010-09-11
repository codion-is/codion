/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A factory class for query criteria implementations.
 */
public final class EntityCriteriaUtil {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";

  private EntityCriteriaUtil() {}

  /**
   * @param key the key
   * @return a select criteria based on the given key
   */
  public static EntitySelectCriteria selectCriteria(final Entity.Key key) {
    return selectCriteria(Arrays.asList(key));
  }

  /**
   * @param keys the keys
   * @return a select criteria based on the given keys
   */
  public static EntitySelectCriteria selectCriteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new DefaultEntitySelectCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param values the criteria values
   * @return a select criteria based on the given values
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final Object... values) {
    return selectCriteria(entityID, propertyID, searchType, -1, values);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param fetchCount the maximum number of entities to fetch
   * @param values the criteria values
   * @return a select criteria based on the given values
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final int fetchCount,
                                                    final Object... values) {
    return selectCriteria(entityID, propertyID, searchType, null, fetchCount, values);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param orderByClause the order by clause
   * @param fetchCount the maximum number of entities to fetch
   * @param values the criteria values
   * @return a select criteria based on the given values
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final String orderByClause,
                                                    final int fetchCount, final Object... values) {

    final Property property = Entities.getProperty(entityID, propertyID);
    final Criteria<Property.ColumnProperty> criteria;
    if (property instanceof Property.ForeignKeyProperty) {
      criteria = new ForeignKeyCriteria((Property.ForeignKeyProperty) property, searchType, values);
    }
    else {
      criteria = new PropertyCriteria((Property.ColumnProperty) property, searchType, values);
    }

    return new DefaultEntitySelectCriteria(entityID, criteria, orderByClause, fetchCount);
  }

  /**
   * @param entityID the entity ID
   * @param orderByClause the order by clause
   * @return a select criteria including all entities of the given type
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String orderByClause) {
    return new DefaultEntitySelectCriteria(entityID, null, orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyReferenceProperties the foreign key reference properties
   * @param primaryKeys the primary keys referenced by the given properties
   * @return a select criteria based on the given values
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final List<Property.ColumnProperty> foreignKeyReferenceProperties,
                                                    final List<Entity.Key> primaryKeys) {
    final EntityKeyCriteria entityKeyCriteria = new EntityKeyCriteria(foreignKeyReferenceProperties, primaryKeys);
    return new DefaultEntitySelectCriteria(entityID, entityKeyCriteria);
  }

  /**
   * @param entityID the entity ID
   * @param criteria the column criteria
   * @param orderByClause the order by clause
   * @return a select criteria based on the given column criteria
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria,
                                                    final String orderByClause) {
    return selectCriteria(entityID, criteria, orderByClause, -1);
  }


  /**
   * @param entityID the entity ID
   * @param propertyCriteria the column criteria
   * @param orderByClause the order by clause
   * @param fetchCount the maximum number of entities to fetch
   * @return a select criteria based on the given column criteria
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> propertyCriteria,
                                                    final String orderByClause, final int fetchCount) {
    return new DefaultEntitySelectCriteria(entityID, propertyCriteria, orderByClause, fetchCount);
  }

  /**
   * @param entityID the entity ID
   * @return a select criteria encompassing all entities of the given type
   */
  public static EntitySelectCriteria selectCriteria(final String entityID) {
    return new DefaultEntitySelectCriteria(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param fetchCount the maximum number of entities to fetch
   * @return a select criteria encompassing all entities of the given type
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final int fetchCount) {
    return new DefaultEntitySelectCriteria(entityID, null, fetchCount);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCriteria the column criteria
   * @return a select criteria based on the given column criteria
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> propertyCriteria) {
    return new DefaultEntitySelectCriteria(entityID, propertyCriteria);
  }

  /**
   * @param key the primary key
   * @return a criteria specifying the entity having the given primary key
   */
  public static EntityCriteria criteria(final Entity.Key key) {
    return criteria(Arrays.asList(key));
  }

  /**
   * @param keys the primary keys
   * @return a criteria specifying the entities having the given primary keys
   */
  public static EntityCriteria criteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new DefaultEntityCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  /**
   * @param entityID the entity ID
   * @return a criteria specifying all entities of the given type
   */
  public static EntityCriteria criteria(final String entityID) {
    return new DefaultEntityCriteria(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param values the criteria values
   * @return a criteria based on the given values
   */
  public static EntityCriteria criteria(final String entityID, final String propertyID,
                                        final SearchType searchType, final Object... values) {
    return new DefaultEntityCriteria(entityID, new PropertyCriteria((Property.ColumnProperty) Entities.getProperty(entityID, propertyID),
            searchType, values));
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID, final SearchType searchType,
                                                                   final Object... values) {
    return propertyCriteria(entityID, propertyID, true, searchType, values);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param caseSensitive true if the criteria should be case sensitive, only applicable to string properties
   * @param searchType the search type
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID, final boolean caseSensitive,
                                                                   final SearchType searchType, final Object... values) {
    return propertyCriteria((Property.ColumnProperty) Entities.getProperty(entityID, propertyID), caseSensitive, searchType, values);
  }

  /**
   * @param property the property
   * @param searchType the search type
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final SearchType searchType,
                                                                   final Object... values) {
    return propertyCriteria(property, true, searchType, values);
  }

  /**
   * @param property the property
   * @param searchType the search type
   * @param caseSensitive true if the criteria should be case sensitive, only applicable to string properties
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final boolean caseSensitive, final SearchType searchType,
                                                                   final Object... values) {
    return new PropertyCriteria(property, searchType, values).setCaseSensitive(caseSensitive);
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param searchType the search type
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                     final SearchType searchType, final Object... values) {
    return new ForeignKeyCriteria(foreignKeyProperty, searchType, values);
  }

  /**
   * @param entityID the entity ID
   * @param criteria the column criteria
   * @return a criteria based on the given column criteria
   */
  public static EntityCriteria criteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
    return new DefaultEntityCriteria(entityID, criteria);
  }

  private static class DefaultEntityCriteria implements EntityCriteria, Serializable {

    private static final long serialVersionUID = 1;

    private String entityID;
    private Criteria<Property.ColumnProperty> criteria;

    DefaultEntityCriteria() {}

    /**
     * Instantiates a new empty EntityCriteria.
     * Using an empty criteria means all underlying records should be selected
     * @param entityID the ID of the entity to select
     */
    private DefaultEntityCriteria(final String entityID) {
      this(entityID, null);
    }

    /**
     * Instantiates a new EntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    private DefaultEntityCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
      Util.rejectNullValue(entityID, "entityID");
      this.entityID = entityID;
      this.criteria = criteria;
    }

    /** {@inheritDoc} */
    public final List<Object> getValues() {
      return criteria == null ? null : criteria.getValues();
    }

    /** {@inheritDoc} */
    public final List<Property.ColumnProperty> getValueProperties() {
      return criteria == null ? null : criteria.getValueKeys();
    }

    /** {@inheritDoc} */
    public final String getEntityID() {
      return entityID;
    }

    /** {@inheritDoc} */
    public final Criteria<Property.ColumnProperty> getCriteria() {
      return criteria;
    }

    /**
     * Returns a where condition based on this EntityCriteria
     * @return a where condition based on this EntityCriteria
     */
    public String asString() {
      return Entities.getTableName(entityID) + " " + getWhereClause();
    }

    /** {@inheritDoc} */
    public final String getWhereClause() {
      return getWhereClause(true);
    }

    /** {@inheritDoc} */
    public final String getWhereClause(final boolean includeWhereKeyword) {
      final String criteriaString = criteria == null ? "" : criteria.asString();

      return !criteriaString.isEmpty() ? (includeWhereKeyword ? "where " : "and ") + criteriaString : "";
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entityID);
      stream.writeObject(criteria);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      criteria = (Criteria<Property.ColumnProperty>) stream.readObject();
    }
  }

  private static final class DefaultEntitySelectCriteria implements EntitySelectCriteria, Serializable {

    private static final long serialVersionUID = 1;

    private EntityCriteria criteria;
    private Map<String, Integer> foreignKeyFetchDepthLimits;

    private String orderByClause;
    private int fetchCount;
    private boolean selectForUpdate;

    DefaultEntitySelectCriteria() {}

    /**
     * Instantiates a new DefaultEntityCriteria, which includes all the underlying entities
     * @param entityID the ID of the entity to select
     */
    private DefaultEntitySelectCriteria(final String entityID) {
      this(entityID, null);
    }

    /**
     * Instantiates a new DefaultEntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    private DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
      this(entityID, criteria, null);
    }

    /**
     * Instantiates a new DefaultEntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    private DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final String orderByClause) {
      this(entityID, criteria, orderByClause, -1);
    }

    /**
     * Instantiates a new DefaultEntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param fetchCount the maximum number of records to fetch from the result
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    private DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final int fetchCount) {
      this(entityID, criteria, null, fetchCount);
    }

    /**
     * Instantiates a new DefaultEntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
     * @param fetchCount the maximum number of records to fetch from the result
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    private DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final String orderByClause,
                                        final int fetchCount) {
      this.criteria = new DefaultEntityCriteria(entityID, criteria);
      this.fetchCount = fetchCount;
      this.orderByClause = orderByClause;
    }

    /** {@inheritDoc} */
    public Criteria<Property.ColumnProperty> getCriteria() {
      return criteria.getCriteria();
    }

    /** {@inheritDoc} */
    public String getEntityID() {
      return criteria.getEntityID();
    }

    /** {@inheritDoc} */
    public List<Property.ColumnProperty> getValueProperties() {
      return criteria.getValueProperties();
    }

    /** {@inheritDoc} */
    public List<Object> getValues() {
      return criteria.getValues();
    }

    /** {@inheritDoc} */
    public String getWhereClause() {
      return criteria.getWhereClause();
    }

    /** {@inheritDoc} */
    public String getWhereClause(final boolean includeWhereKeyword) {
      return criteria.getWhereClause(includeWhereKeyword);
    }

    /** {@inheritDoc} */
    public int getFetchCount() {
      return fetchCount;
    }

    /** {@inheritDoc} */
    public String getOrderByClause() {
      return orderByClause;
    }

    /** {@inheritDoc} */
    public EntitySelectCriteria setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int fetchDepthLimit) {
      if (foreignKeyFetchDepthLimits == null) {
        foreignKeyFetchDepthLimits = new HashMap<String, Integer>();
      }
      this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyID, fetchDepthLimit);
      return this;
    }

    /** {@inheritDoc} */
    public int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyID) {
      if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyID)) {
        return foreignKeyFetchDepthLimits.get(foreignKeyPropertyID);
      }

      return Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID).getFetchDepth();
    }

    /** {@inheritDoc} */
    public EntitySelectCriteria setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final Collection<Property.ForeignKeyProperty > properties = Entities.getForeignKeyProperties(getEntityID());
      for (final Property.ForeignKeyProperty property : properties) {
        setForeignKeyFetchDepthLimit(property.getPropertyID(), fetchDepthLimit);
      }

      return this;
    }

    /** {@inheritDoc} */
    public boolean isSelectForUpdate() {
      return selectForUpdate;
    }

    /** {@inheritDoc} */
    public EntitySelectCriteria setSelectForUpdate(final boolean selectForUpdate) {
      this.selectForUpdate = selectForUpdate;
      return this;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(orderByClause);
      stream.writeInt(fetchCount);
      stream.writeBoolean(selectForUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(criteria);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      orderByClause = (String) stream.readObject();
      fetchCount = stream.readInt();
      selectForUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimits = (Map<String, Integer>) stream.readObject();
      criteria = (EntityCriteria) stream.readObject();
    }
  }

  /**
   * A class encapsulating a query criteria with Entity.Key objects as values.
   */
  private static final class EntityKeyCriteria implements Criteria<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    private String entityID;
    private CriteriaSet<Property.ColumnProperty> criteria;

    EntityKeyCriteria() {}

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys
     * @param keys the keys
     */
    private EntityKeyCriteria(final List<Entity.Key> keys) {
      this(null, keys);
    }

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys which uses the given properties
     * as column names when constructing the criteria string
     * @param properties the properties to use for column names when constructing the criteria string
     * @param keys the keys
     */
    private EntityKeyCriteria(final List<Property.ColumnProperty> properties, final List<Entity.Key> keys) {
      criteria = new CriteriaSet<Property.ColumnProperty>(Conjunction.OR);
      Util.rejectNullValue(keys, "keys");
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
      }
      if (properties != null && properties.size() != keys.get(0).getPropertyCount()) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }
      entityID = keys.get(0).getEntityID();
      setupCriteria(properties, keys);
    }

    /** {@inheritDoc} */
    public String asString() {
      return criteria.asString();
    }

    /** {@inheritDoc} */
    public List<Property.ColumnProperty> getValueKeys() {
      return criteria.getValueKeys();
    }

    /** {@inheritDoc} */
    public List<Object> getValues() {
      return criteria.getValues();
    }

    /**
     * @return the entityID
     */
    private String getEntityID() {
      return entityID;
    }

    private void setupCriteria(final List<Property.ColumnProperty> properties, final List<Entity.Key> keys) {
      if (keys.get(0).isCompositeKey()) {//multiple column key
        final List<Property.PrimaryKeyProperty> pkProperties = keys.get(0).getProperties();
        final List<? extends Property.ColumnProperty> propertyList = properties == null ? pkProperties : properties;
        //(a = b and c = d) or (a = g and c = d)
        for (final Entity.Key key : keys) {
          final CriteriaSet<Property.ColumnProperty> andSet = new CriteriaSet<Property.ColumnProperty>(Conjunction.AND);
          int i = 0;
          for (final Property.ColumnProperty property : propertyList) {
            andSet.add(new PropertyCriteria(property, SearchType.LIKE, key.getValue(pkProperties.get(i++).getPropertyID())));
          }

          criteria.add(andSet);
        }
      }
      else {
        final Property.ColumnProperty property = properties == null ? keys.get(0).getFirstKeyProperty() : properties.get(0);
        final Property primaryKeyProperty = properties == null ? property : keys.get(0).getFirstKeyProperty();
        //a = b
        if (keys.size() == 1) {
          final Entity.Key key = keys.get(0);
          criteria.add(new PropertyCriteria(property, SearchType.LIKE, key.getValue(primaryKeyProperty.getPropertyID())));
        }
        else { //a in (c, v, d, s)
          criteria.add(new PropertyCriteria(property, SearchType.LIKE, EntityUtil.getPropertyValues(keys)));
        }
      }
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entityID);
      stream.writeObject(criteria);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      criteria = (CriteriaSet<Property.ColumnProperty>) stream.readObject();
    }
  }

  /**
   * A object for encapsulating a query criteria with a single property and one or more values.
   */
  private static final class PropertyCriteria implements Criteria<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this criteria
     */
    private Property.ColumnProperty property;

    /**
     * The values used in this criteria
     */
    private List<Object> values;

    /**
     * True if this criteria tests for null
     */
    private boolean isNullCriteria;

    /**
     * The search type used in this criteria
     */
    private SearchType searchType;

    /**
     * True if this criteria should be case sensitive, only applies to criteria based on string properties
     */
    private boolean caseSensitive = true;

    PropertyCriteria() {}

    /**
     * Instantiates a new PropertyCriteria instance
     * @param property the property
     * @param searchType the search type
     * @param values the values
     */
    private PropertyCriteria(final Property.ColumnProperty property, final SearchType searchType, final Object... values) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      if (values != null && values.length == 0) {
        throw new IllegalArgumentException("No values specified for PropertyCriteria: " + property);
      }
      this.property = property;
      this.searchType = searchType;
      this.values = initializeValues(values);
      this.isNullCriteria = this.values.size() == 1 && this.values.get(0) == null;
    }

    /** {@inheritDoc} */
    public List<Object> getValues() {
      if (isNullCriteria) {
        return new ArrayList<Object>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return values;
    }

    /** {@inheritDoc} */
    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return new ArrayList<Property.ColumnProperty>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    /** {@inheritDoc} */
    public String asString() {
      return getConditionString();
    }

    /**
     * @return the number values contained in this criteria.
     */
    private int getValueCount() {
      return getValues().size();
    }

    /**
     * Sets whether this criteria should be case sensitive, only applies to criteria based on string properties
     * @param caseSensitive if true then this criteria is case sensitive, false otherwise
     * @return this PropertyCriteria instance
     */
    private PropertyCriteria setCaseSensitive(final boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    private String getConditionString() {
      final String columnIdentifier = initializeColumnIdentifier(property.isString());
      if (isNullCriteria) {
        return columnIdentifier + (searchType == SearchType.LIKE ? " is null" : " is not null");
      }

      final String sqlValue = getSqlValue("?");
      final String sqlValue2 = getValueCount() == 2 ? getSqlValue("?") : null;

      switch(searchType) {
        case LIKE:
          return getLikeCondition(columnIdentifier, sqlValue);
        case NOT_LIKE:
          return getNotLikeCondition(columnIdentifier, sqlValue);
        case AT_LEAST:
          return columnIdentifier + " <= " + sqlValue;
        case AT_MOST:
          return columnIdentifier + " >= " + sqlValue;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
      }

      throw new IllegalArgumentException("Unknown search type" + searchType);
    }

    private String getSqlValue(final String sqlStringValue) {
      return property.isString() && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
    }

    private String getNotLikeCondition(final String columnIdentifier, final String likeValue) {
      return getValueCount() > 1 ? getInList(true) :
              columnIdentifier + (property.isString() ? " not like "  + likeValue: " <> " + likeValue);
    }

    private String getInList(final boolean notIn) {
      final boolean isStringProperty = property.isString();
      final StringBuilder stringBuilder = new StringBuilder("(").append(initializeColumnIdentifier(isStringProperty)).append((notIn ? " not in (" : IN_PREFIX));
      int cnt = 1;
      for (int i = 0; i < getValueCount(); i++) {
        if (isStringProperty && !caseSensitive) {
          stringBuilder.append("upper(?)");
        }
        else {
          stringBuilder.append("?");
        }
        if (cnt++ == IN_CLAUSE_LIMIT && i < getValueCount() - 1) {
          stringBuilder.append(notIn ? ") and " : ") or ").append(property.getColumnName()).append(IN_PREFIX);
          cnt = 1;
        }
        else if (i < getValueCount() - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }

    private String getLikeCondition(final String columnIdentifier, final String likeValue) {
      return getValueCount() > 1 ? getInList(false) : columnIdentifier +
              (property.isString() ? " like " + likeValue : " = " + likeValue);
    }

    private String initializeColumnIdentifier(final boolean isStringProperty) {
      String columnName;
      if (property instanceof Property.SubqueryProperty) {
        columnName = "(" + ((Property.SubqueryProperty) property).getSubQuery() + ")";
      }
      else {
        columnName = property.getColumnName();
      }

      if (!isNullCriteria && isStringProperty && !caseSensitive) {
        columnName = "upper(" + columnName + ")";
      }

      return columnName;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(property.getEntityID());
      stream.writeObject(property.getPropertyID());
      stream.writeObject(searchType);
      stream.writeBoolean(isNullCriteria);
      stream.writeBoolean(caseSensitive);
      stream.writeInt(values.size());
      for (final Object value : values) {
        stream.writeObject(value);
      }
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String entityID = (String) stream.readObject();
      final String propertyID = (String) stream.readObject();
      property = (Property.ColumnProperty) Entities.getProperty(entityID, propertyID);
      searchType = (SearchType) stream.readObject();
      isNullCriteria = stream.readBoolean();
      caseSensitive = stream.readBoolean();
      final int valueCount = stream.readInt();
      values = new ArrayList<Object>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
    }

    /**
     * @param values the values to use in this criteria
     * @return a list containing the values
     */
    @SuppressWarnings({"unchecked"})
    private static List<Object> initializeValues(final Object... values) {
      final List<Object> ret = new ArrayList<Object>();
      if (values == null) {
        ret.add(null);
      }
      else {
        ret.addAll(getValueList(values));
      }

      return ret;
    }

    @SuppressWarnings({"unchecked"})
    private static Collection getValueList(final Object... values) {
      if (values.length == 1 && values[0] instanceof Collection) {
        return getValueList(((Collection) values[0]).toArray());
      }
      else {
        return Arrays.asList(values);
      }
    }
  }

  private static final class ForeignKeyCriteria implements Criteria<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this criteria
     */
    private Property.ForeignKeyProperty property;

    /**
     * The values used in this criteria
     */
    private List<Entity.Key> values;
    private SearchType searchType;
    private boolean isNullCriteria;

    private ForeignKeyCriteria(final Property.ForeignKeyProperty property, final SearchType searchType, final Object... values) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      if (values != null && values.length == 0) {
        throw new IllegalArgumentException("No values specified for ForeignKeyPropertyCriteria: " + property);
      }
      this.property = property;
      this.searchType = searchType;
      this.values = initializeValues(values);
      this.isNullCriteria = this.values.size() == 1 && this.values.get(0) == null;
    }

    /** {@inheritDoc} */
    public String asString() {
      return getForeignKeyCriteriaString();
    }

    /** {@inheritDoc} */
    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return new ArrayList<Property.ColumnProperty>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return getForeignKeyValueProperties();
    }

    /** {@inheritDoc} */
    public List<Object> getValues() {
      if (isNullCriteria) {
        return new ArrayList<Object>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return getForeignKeyCriteriaValues();
    }

    private String getForeignKeyCriteriaString() {
      if (getValues().size() > 1) {
        return getMultipleForeignKeyCriteriaString();
      }

      return createSingleForeignKeyCriteria(values.get(0)).asString();
    }

    private String getMultipleForeignKeyCriteriaString() {
      if (property.isCompositeReference()) {
        return createMultipleCompositeForeignKeyCriteria().asString();
      }
      else {
        return getInList(property.getReferenceProperties().get(0), searchType == SearchType.NOT_LIKE);
      }
    }

    private List<Object> getForeignKeyCriteriaValues() {
      if (values.size() > 1) {
        return getCompositeForeignKeyCriteriaValues();
      }

      return createSingleForeignKeyCriteria(values.get(0)).getValues();
    }

    private List<Object> getCompositeForeignKeyCriteriaValues() {
      return createMultipleCompositeForeignKeyCriteria().getValues();
    }

    private List<Property.ColumnProperty> getForeignKeyValueProperties() {
      if (values.size() > 1) {
        return createMultipleCompositeForeignKeyCriteria().getValueKeys();
      }

      return createSingleForeignKeyCriteria(values.get(0)).getValueKeys();
    }

    private Criteria<Property.ColumnProperty> createMultipleCompositeForeignKeyCriteria() {
      final CriteriaSet<Property.ColumnProperty> criteriaSet = new CriteriaSet<Property.ColumnProperty>(Conjunction.OR);
      for (final Object entityKey : values) {
        criteriaSet.add(createSingleForeignKeyCriteria((Entity.Key) entityKey));
      }

      return criteriaSet;
    }

    private Criteria<Property.ColumnProperty> createSingleForeignKeyCriteria(final Entity.Key entityKey) {
      final Property.ForeignKeyProperty foreignKeyProperty = property;
      if (foreignKeyProperty.isCompositeReference()) {
        final CriteriaSet<Property.ColumnProperty> pkSet = new CriteriaSet<Property.ColumnProperty>(Conjunction.AND);
        for (final Property.ColumnProperty referencedProperty : foreignKeyProperty.getReferenceProperties()) {
          final String referencedPropertyID = foreignKeyProperty.getReferencedPropertyID(referencedProperty);
          final Object referencedValue = entityKey == null ? null : entityKey.getValue(referencedPropertyID);
          pkSet.add(new PropertyCriteria(referencedProperty, searchType, referencedValue));
        }

        return pkSet;
      }
      else {
        return new PropertyCriteria(foreignKeyProperty.getReferenceProperties().get(0), searchType, entityKey == null ? null : entityKey.getFirstKeyValue());
      }
    }

    private String getInList(final Property.ColumnProperty property, final boolean notIn) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(property.getColumnName()).append((notIn ? " not in (" : IN_PREFIX));
      int cnt = 1;
      for (int i = 0; i < getValues().size(); i++) {
        stringBuilder.append("?");
        if (cnt++ == IN_CLAUSE_LIMIT && i < getValues().size() - 1) {
          stringBuilder.append(notIn ? ") and " : ") or ").append(property.getColumnName()).append(IN_PREFIX);
          cnt = 1;
        }
        else if (i < getValues().size() - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeBoolean(isNullCriteria);
      stream.writeObject(property.getEntityID());
      stream.writeObject(property.getPropertyID());
      stream.writeObject(searchType);
      stream.writeInt(values.size());
      for (final Entity.Key key : values) {
        stream.writeObject(key);
      }
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      isNullCriteria = stream.readBoolean();
      final String entityID = (String) stream.readObject();
      final String propertyID = (String) stream.readObject();
      property = (Property.ForeignKeyProperty) Entities.getProperty(entityID, propertyID);
      searchType = (SearchType) stream.readObject();
      final int valueCount = stream.readInt();
      values = new ArrayList<Entity.Key>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add((Entity.Key) stream.readObject());
      }
    }

    /**
     * @param values the values to use in this criteria
     * @return a list containing the values
     */
    @SuppressWarnings({"unchecked"})
    private static List<Entity.Key> initializeValues(final Object... values) {
      final List<Entity.Key> ret = new ArrayList<Entity.Key>();
      if (values == null) {
        ret.add(null);
      }
      else {
        if (values.length == 1 && values[0] instanceof Collection) {
          ret.addAll(getValueList(((Collection) values[0]).toArray()));
        }
        else {
          ret.addAll(getValueList(values));
        }
      }

      return ret;
    }

    @SuppressWarnings({"unchecked"})
    private static Collection getValueList(final Object... values) {
      if (values.length == 0) {
        return new ArrayList();
      }
      if (values[0] instanceof Entity.Key) {
        return Arrays.asList(values);
      }
      else {
        final List entities = new ArrayList();
        entities.addAll(Arrays.asList(values));

        return EntityUtil.getPrimaryKeys(entities);
      }
    }
  }
}
