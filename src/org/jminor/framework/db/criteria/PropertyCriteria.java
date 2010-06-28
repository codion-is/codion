/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A object for encapsulating a query criteria with a single property and one or more values.
 */
public class PropertyCriteria implements Criteria<Property>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The property used in this criteria
   */
  private final Property property;

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
  public PropertyCriteria(final Property property, final SearchType searchType, final Object... values) {
    Util.rejectNullValue(property);
    Util.rejectNullValue(searchType);
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

    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKeyCriteriaValues();
    }

    return values;
  }

  public List<Property> getValueKeys() {
    if (isNullCriteria) {
      return new ArrayList<Property>();
    }//null criteria, uses 'x is null', not 'x = ?'

    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKeyValueProperties();
    }

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
   * @param value if true then this criteria is case sensitive, false otherwise
   * @return this PropertyCriteria instance
   */
  public PropertyCriteria setCaseSensitive(final boolean value) {
    this.caseSensitive = value;
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
    else if (values.length > 0 && values[0] instanceof Entity) {
      return EntityUtil.getPrimaryKeys((Collection) Arrays.asList(values));
    }
    else {
      return Arrays.asList(values);
    }
  }

  private String getConditionString() {
    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKeyCriteriaString();
    }

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

  private String getForeignKeyCriteriaString() {
    if (getValueCount() > 1) {
      return getMultipleForeignKeyCriteriaString();
    }

    return createSingleForeignKeyCriteria((Entity.Key) values.get(0)).asString();
  }

  private String getMultipleForeignKeyCriteriaString() {
    if (((Property.ForeignKeyProperty) property).isCompositeReference()) {
      return createMultipleCompositeForeignKeyCriteria().asString();
    }
    else {
      return getInList(((Property.ForeignKeyProperty) property).getReferenceProperties().get(0),
              searchType == SearchType.NOT_LIKE);
    }
  }

  private List<Object> getForeignKeyCriteriaValues() {
    if (values.size() > 1) {
      return getCompositeForeignKeyCriteriaValues();
    }

    return createSingleForeignKeyCriteria((Entity.Key) values.get(0)).getValues();
  }

  private List<Object> getCompositeForeignKeyCriteriaValues() {
    return createMultipleCompositeForeignKeyCriteria().getValues();
  }

  private List<Property> getForeignKeyValueProperties() {
    if (values.size() > 1) {
      return createMultipleCompositeForeignKeyCriteria().getValueKeys();
    }

    return createSingleForeignKeyCriteria((Entity.Key) values.get(0)).getValueKeys();
  }

  private Criteria<Property> createMultipleCompositeForeignKeyCriteria() {
    final CriteriaSet<Property> criteriaSet = new CriteriaSet<Property>(CriteriaSet.Conjunction.OR);
    for (final Object entityKey : values) {
      criteriaSet.addCriteria(createSingleForeignKeyCriteria((Entity.Key) entityKey));
    }

    return criteriaSet;
  }

  private Criteria<Property> createSingleForeignKeyCriteria(final Entity.Key entityKey) {
    final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
    if (foreignKeyProperty.isCompositeReference()) {
      final CriteriaSet<Property> pkSet = new CriteriaSet<Property>(CriteriaSet.Conjunction.AND);
      for (final Property referencedProperty : foreignKeyProperty.getReferenceProperties()) {
        final String referencedPropertyID = foreignKeyProperty.getReferencedPropertyID(referencedProperty);
        final Object referencedValue = entityKey == null ? null : entityKey.getValue(referencedPropertyID);
        pkSet.addCriteria(new PropertyCriteria(referencedProperty, searchType, referencedValue));
      }

      return pkSet;
    }
    else {
      return new PropertyCriteria(foreignKeyProperty.getReferenceProperties().get(0), searchType, entityKey == null ? null : entityKey.getFirstKeyValue());
    }
  }

  private String getNotLikeCondition(final Property property, final String likeValue) {
    return getValueCount() > 1 ? getInList(property, true) :
            initializeColumnIdentifier(property) + (property.isString() ? " not like "  + likeValue: " <> " + likeValue);
  }

  private String getInList(final Property property, final boolean notIn) {
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

  private String getLikeCondition(final Property property, final String likeValue) {
    return getValueCount() > 1 ? getInList(property, false) : initializeColumnIdentifier(property) +
            (property.isString() ? " like " + likeValue : " = " + likeValue);
  }

  private String initializeColumnIdentifier(final Property property) {
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
