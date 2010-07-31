/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A static utility class for constructing query criteria implementations
 */
public final class EntityCriteriaUtil {

  private EntityCriteriaUtil() {}

  public static EntitySelectCriteria selectCriteria(final Entity.Key key) {
    return selectCriteria(Arrays.asList(key));
  }

  public static EntitySelectCriteria selectCriteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new DefaultEntitySelectCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final Object... values) {
    return selectCriteria(entityID, propertyID, searchType, -1, values);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final String propertyID,
                                                    final SearchType searchType, final int fetchCount,
                                                    final Object... values) {
    return selectCriteria(entityID, propertyID, searchType, null, fetchCount, values);
  }

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

  public static EntitySelectCriteria selectCriteria(final String entityID, final String orderByClause) {
    return new DefaultEntitySelectCriteria(entityID, null, orderByClause);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final List<Property.ColumnProperty> foreignKeyProperties,
                                                    final List<Entity.Key> primaryKeys) {
    final EntityKeyCriteria entityKeyCriteria = new EntityKeyCriteria(foreignKeyProperties, primaryKeys);
    return new DefaultEntitySelectCriteria(entityID, entityKeyCriteria);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria,
                                                    final String orderByClause) {
    return selectCriteria(entityID, criteria, orderByClause, -1);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria,
                                                    final String orderByClause, final int fetchCount) {
    return new DefaultEntitySelectCriteria(entityID, criteria, orderByClause, fetchCount);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID) {
    return new DefaultEntitySelectCriteria(entityID);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final int fetchCount) {
    return new DefaultEntitySelectCriteria(entityID, null, fetchCount);
  }

  public static EntitySelectCriteria selectCriteria(final String entityID, final Criteria<Property.ColumnProperty> propertyCriteria) {
    return new DefaultEntitySelectCriteria(entityID, propertyCriteria);
  }

  public static EntityCriteria criteria(final Entity.Key key) {
    return criteria(Arrays.asList(key));
  }

  public static EntityCriteria criteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new DefaultEntityCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static EntityCriteria criteria(final String entityID) {
    return new DefaultEntityCriteria(entityID);
  }

  public static EntityCriteria criteria(final String entityID, final String propertyID,
                                        final SearchType searchType, final Object... values) {
    return new DefaultEntityCriteria(entityID, new PropertyCriteria((Property.ColumnProperty) Entities.getProperty(entityID, propertyID),
            searchType, values));
  }

  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID, final SearchType searchType,
                                                                   final Object... values) {
    return propertyCriteria(entityID, propertyID, true, searchType, values);
  }

  public static Criteria<Property.ColumnProperty> propertyCriteria(final String entityID, final String propertyID, final boolean caseSensitive,
                                                                   final SearchType searchType, final Object... values) {
    return propertyCriteria((Property.ColumnProperty) Entities.getProperty(entityID, propertyID), caseSensitive, searchType, values);
  }

  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final SearchType searchType,
                                                                   final Object... values) {
    return propertyCriteria(property, true, searchType, values);
  }

  public static Criteria<Property.ColumnProperty> propertyCriteria(final Property.ColumnProperty property,
                                                                   final boolean caseSensitive, final SearchType searchType,
                                                                   final Object... values) {
    return new PropertyCriteria(property, searchType, values).setCaseSensitive(caseSensitive);
  }

  public static Criteria<Property.ColumnProperty> foreignKeyCriteria(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                     final SearchType searchType, final Object... values) {
    return new ForeignKeyCriteria(foreignKeyProperty, searchType, values);
  }

  public static EntityCriteria criteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
    return new DefaultEntityCriteria(entityID, criteria);
  }

  private static class DefaultEntityCriteria implements EntityCriteria {

    private static final long serialVersionUID = 1;

    private final String entityID;
    private final Criteria<Property.ColumnProperty> criteria;

    /**
     * Instantiates a new empty EntityCriteria.
     * Using an empty criteria means all underlying records should be selected
     * @param entityID the ID of the entity to select
     */
    DefaultEntityCriteria(final String entityID) {
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
    DefaultEntityCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
      Util.rejectNullValue(entityID, "entityID");
      this.entityID = entityID;
      this.criteria = criteria;
    }

    public final List<Object> getValues() {
      return criteria == null ? null : criteria.getValues();
    }

    public final List<Property.ColumnProperty> getValueProperties() {
      return criteria == null ? null : criteria.getValueKeys();
    }

    public final String getEntityID() {
      return entityID;
    }

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

    public final String getWhereClause() {
      return getWhereClause(true);
    }

    public final String getWhereClause(final boolean includeWhereKeyword) {
      final String criteriaString = criteria == null ? "" : criteria.asString();

      return criteriaString.length() > 0 ? (includeWhereKeyword ? "where " : "and ") + criteriaString : "";
    }
  }

  private static final class DefaultEntitySelectCriteria extends DefaultEntityCriteria implements EntitySelectCriteria {

    private static final long serialVersionUID = 1;

    private final int fetchCount;
    private final String orderByClause;
    private int currentFetchDepth = 0;
    private Map<String, Integer> foreignKeyFetchDepths;
    private int fetchDepth;
    private boolean selectForUpdate;

    /**
     * Instantiates a new EntityCriteria, which includes all the underlying entities
     * @param entityID the ID of the entity to select
     */
    DefaultEntitySelectCriteria(final String entityID) {
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
    DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria) {
      this(entityID, criteria, null);
    }

    /**
     * Instantiates a new EntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final String orderByClause) {
      this(entityID, criteria, orderByClause, -1);
    }

    /**
     * Instantiates a new EntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param fetchCount the maximum number of records to fetch from the result
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final int fetchCount) {
      this(entityID, criteria, null, fetchCount);
    }

    /**
     * Instantiates a new EntityCriteria
     * @param entityID the ID of the entity to select
     * @param criteria the Criteria object
     * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
     * @param fetchCount the maximum number of records to fetch from the result
     * @see org.jminor.common.db.criteria.CriteriaSet
     * @see PropertyCriteria
     * @see EntityKeyCriteria
     */
    DefaultEntitySelectCriteria(final String entityID, final Criteria<Property.ColumnProperty> criteria, final String orderByClause,
                                final int fetchCount) {
      super(entityID, criteria);
      this.fetchCount = fetchCount;
      this.orderByClause = orderByClause;
      this.foreignKeyFetchDepths = initializeForeignKeyFetchDepths();
    }

    /**
     * Returns a where condition based on this EntityCriteria
     * @return a where condition based on this EntityCriteria
     */
    @Override
    public String asString() {
      return Entities.getSelectTableName(getEntityID()) + " " + getWhereClause();
    }

    public int getFetchCount() {
      return fetchCount;
    }

    public String getOrderByClause() {
      return orderByClause;
    }

    public int getCurrentFetchDepth() {
      return currentFetchDepth;
    }

    public EntitySelectCriteria setCurrentFetchDepth(final int currentFetchDepth) {
      this.currentFetchDepth = currentFetchDepth;
      return this;
    }

    public EntitySelectCriteria setFetchDepth(final String foreignKeyPropertyID, final int maxFetchDepth) {
      this.foreignKeyFetchDepths.put(foreignKeyPropertyID, maxFetchDepth);
      return this;
    }

    public int getFetchDepth(final String foreignKeyPropertyID) {
      if (foreignKeyFetchDepths.containsKey(foreignKeyPropertyID)) {
        return foreignKeyFetchDepths.get(foreignKeyPropertyID);
      }

      return 0;
    }

    public int getFetchDepth() {
      return fetchDepth;
    }

    public EntitySelectCriteria setFetchDepth(final int maxFetchDepth) {
      this.fetchDepth = maxFetchDepth;
      return this;
    }

    public EntitySelectCriteria setFetchDepthForAll(final int fetchDepth) {
      final Collection<Property.ForeignKeyProperty > properties = Entities.getForeignKeyProperties(getEntityID());
      for (final Property.ForeignKeyProperty property : properties) {
        foreignKeyFetchDepths.put(property.getPropertyID(), fetchDepth);
      }

      return this;
    }

    public boolean isSelectForUpdate() {
      return selectForUpdate;
    }

    public EntitySelectCriteria setSelectForUpdate(final boolean selectForUpdate) {
      this.selectForUpdate = selectForUpdate;
      return this;
    }

    private Map<String, Integer> initializeForeignKeyFetchDepths() {
      final Collection<Property.ForeignKeyProperty > properties = Entities.getForeignKeyProperties(getEntityID());
      final Map<String, Integer> depths = new HashMap<String, Integer>(properties.size());
      for (final Property.ForeignKeyProperty property : properties) {
        depths.put(property.getPropertyID(), property.getFetchDepth());
      }

      return depths;
    }
  }

  /**
   * A class encapsulating a query criteria with Entity.Key objects as values.
   */
  private static final class EntityKeyCriteria extends CriteriaSet<Property.ColumnProperty> {

    private static final long serialVersionUID = 1;

    /**
     * The keys used in this criteria
     */
    private final List<Entity.Key> keys;

    /**
     * The properties to use for column names when constructing the criteria string
     */
    private final List<Property.ColumnProperty> properties;

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys
     * @param keys the keys
     */
    EntityKeyCriteria(final Entity.Key... keys) {
      this(Arrays.asList(keys));
    }

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys
     * @param keys the keys
     */
    EntityKeyCriteria(final List<Entity.Key> keys) {
      this(null, keys);
    }

    /**
     * Instantiates a new EntityKeyCriteria comprised of the given keys which uses the given properties
     * as column names when constructing the criteria string
     * @param properties the properties to use for column names when constructing the criteria string
     * @param keys the keys
     */
    EntityKeyCriteria(final List<Property.ColumnProperty> properties, final List<Entity.Key> keys) {
      super(Conjunction.OR);
      Util.rejectNullValue(keys, "keys");
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("EntityKeyCriteria requires at least one key");
      }
      if (properties != null && properties.size() != keys.get(0).getPropertyCount()) {
        throw new IllegalArgumentException("Reference property count mismatch");
      }

      this.keys = keys;
      this.properties = properties;
      setupCriteria();
    }

    public List<String> getColumnNames() {
      if (properties == null) {
        return null;
      }

      final List<String> columnNames = new ArrayList<String>(properties.size());
      for (final Property.ColumnProperty property : properties) {
        columnNames.add(property.getColumnName());
      }

      return columnNames;
    }

    /**
     * @return the entityID
     */
    public String getEntityID() {
      return keys.get(0).getEntityID();
    }

    private void setupCriteria() {
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

          add(andSet);
        }
      }
      else {
        final Property.ColumnProperty property = properties == null ? keys.get(0).getFirstKeyProperty() : properties.get(0);
        final Property primaryKeyProperty = properties == null ? property : keys.get(0).getFirstKeyProperty();
        //a = b
        if (keys.size() == 1) {
          final Entity.Key key = keys.get(0);
          add(new PropertyCriteria(property, SearchType.LIKE, key.getValue(primaryKeyProperty.getPropertyID())));
        }
        else { //a in (c, v, d, s)
          add(new PropertyCriteria(property, SearchType.LIKE, EntityUtil.getPropertyValues(keys)));
        }
      }
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
    private final Property.ColumnProperty property;

    /**
     * The values used in this criteria
     */
    private final List<Object> values;

    /**
     * True if this criteria tests for null
     */
    private final boolean isNullCriteria;

    /**
     * The search type used in this criteria
     */
    private final SearchType searchType;

    /**
     * The wildcard being used
     */
    private final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);

    /**
     * True if this criteria should be case sensitive, only applies to criteria based on string properties
     */
    private boolean caseSensitive = true;

    /**
     * Instantiates a new PropertyCriteria instance
     * @param property the property
     * @param searchType the search type
     * @param values the values
     */
    PropertyCriteria(final Property.ColumnProperty property, final SearchType searchType, final Object... values) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      if (values != null && values.length == 0) {
        throw new RuntimeException("No values specified for PropertyCriteria: " + property);
      }
      this.property = property;
      this.searchType = searchType;
      this.values = initializeValues(values);
      this.isNullCriteria = this.values.size() == 1 && this.values.get(0) == null;
    }

    public Property getProperty() {
      return property;
    }

    public List<Object> getValues() {
      if (isNullCriteria) {
        return new ArrayList<Object>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return values;
    }

    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return new ArrayList<Property.ColumnProperty>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    /**
     * @return the number values contained in this criteria.
     */
    public int getValueCount() {
      return getValues().size();
    }

    /**
     * @return the active search type.
     */
    public SearchType getSearchType() {
      return searchType;
    }

    /**
     * @return the wildcard to use in case of string properties.
     */
    public String getWildcard() {
      return wildcard;
    }

    public String asString() {
      return getConditionString();
    }

    /**
     * Sets whether this criteria should be case sensitive, only applies to criteria based on string properties
     * @param caseSensitive if true then this criteria is case sensitive, false otherwise
     * @return this PropertyCriteria instance
     */
    public PropertyCriteria setCaseSensitive(final boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    /**
     * @return true if this criteria is case sensitive (only applies to criteria based on string properties)
     */
    public boolean isCaseSensitive() {
      return caseSensitive;
    }

    /**
     * @param values the values to use in this criteria
     * @return a list containing the values
     */
    @SuppressWarnings({"unchecked"})
    private List<Object> initializeValues(final Object... values) {
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
    private Collection getValueList(final Object... values) {
      if (values.length == 1 && values[0] instanceof Collection) {
        return getValueList(((Collection) values[0]).toArray());
      }
      else {
        return Arrays.asList(values);
      }
    }

    private String getConditionString() {
      final String columnIdentifier = initializeColumnIdentifier(property);
      if (isNullCriteria) {
        return columnIdentifier + (searchType == SearchType.LIKE ? " is null" : " is not null");
      }

      final String sqlValue = getSqlValue("?");
      final String sqlValue2 = getValueCount() == 2 ? getSqlValue("?") : null;

      switch(searchType) {
        case LIKE:
          return getLikeCondition(property, sqlValue);
        case NOT_LIKE:
          return getNotLikeCondition(property, sqlValue);
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



    private String getNotLikeCondition(final Property.ColumnProperty property, final String likeValue) {
      return getValueCount() > 1 ? getInList(property, true) :
              initializeColumnIdentifier(property) + (property.isString() ? " not like "  + likeValue: " <> " + likeValue);
    }

    private String getInList(final Property.ColumnProperty property, final boolean notIn) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(initializeColumnIdentifier(property)).append((notIn ? " not in (" : " in ("));
      int cnt = 1;
      for (int i = 0; i < getValueCount(); i++) {
        if (this.property.isString() && !caseSensitive) {
          stringBuilder.append("upper(?)");
        }
        else {
          stringBuilder.append("?");
        }
        if (cnt++ == 1000 && i < getValueCount() - 1) {//Oracle limit
          stringBuilder.append(notIn ? ") and " : ") or ").append(property.getColumnName()).append(" in (");
          cnt = 1;
        }
        else if (i < getValueCount() - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }

    private String getLikeCondition(final Property.ColumnProperty property, final String likeValue) {
      return getValueCount() > 1 ? getInList(property, false) : initializeColumnIdentifier(property) +
              (property.isString() ? " like " + likeValue : " = " + likeValue);
    }

    private String initializeColumnIdentifier(final Property.ColumnProperty property) {
      String columnName;
      if (property instanceof Property.SubqueryProperty) {
        columnName = "(" + ((Property.SubqueryProperty) property).getSubQuery() + ")";
      }
      else {
        columnName = property.getColumnName();
      }

      if (!isNullCriteria && property.isString() && !caseSensitive) {
        columnName = "upper(" + columnName + ")";
      }

      return columnName;
    }
  }

  private static final class ForeignKeyCriteria implements Criteria<Property.ColumnProperty>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this criteria
     */
    private final Property.ForeignKeyProperty property;

    /**
     * The values used in this criteria
     */
    private final List<Entity.Key> values;
    private final SearchType searchType;
    private final boolean isNullCriteria;

    private ForeignKeyCriteria(final Property.ForeignKeyProperty property, final SearchType searchType, final Object... values) {
      Util.rejectNullValue(property, "property");
      Util.rejectNullValue(searchType, "searchType");
      if (values != null && values.length == 0) {
        throw new RuntimeException("No values specified for ForeignKeyPropertyCriteria: " + property);
      }
      this.property = property;
      this.searchType = searchType;
      this.values = initializeValues(values);
      this.isNullCriteria = this.values.size() == 1 && this.values.get(0) == null;
    }

    public String asString() {
      return getConditionString();
    }

    private String getConditionString() {
      return getForeignKeyCriteriaString();
    }

    public List<Property.ColumnProperty> getValueKeys() {
      if (isNullCriteria) {
        return new ArrayList<Property.ColumnProperty>();
      }//null criteria, uses 'x is null', not 'x = ?'

      return getForeignKeyValueProperties();
    }

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

    /**
     * @param values the values to use in this criteria
     * @return a list containing the values
     */
    @SuppressWarnings({"unchecked"})
    private List<Entity.Key> initializeValues(final Object... values) {
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
    private Collection getValueList(final Object... values) {
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

    private String getInList(final Property.ColumnProperty property, final boolean notIn) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(property.getColumnName()).append((notIn ? " not in (" : " in ("));
      int cnt = 1;
      for (int i = 0; i < getValues().size(); i++) {
        stringBuilder.append("?");
        if (cnt++ == 1000 && i < getValues().size() - 1) {//Oracle limit
          stringBuilder.append(notIn ? ") and " : ") or ").append(property.getColumnName()).append(" in (");
          cnt = 1;
        }
        else if (i < getValues().size() - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }
  }
}
