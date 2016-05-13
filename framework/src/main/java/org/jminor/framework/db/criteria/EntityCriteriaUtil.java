/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.Version;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A factory class for query criteria implementations.
 */
public final class EntityCriteriaUtil {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  //todo remove when time is right
  private static final Version V0_9_11 = new Version(0, 9, 11);

  private EntityCriteriaUtil() {}

  /**
   * @param key the key
   * @return a select criteria based on the given key
   */
  public static EntitySelectCriteria selectCriteria(final Entity.Key key) {
    return selectCriteria(Collections.singletonList(key));
  }

  /**
   * @param keys the keys
   * @return a select criteria based on the given keys
   */
  public static EntitySelectCriteria selectCriteria(final Collection<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new DefaultEntitySelectCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param value the criteria value, can be a Collection of values
   * @return a select criteria based on the given value
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final Object value) {
    return selectCriteria(entityID, propertyID, searchType, -1, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param fetchCount the maximum number of entities to fetch
   * @param value the criteria value, can be a Collection of values
   * @return a select criteria based on the given value
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final int fetchCount,
                                                    final Object value) {
    return selectCriteria(entityID, propertyID, searchType, null, fetchCount, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @param fetchCount the maximum number of entities to fetch
   * @param value the criteria value, can be a Collection of values
   * @return a select criteria based on the given value
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final String orderByClause,
                                                    final int fetchCount, final Object value) {
    return new DefaultEntitySelectCriteria(entityID, createPropertyCriteria(entityID, propertyID, searchType, value),
            fetchCount).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select criteria including all entities of the given type
   */
  public static EntitySelectCriteria selectCriteria(final String entityID, final String orderByClause) {
    return new DefaultEntitySelectCriteria(entityID, null).setOrderByClause(orderByClause);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyReferenceProperties the foreign key reference properties
   * @param primaryKeys the primary keys referenced by the given properties
   * @return a select criteria based on the given values
   */
  public static EntitySelectCriteria selectCriteria(final String entityID,
                                                    final List<Property.ColumnProperty> foreignKeyReferenceProperties,
                                                    final List<Entity.Key> primaryKeys) {
    final EntityKeyCriteria entityKeyCriteria = new EntityKeyCriteria(foreignKeyReferenceProperties, primaryKeys);
    return new DefaultEntitySelectCriteria(entityID, entityKeyCriteria);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCriteria the column criteria
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @return a select criteria based on the given column criteria
   */
  public static EntitySelectCriteria selectCriteria(final String entityID,
                                                    final Criteria<Property.ColumnProperty> propertyCriteria,
                                                    final String orderByClause) {
    return selectCriteria(entityID, propertyCriteria, orderByClause, -1);
  }

  /**
   * @param entityID the entity ID
   * @param propertyCriteria the column criteria
   * @param orderByClause the order by clause, without the 'order by' keywords
   * @param fetchCount the maximum number of entities to fetch
   * @return a select criteria based on the given column criteria
   */
  public static EntitySelectCriteria selectCriteria(final String entityID,
                                                    final Criteria<Property.ColumnProperty> propertyCriteria,
                                                    final String orderByClause, final int fetchCount) {
    return new DefaultEntitySelectCriteria(entityID, propertyCriteria, fetchCount).setOrderByClause(orderByClause);
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
  public static EntitySelectCriteria selectCriteria(final String entityID,
                                                    final Criteria<Property.ColumnProperty> propertyCriteria) {
    return new DefaultEntitySelectCriteria(entityID, propertyCriteria);
  }

  /**
   * @param key the primary key
   * @return a criteria specifying the entity having the given primary key
   */
  public static EntityCriteria criteria(final Entity.Key key) {
    return criteria(Collections.singletonList(key));
  }

  /**
   * Creates a criteria based on the given primary keys, it is assumed they are all for the same entityID
   * @param keys the primary keys
   * @return a criteria specifying the entities having the given primary keys
   */
  public static EntityCriteria criteria(final Collection<Entity.Key> keys) {
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
   * @param value the criteria value, can be a Collection of values
   * @return a criteria based on the given value
   */
  public static EntityCriteria criteria(final String entityID, final String propertyID,
                                        final SearchType searchType, final Object value) {
    return new DefaultEntityCriteria(entityID, createPropertyCriteria(entityID, propertyID, searchType, value));
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param value the criteria value, can be a Collection of values
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID,
                                                                   final SearchType searchType, final Object value) {
    return propertyCriteria(entityID, propertyID, searchType, true, value);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param searchType the search type
   * @param caseSensitive true if the criteria should be case sensitive, only applicable to string properties
   * @param value the criteria value, can be a Collection of values
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID,
                                                                   final SearchType searchType, final boolean caseSensitive,
                                                                   final Object value) {
    final Property property = Entities.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(property + " is not a " + Property.ColumnProperty.class.getName());
    }

    return propertyCriteria((Property.ColumnProperty) property, searchType, caseSensitive, value);
  }

  /**
   * @param property the property
   * @param searchType the search type
   * @param value the criteria value, can be a Collection of values
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final SearchType searchType, final Object value) {
    return propertyCriteria(property, searchType, true, value);
  }

  /**
   * @param property the property
   * @param searchType the search type
   * @param caseSensitive true if the criteria should be case sensitive, only applicable to string properties
   * @param value the criteria value, can be a Collection of values
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final SearchType searchType, final boolean caseSensitive,
                                                                   final Object value) {
    return new PropertyCriteria(property, searchType, value).setCaseSensitive(caseSensitive);
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param searchType the search type
   * @param value the criteria value
   * @return a foreign key property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final String entityID, final String fkPropertyID,
                                                                     final SearchType searchType, final Entity value) {
    return foreignKeyCriteria(entityID, fkPropertyID, searchType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param searchType the search type
   * @param value the criteria value
   * @return a foreign key property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final String entityID, final String fkPropertyID,
                                                                     final SearchType searchType, final Entity.Key value) {
    return foreignKeyCriteria(entityID, fkPropertyID, searchType, Collections.singletonList(value));
  }

  /**
   * @param entityID the entity ID
   * @param fkPropertyID the property ID
   * @param searchType the search type
   * @param values the criteria values
   * @return a foreign key property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final String entityID, final String fkPropertyID,
                                                                     final SearchType searchType, final Collection values) {
    return foreignKeyCriteria(Entities.getForeignKeyProperty(entityID, fkPropertyID), searchType, values);
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param searchType the search type
   * @param value the criteria value
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                     final SearchType searchType, final Entity value) {
    return foreignKeyCriteria(foreignKeyProperty, searchType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param searchType the search type
   * @param value the criteria value
   * @return a property criteria based on the given value
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                     final SearchType searchType, final Entity.Key value) {
    return foreignKeyCriteria(foreignKeyProperty, searchType, Collections.singletonList(value));
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @param searchType the search type
   * @param values the criteria values
   * @return a property criteria based on the given values
   */
  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                     final SearchType searchType, final Collection values) {
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

  private static Criteria<Property.ColumnProperty> createPropertyCriteria(final String entityID, final String propertyID,
                                                                          final SearchType searchType, final Object value) {
    final Property property = Entities.getProperty(entityID, propertyID);
    final Criteria<Property.ColumnProperty> criteria;
    if (property instanceof Property.ForeignKeyProperty) {
      criteria = new ForeignKeyCriteria((Property.ForeignKeyProperty) property, searchType, value);
    }
    else {
      criteria = new PropertyCriteria((Property.ColumnProperty) property, searchType, value);
    }

    return criteria;
  }

  private static final class DefaultEntityCriteria implements EntityCriteria, Serializable {

    private static final long serialVersionUID = 1;

    private String entityID;
    private Criteria<Property.ColumnProperty> criteria;
    private String cachedWhereClause;

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

    @Override
    public List<?> getValues() {
      return criteria == null ? null : criteria.getValues();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return criteria == null ? null : criteria.getValueKeys();
    }

    @Override
    public String getEntityID() {
      return entityID;
    }

    @Override
    public Criteria<Property.ColumnProperty> getCriteria() {
      return criteria;
    }

    @Override
    public String getWhereClause() {
      if (cachedWhereClause == null) {
        cachedWhereClause = criteria == null ? "" : criteria.getWhereClause();
      }

      return cachedWhereClause;
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

    private OrderBy orderBy;
    private String orderByClause;
    private int fetchCount;
    private boolean forUpdate;
    private int limit;
    private int offset;

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
      this(entityID, criteria, -1);
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
    private DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria,
                                        final int fetchCount) {
      this.criteria = new DefaultEntityCriteria(entityID, criteria);
      this.fetchCount = fetchCount;
    }

    @Override
    public Criteria<Property.ColumnProperty> getCriteria() {
      return criteria.getCriteria();
    }

    @Override
    public String getEntityID() {
      return criteria.getEntityID();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return criteria.getValueKeys();
    }

    @Override
    public List<?> getValues() {
      return criteria.getValues();
    }

    @Override
    public String getWhereClause() {
      return criteria.getWhereClause();
    }

    @Override
    public int getFetchCount() {
      return fetchCount;
    }

    @Override
    public String getOrderByClause() {
      return orderBy != null ? orderBy.getOrderByClause() : orderByClause;
    }

    @Override
    public EntitySelectCriteria setOrderByClause(final String orderByClause) {
      this.orderByClause = orderByClause;
      return this;
    }

    @Override
    public EntitySelectCriteria orderByAscending(final String propertyID) {
      getOrderBy().add(propertyID, OrderBy.SortOrder.ASCENDING);
      return this;
    }

    @Override
    public EntitySelectCriteria orderByDescending(final String propertyID) {
      getOrderBy().add(propertyID, OrderBy.SortOrder.DESCENDING);
      return this;
    }

    @Override
    public int getLimit() {
      return limit;
    }

    @Override
    public EntitySelectCriteria setLimit(final int limit) {
      this.limit = limit;
      return this;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    @Override
    public EntitySelectCriteria setOffset(final int offset) {
      this.offset = offset;
      return this;
    }

    @Override
    public EntitySelectCriteria setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int fetchDepthLimit) {
      if (foreignKeyFetchDepthLimits == null) {
        foreignKeyFetchDepthLimits = new HashMap<>();
      }
      this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyID, fetchDepthLimit);
      return this;
    }

    @Override
    public int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyID) {
      if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyID)) {
        return foreignKeyFetchDepthLimits.get(foreignKeyPropertyID);
      }

      return Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID).getFetchDepth();
    }

    @Override
    public EntitySelectCriteria setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
      final Collection<Property.ForeignKeyProperty > properties = Entities.getForeignKeyProperties(getEntityID());
      for (final Property.ForeignKeyProperty property : properties) {
        setForeignKeyFetchDepthLimit(property.getPropertyID(), fetchDepthLimit);
      }

      return this;
    }

    @Override
    public boolean isForUpdate() {
      return forUpdate;
    }

    @Override
    public EntitySelectCriteria setForUpdate(final boolean forUpdate) {
      this.forUpdate = forUpdate;
      return this;
    }

    private OrderBy getOrderBy() {
      if (orderBy == null) {
        orderBy = new OrderBy(getEntityID());
      }

      return orderBy;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      final Version serverVersion = (Version) Configuration.getValue(Configuration.REMOTE_SERVER_VERSION);
      if (serverVersion != null && serverVersion.compareTo(V0_9_11) >= 0) {
        stream.writeObject(orderBy);
        stream.writeObject(orderByClause);
      }
      else {
        stream.writeObject(orderBy != null ? orderBy.getOrderByClause() : orderByClause);
      }
      stream.writeInt(fetchCount);
      stream.writeBoolean(forUpdate);
      stream.writeObject(foreignKeyFetchDepthLimits);
      stream.writeObject(criteria);
      stream.writeInt(limit);
      stream.writeInt(offset);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      orderBy = (OrderBy) stream.readObject();
      orderByClause = (String) stream.readObject();
      fetchCount = stream.readInt();
      forUpdate = stream.readBoolean();
      foreignKeyFetchDepthLimits = (HashMap<String, Integer>) stream.readObject();
      criteria = (EntityCriteria) stream.readObject();
      limit = stream.readInt();
      offset = stream.readInt();
    }
  }

  /**
   * A class encapsulating a query criteria with Entity.Key objects as values.
   */
  private static final class EntityKeyCriteria implements Criteria<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    private String entityID;
    private CriteriaSet<Property.ColumnProperty> criteria;

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys
     * @param keys the keys
     */
    private EntityKeyCriteria(final Collection<Entity.Key> keys) {
      this(null, keys);
    }

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys which uses the given properties
     * as column names when constructing the criteria string
     * @param properties the properties to use for column names when constructing the criteria string
     * @param keys the keys
     */
    private EntityKeyCriteria(final List<Property.ColumnProperty> properties, final Collection<Entity.Key> keys) {
      criteria = CriteriaUtil.criteriaSet(Conjunction.OR);
      Util.rejectNullValue(keys, "keys");
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
      }
      final Entity.Key firstKey = keys.iterator().next();
      if (properties != null && properties.size() != firstKey.getPropertyCount()) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }
      entityID = firstKey.getEntityID();
      setupCriteria(properties, keys, firstKey);
    }

    @Override
    public String getWhereClause() {
      return criteria.getWhereClause();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      return criteria.getValueKeys();
    }

    @Override
    public List<Object> getValues() {
      return criteria.getValues();
    }

    /**
     * @return the entityID
     */
    private String getEntityID() {
      return entityID;
    }

    private void setupCriteria(final List<Property.ColumnProperty> properties, final Collection<Entity.Key> keys,
                               final Entity.Key firstKey) {
      if (firstKey.isCompositeKey()) {//multiple column key
        final List<Property.ColumnProperty> pkProperties = firstKey.getProperties();
        final List<? extends Property.ColumnProperty> propertyList = properties == null ? pkProperties : properties;
        //(a = b and c = d) or (a = g and c = d)
        for (final Entity.Key key : keys) {
          final CriteriaSet<Property.ColumnProperty> andSet = CriteriaUtil.criteriaSet(Conjunction.AND);
          int i = 0;
          for (final Property.ColumnProperty property : propertyList) {
            andSet.add(new PropertyCriteria(property, SearchType.LIKE, key.get(pkProperties.get(i++).getPropertyID())));
          }

          criteria.add(andSet);
        }
      }
      else {
        final Property.ColumnProperty property = properties == null ? firstKey.getFirstProperty() : properties.get(0);
        final Property primaryKeyProperty = properties == null ? property : firstKey.getFirstProperty();
        //a = b
        if (keys.size() == 1) {
          criteria.add(new PropertyCriteria(property, SearchType.LIKE, firstKey.get(primaryKeyProperty.getPropertyID())));
        }
        else { //a in (c, v, d, s)
          criteria.add(new PropertyCriteria(property, SearchType.LIKE, EntityUtil.getValues(keys)));
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
    private Collection values;

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

    /**
     * Instantiates a new PropertyCriteria instance
     * @param property the property
     * @param searchType the search type
     * @param value the value, can be a Collection
     */
    private PropertyCriteria(final Property.ColumnProperty property, final SearchType searchType, final Object value) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      if (value instanceof Collection) {
        this.values = (Collection) value;
      }
      else {
        this.values = Collections.singletonList(value);
      }
      if (values.isEmpty()) {
        throw new IllegalArgumentException("No values specified for PropertyCriteria: " + property);
      }
      this.property = property;
      this.searchType = searchType;
      this.isNullCriteria = this.values.size() == 1 && this.values.iterator().next() == null;
    }

    @Override
    public List getValues() {
      if (isNullCriteria) {
        return Collections.emptyList();
      }//null criteria, uses 'x is null', not 'x = ?'

      //noinspection unchecked
      return new ArrayList(values);
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return Collections.emptyList();
      }//null criteria, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    @Override
    public String getWhereClause() {
      return getConditionString();
    }

    /**
     * @return the number values contained in this criteria.
     */
    private int getValueCount() {
      if (isNullCriteria) {
        return 0;
      }

      return values.size();
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
          return getLikeCondition(columnIdentifier, sqlValue, false);
        case NOT_LIKE:
          return getLikeCondition(columnIdentifier, sqlValue, true);
        case LESS_THAN:
          return columnIdentifier + " <= " + sqlValue;
        case GREATER_THAN:
          return columnIdentifier + " >= " + sqlValue;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
        default:
          throw new IllegalArgumentException("Unknown search type" + searchType);
      }
    }

    private String getSqlValue(final String sqlStringValue) {
      return property.isString() && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
    }

    private String getLikeCondition(final String columnIdentifier, final String value, final boolean not) {
      if (getValueCount() > 1) {
        return getInList(columnIdentifier, value, getValueCount(), not);
      }

      if (property.isString()) {
        return columnIdentifier + (not ? " not like " : " like ") + value;
      }
      else {
        return columnIdentifier + (not ? " <> " : " = ") + value;
      }
    }

    private static String getInList(final String columnIdentifier, final String value, final int valueCount, final boolean not) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(columnIdentifier).append(not ? " not in (" : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(value);
        if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
          stringBuilder.append(not ? ") and " : ") or ").append(columnIdentifier).append(IN_PREFIX);
          cnt = 1;
        }
        else if (i < valueCount - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
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
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
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
    private Collection<Entity.Key> values;
    private SearchType searchType;
    private boolean isNullCriteria;

    private ForeignKeyCriteria(final Property.ForeignKeyProperty property, final SearchType searchType, final Object value) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      this.property = property;
      this.searchType = searchType;
      this.values = getEntityKeys(value);
      this.isNullCriteria = this.values.size() == 1 && this.values.iterator().next() == null;
    }

    @Override
    public String getWhereClause() {
      return getForeignKeyCriteriaString();
    }

    @Override
    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return Collections.emptyList();
      }//null criteria, uses 'x is null', not 'x = ?'

      return getForeignKeyValueProperties();
    }

    @Override
    public List<?> getValues() {
      if (isNullCriteria) {
        return Collections.emptyList();
      }//null criteria, uses 'x is null', not 'x = ?'

      return getForeignKeyCriteriaValues();
    }

    private String getForeignKeyCriteriaString() {
      if (getValues().size() > 1) {
        return getMultipleForeignKeyCriteriaString();
      }

      return createSingleForeignKeyCriteria(values.iterator().next()).getWhereClause();
    }

    private String getMultipleForeignKeyCriteriaString() {
      if (property.isCompositeReference()) {
        return createMultipleCompositeForeignKeyCriteria().getWhereClause();
      }
      else {
        return PropertyCriteria.getInList(property.getReferenceProperties().get(0).getColumnName(), "?",
                getValues().size(), searchType == SearchType.NOT_LIKE);
      }
    }

    private List<?> getForeignKeyCriteriaValues() {
      if (values.size() > 1) {
        return getCompositeForeignKeyCriteriaValues();
      }

      return createSingleForeignKeyCriteria(values.iterator().next()).getValues();
    }

    private List<?> getCompositeForeignKeyCriteriaValues() {
      return createMultipleCompositeForeignKeyCriteria().getValues();
    }

    private List<Property.ColumnProperty> getForeignKeyValueProperties() {
      if (values.size() > 1) {
        return createMultipleCompositeForeignKeyCriteria().getValueKeys();
      }

      return createSingleForeignKeyCriteria(values.iterator().next()).getValueKeys();
    }

    private Criteria<Property.ColumnProperty> createMultipleCompositeForeignKeyCriteria() {
      final CriteriaSet<Property.ColumnProperty> criteriaSet = CriteriaUtil.criteriaSet(Conjunction.OR);
      for (final Object entityKey : values) {
        criteriaSet.add(createSingleForeignKeyCriteria((Entity.Key) entityKey));
      }

      return criteriaSet;
    }

    private Criteria<Property.ColumnProperty> createSingleForeignKeyCriteria(final Entity.Key entityKey) {
      final Property.ForeignKeyProperty foreignKeyProperty = property;
      if (foreignKeyProperty.isCompositeReference()) {
        final CriteriaSet<Property.ColumnProperty> pkSet = CriteriaUtil.criteriaSet(Conjunction.AND);
        for (final Property.ColumnProperty referencedProperty : foreignKeyProperty.getReferenceProperties()) {
          final String referencedPropertyID = foreignKeyProperty.getReferencedPropertyID(referencedProperty);
          final Object referencedValue = entityKey == null ? null : entityKey.get(referencedPropertyID);
          pkSet.add(new PropertyCriteria(referencedProperty, searchType, referencedValue));
        }

        return pkSet;
      }
      else {
        return new PropertyCriteria(foreignKeyProperty.getReferenceProperties().get(0), searchType,
                entityKey == null ? null : entityKey.getFirstValue());
      }
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
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add((Entity.Key) stream.readObject());
      }
    }

    @SuppressWarnings({"unchecked"})
    private static Collection<Entity.Key> getEntityKeys(final Object value) {
      final Collection<Entity.Key> keys = new ArrayList<>();
      if (value instanceof Collection) {
        if (((Collection) value).isEmpty()) {
          keys.add(null);
        }
        else {
          for (final Object object : (Collection) value) {
            keys.add(getKey(object));
          }
        }
      }
      else {
        keys.add(getKey(value));
      }

      return keys;
    }

    private static Entity.Key getKey(final Object value) {
      if (value == null || value instanceof Entity.Key) {
        return (Entity.Key) value;
      }
      else if (value instanceof Entity) {
        return ((Entity) value).getKey();
      }

      throw new IllegalArgumentException("Foreign key criteria uses only Entity or Entity.Key instances for values");
    }
  }

  private static final class OrderBy implements Serializable {

    private static final long serialVersionUID = 1;

    private enum SortOrder {
      ASCENDING, DESCENDING
    }

    private String entityID;
    private LinkedHashMap<String, SortOrder> propertySortOrder = new LinkedHashMap<>();

    private OrderBy(final String entityID) {
      Util.rejectNullValue(entityID, "entityID");
      this.entityID = entityID;
    }

    private OrderBy add(final String propertyID, final SortOrder order) {
      Util.rejectNullValue(propertyID, "propertyID");
      if (propertySortOrder.containsKey(propertyID)) {
        throw new IllegalArgumentException("Order by already contains the given property: " + propertyID);
      }
      propertySortOrder.put(propertyID, order);
      return this;
    }

    private String getOrderByClause() {
      final StringBuilder builder = new StringBuilder();
      final Set<Map.Entry<String, SortOrder>> entries = propertySortOrder.entrySet();
      int counter = 0;
      for (final Map.Entry<String, SortOrder> entry : entries) {
        final Property.ColumnProperty property = Entities.getColumnProperty(entityID, entry.getKey());
        builder.append(property.getColumnName());
        if (entry.getValue().equals(SortOrder.DESCENDING)) {
          builder.append(" desc");
        }
        if (counter++ < entries.size() - 1) {
          builder.append(", ");
        }
      }

      return builder.toString();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(entityID);
      stream.writeObject(propertySortOrder);
    }

    @SuppressWarnings({"unchecked"})
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      entityID = (String) stream.readObject();
      propertySortOrder = (LinkedHashMap<String, SortOrder>) stream.readObject();
    }
  }
}
