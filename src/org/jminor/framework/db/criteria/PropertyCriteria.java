/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.SearchType;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbUtil;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A object for encapsulating a query criteria with a single property and one or more values
 */
public class PropertyCriteria implements ICriteria {

  /**
   * The property used in this criteria
   */
  private final Property property;

  /**
   * The values used in this criteria
   */
  private final List<Object> values = new ArrayList<Object>();

  /**
   * The search type used in this criteria
   */
  private final SearchType searchType;

  /**
   * The wildcard being used
   */
  private final String wildcard = (String) FrameworkSettings.get().getProperty(FrameworkSettings.WILDCARD_CHARACTER);

  /**
   * True if this criteria should be case sensitive, only applies for criterias based on string properties
   */
  private boolean caseSensitive = true;

  /**
   * Instantiates a new PropertyCriteria instance
   * @param property the property
   * @param searchType the search type
   * @param values the values
   */
  public PropertyCriteria(final Property property, final SearchType searchType, final Object... values) {
    this.property = property;
    this.searchType = searchType;
    setValues(values);
  }

  /**
   * @param values the values to use in this criteria
   */
  @SuppressWarnings({"unchecked"})
  public void setValues(final Object... values) {
    this.values.clear();
    if (values == null)
      this.values.add(null);
    else
      this.values.addAll(getValues(values));
  }

  /**
   * @return the values used by this criteria
   */
  public List<Object> getValues() {
    return this.values;
  }

  /**
   * @return the search type used by this criteria
   */
  public SearchType getSearchType() {
    return this.searchType;
  }

  /** {@inheritDoc} */
  public String toString() {
    return getConditionString();
  }

  /**
   * @return the SQL condition string this criteria represents, i.e. propertyName = 'value'
   */
  public String getConditionString() {
    if (property instanceof Property.EntityProperty)
      return getReferenceCriteriaString();

    String columnName;
    if (property instanceof Property.SubQueryProperty)
      columnName = "("+((Property.SubQueryProperty)property).getSubQuery()+")";
    else
      columnName = property.propertyID;

    if (values.size() == 0)
      throw new RuntimeException("No values specified for PropertyCriteria: " + property);
    if (values.size() == 1 && Entity.isValueNull(property.getPropertyType(), values.get(0)))
      return columnName + (searchType == SearchType.LIKE ? " is null" : " is not null");

    String sqlValue = EntityDbUtil.getSQLStringValue(property, values.get(0));
    String sqlValue2 = values.size() == 2 ? EntityDbUtil.getSQLStringValue(property, values.get(1)) : null;

    if (property.getPropertyType() == Type.STRING && !caseSensitive) {
      columnName = "upper(" + columnName + ")";
      sqlValue = "upper(" + sqlValue + ")";
      sqlValue2 = "upper(" + sqlValue2 + ")";
    }

    switch(searchType) {
      case LIKE:
        if (values.size() > 1)
          return getInList(columnName, false);
        else
          return columnName + (property.getPropertyType() == Type.STRING && containsWildcard(sqlValue)
                ? " like " + sqlValue : " = " + sqlValue);
      case NOT_LIKE:
        if (values.size() > 1)
          return getInList(columnName, true);
        else
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
        return getInList(columnName, false);
    }

    throw new IllegalArgumentException("Unknown search type" + searchType);
  }

  /**
   * Sets whether this criteria should be case sensitive, only applies to criterias base on string properties
   * @param value if true then this criteria is case sensitive, false otherwise
   * @return this PropertyCriteria instance
   */
  public PropertyCriteria setCaseSensitive(final boolean value) {
    this.caseSensitive = value;
    return this;
  }

  /**
   * @return true if this criteria is case sensitive (only applies to criterias based on string properties)
   */
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
      return getInList(((Property.EntityProperty) property).referenceProperties.get(0).propertyID, false);
  }

  private boolean containsWildcard(final String val) {
    return val != null && val.length() > 0 && val.indexOf(wildcard) > -1;
  }

  private String getInList(final String whereColumn, final boolean notIn) {
    final StringBuffer ret = new StringBuffer(whereColumn + (notIn ? " not" : "") + " in (");
    int cnt = 1;
    for (int i = 0; i < values.size(); i++) {
      String sqlValue = EntityDbUtil.getSQLStringValue(property, values.get(i));
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

  @SuppressWarnings({"unchecked"})
  private Collection getValues(final Object... values) {
    if (values.length == 1 && values[0] instanceof Collection)
      return getValues(((Collection) values[0]).toArray());
    else if (values.length > 0 && values[0] instanceof Entity)
      return EntityUtil.getPrimaryKeys((Collection) Arrays.asList(values));
    else
      return Arrays.asList(values);
  }
}
