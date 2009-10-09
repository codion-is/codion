/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.model.SearchType;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A object for encapsulating a query criteria with a single property and one or more values
 */
public class PropertyCriteria implements Criteria, Serializable {

  private static final long serialVersionUID = 1;

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
  private final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);

  /**
   * True if this criteria should be case sensitive, only applies to criterias based on string properties
   */
  private boolean caseSensitive = true;

  /**
   * Instantiates a new PropertyCriteria instance
   * @param property the property
   * @param searchType the search type
   * @param values the values
   */
  public PropertyCriteria(final Property property, final SearchType searchType, final Object... values) {
    if (property == null)
      throw new IllegalArgumentException("Property criteria requires a non-null property");
    if (searchType == null)
      throw new IllegalArgumentException("Property criteria requires a non-null search type");
    this.property = property;
    this.searchType = searchType;
    setValues(values);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getConditionString();
  }

  /**
   * Sets whether this criteria should be case sensitive, only applies to criterias based on string properties
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

  /**
   * @return the SQL condition string this criteria represents, i.e. propertyName = 'value',
   * this string should not contain the 'where' keyword
   */
  String getConditionString() {
    if (property instanceof Property.ForeignKeyProperty)
      return getForeignKeyCriteriaString();

    String columnName;
    if (property instanceof Property.SubqueryProperty)
      columnName = "("+((Property.SubqueryProperty)property).getSubQuery()+")";
    else
      columnName = property.getPropertyID();

    if (values.size() == 0)
      throw new RuntimeException("No values specified for PropertyCriteria: " + property);
    if (values.size() == 1 && Entity.isValueNull(property.getPropertyType(), values.get(0)))
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
      case AT_LEAST:
        return columnName + " <= " + sqlValue;
      case AT_MOST:
        return columnName + " >= " + sqlValue;
      case WITHIN_RANGE:
        return "(" + columnName + " >= " + sqlValue + " and " + columnName +  " <= " + sqlValue2 + ")";
      case OUTSIDE_RANGE:
        return "(" + columnName + " <= "+ sqlValue + " or " + columnName + " >= " + sqlValue2 + ")";
      case IN:
        return getInList(columnName, false);
    }

    throw new IllegalArgumentException("Unknown search type" + searchType);
  }

  private String getForeignKeyCriteriaString() {
    if (values.size() > 1)
      return getMultiColumnForeignKeyCriteriaString();

    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
    final Entity.Key entityKey = (Entity.Key) values.get(0);
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) property).referenceEntityID);
    for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
      set.addCriteria(new PropertyCriteria(
              ((Property.ForeignKeyProperty) property).referenceProperties.get(keyProperty.getIndex()),
              searchType, entityKey == null ? null : entityKey.getValue(keyProperty.getPropertyID())));

    return set.toString();
  }

  private String getMultiColumnForeignKeyCriteriaString() {
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) property).referenceEntityID);
    if (primaryKeyProperties.size() > 1) {
      final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR);
      for (final Object entityKey : values) {
        final CriteriaSet pkSet = new CriteriaSet(CriteriaSet.Conjunction.AND);
        for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
          pkSet.addCriteria(new PropertyCriteria(
                  ((Property.ForeignKeyProperty) property).referenceProperties.get(keyProperty.getIndex()),
                  searchType, ((Entity.Key) entityKey).getValue(keyProperty.getPropertyID())));

        set.addCriteria(pkSet);
      }

      return set.toString();
    }
    else
      return getInList(((Property.ForeignKeyProperty) property).referenceProperties.get(0).getPropertyID(),
              searchType == SearchType.NOT_LIKE);
  }

  private boolean containsWildcard(final String val) {
    return val != null && val.length() > 0 && val.indexOf(wildcard) > -1;
  }

  private String getInList(final String whereColumn, final boolean notIn) {
    final StringBuilder ret = new StringBuilder("(").append(whereColumn).append((notIn ? " not in (" : " in ("));
    int cnt = 1;
    for (int i = 0; i < values.size(); i++) {
      final String sqlValue = EntityUtil.getSQLStringValue(property, values.get(i));
      if (property.getPropertyType() == Type.STRING && !caseSensitive)
        ret.append("upper(").append(sqlValue).append(")");
      else
        ret.append(sqlValue);
      if (cnt++ == 1000 && i < values.size()-1) {//Oracle limit
        ret.append(notIn ? ") and " : ") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < values.size()-1)
        ret.append(", ");
    }
    ret.append("))");

    return ret.toString();
  }

  /**
   * @param values the values to use in this criteria
   */
  @SuppressWarnings({"unchecked"})
  private void setValues(final Object... values) {
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
}
