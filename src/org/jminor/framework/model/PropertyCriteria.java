/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Constants;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.SearchType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A object for encapsulating a query criteria with a single property and one or more values
 */
public class PropertyCriteria implements ICriteria {

  private final Property property;
  private final List<Object> values = new ArrayList<Object>();
  private final SearchType searchType;

  private boolean caseSensitive = true;

  public PropertyCriteria(final Property property, final SearchType searchType, final Object... values) {
    this.property = property;
    this.searchType = searchType;
    setValues(values);
  }

  /**
   * @param values Value to set for property 'values'.
   */
  @SuppressWarnings({"unchecked"})
  public void setValues(final Object... values) {
    this.values.clear();
    if (values == null)
      this.values.add(null);
    else
      this.values.addAll(getValues(values));
  }

  @SuppressWarnings({"unchecked"})
  private Collection getValues(final Object... values) {
    if (values.length == 1 && values[0] instanceof Collection)
      return getValues(((Collection) values[0]).toArray());
    else if (values.length > 0 && values[0] instanceof Entity)
      return EntityUtil.getPrimaryKeys((Collection) Arrays.asList(values));
    else
      return Arrays.asList(values);
  }

  /**
   * @return Value for property 'values'.
   */
  public List<Object> getValues() {
    return this.values;
  }

  /**
   * @return Value for property 'searchType'.
   */
  public SearchType getSearchType() {
    return this.searchType;
  }

  /** {@inheritDoc} */
  public String toString() {
    return getConditionString();
  }

  /**
   * @return Value for property 'conditionString'.
   */
  public String getConditionString() {
    if (property instanceof Property.EntityProperty)
      return getReferenceCriteriaString();

    String columnName;
    if (property instanceof Property.SubQueryProperty)
      columnName = "("+((Property.SubQueryProperty)property).getSubQuery()+")";
    else
      columnName = property.propertyID;

    if (values.size() == 1 && EntityUtil.isValueNull(property.getPropertyType(), values.get(0)))
      return columnName + (searchType == SearchType.LIKE ? " is null" : " is not null");

    String sqlValue = EntityUtil.getSQLStringValue(property, values.get(0));
    String sqlValue2 = values.size() == 2 ? EntityUtil.getSQLStringValue(property, values.get(1)) : null;

    if (property.getPropertyType() == Type.STRING && !caseSensitive) {
      columnName = "upper(" + columnName + ")";
      sqlValue = "upper(" + sqlValue + ")";
      sqlValue2 = "upper(" + sqlValue2 + ")";
    }

    switch(searchType) {
      case LIKE:
        return columnName + (property.getPropertyType() == Type.STRING && containsWildcard(sqlValue)
                ? " like " + sqlValue : " = " + sqlValue);
      case NOT_LIKE:
        return columnName + (property.getPropertyType() == Type.STRING && containsWildcard(sqlValue)
                ? " not like " + sqlValue : " <> " + sqlValue);
      case MAX :
        return columnName + " <= " + sqlValue;
      case MIN :
        return columnName + " >= " + sqlValue;
      case INSIDE:
        return "(" + columnName + " >= " + sqlValue + " and " + columnName +  " <= " + sqlValue2 + ")";
      case OUTSIDE:
        return "(" + columnName + " <= "+ sqlValue + " or " + columnName + " >= " + sqlValue2 + ")";
      case IN:
        return getInList(columnName);
    }

    throw new IllegalArgumentException("Unknown search type" + searchType);
  }

  public PropertyCriteria setCaseSensitive(final boolean value) {
    this.caseSensitive = value;
    return this;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  private String getReferenceCriteriaString() {
    if (values.size() > 1)
      return getMultiReferenceCriteriaString();

    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
    final EntityKey entityKey = (EntityKey) values.get(0);
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.get().getPrimaryKeyProperties(((Property.EntityProperty) property).referenceEntityID);
    for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
      set.addCriteria(new PropertyCriteria(
              ((Property.EntityProperty) property).referenceProperties.get(keyProperty.primaryKeyIndex),
              searchType, entityKey == null ? null : entityKey.getValue(keyProperty.propertyID)));

    return set.toString();
  }

  private String getMultiReferenceCriteriaString() {
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.get().getPrimaryKeyProperties(((Property.EntityProperty) property).referenceEntityID);
    if (primaryKeyProperties.size() > 1) {
      final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR);
      for (final Object entityKey : values) {
        final CriteriaSet pkSet = new CriteriaSet(CriteriaSet.Conjunction.AND);
        for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
          pkSet.addCriteria(new PropertyCriteria(
                  ((Property.EntityProperty) property).referenceProperties.get(keyProperty.primaryKeyIndex),
                  searchType, ((EntityKey) entityKey).getValue(keyProperty.propertyID)));

        set.addCriteria(pkSet);
      }

      return set.toString();
    }
    else
      return getInList(((Property.EntityProperty) property).referenceProperties.get(0).propertyID);
  }

  private boolean containsWildcard(final String val) {
    return val != null && val.length() > 0 && val.indexOf(Constants.WILDCARD) > -1;
  }

  private String getInList(final String whereColumn) {
    final StringBuffer ret = new StringBuffer(whereColumn + " in (");
    int cnt = 1;
    for (int i = 0; i < values.size(); i++) {
      String sqlValue = EntityUtil.getSQLStringValue(property, values.get(i));
      if (!caseSensitive)
        sqlValue = "upper(" + sqlValue + ")";
      ret.append(sqlValue);
      if (cnt++ == 1000 && i < values.size()-1) {//Oracle limit
        ret.append(") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < values.size()-1)
        ret.append(", ");
    }
    ret.append(")");

    return ret.toString();
  }
}
