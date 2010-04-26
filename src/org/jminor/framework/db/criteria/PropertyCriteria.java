/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.SearchType;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A object for encapsulating a query criteria with a single property and one or more values.
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
  private final List<Object> values;

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
    if (property == null)
      throw new IllegalArgumentException("Property criteria requires a non-null property");
    if (searchType == null)
      throw new IllegalArgumentException("Property criteria requires a non-null search type");
    if (values != null && values.length == 0)
      throw new RuntimeException("No values specified for PropertyCriteria: " + property);
    this.property = property;
    this.searchType = searchType;
    this.values = initValues(values);
  }

  public Property getProperty() {
    return property;
  }

  public List<Object> getValues() {
    return values;
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public String getWildcard() {
    return wildcard;
  }

  /** {@inheritDoc} */
  public String asString(final Database database, final ValueProvider valueProvider) {
    return getConditionString(database, valueProvider);
  }

  public int getValueCount() {
    return getValues().size();
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
  private List<Object> initValues(final Object... values) {
    final List<Object> ret = new ArrayList<Object>();
    if (values == null)
      ret.add(null);
    else
      ret.addAll(getValueList(values));

    return ret;
  }

  @SuppressWarnings({"unchecked"})
  private Collection getValueList(final Object... values) {
    if (values.length == 1 && values[0] instanceof Collection)
      return getValueList(((Collection) values[0]).toArray());
    else if (values.length > 0 && values[0] instanceof Entity)
      return EntityUtil.getPrimaryKeys((Collection) Arrays.asList(values));
    else
      return Arrays.asList(values);
  }

  private String getSqlValue(final String sqlStringValue) {
    return property.isType(String.class) && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
  }

  private String getConditionString(final Database database, final ValueProvider valueProvider) {
    if (getProperty() instanceof Property.ForeignKeyProperty)
      return getForeignKeyCriteriaString(this, database, valueProvider);

    final boolean isNullCriteria = getValueCount() == 1 &&
            Entity.isValueNull(getProperty().getType(), getValues().get(0));
    final String columnIdentifier = initializeColumnIdentifier(isNullCriteria);
    if (isNullCriteria)
      return columnIdentifier + (getSearchType() == SearchType.LIKE ? " is null" : " is not null");

    final String sqlValue = getSqlValue(valueProvider.getSQLString(database,
            getProperty(), getValues().get(0)));
    final String sqlValue2 = getValueCount() == 2 ? getSqlValue(valueProvider.getSQLString(
            database, getProperty(), getValues().get(1))) : null;

    switch(getSearchType()) {
      case LIKE:
        return getLikeCondition(database, columnIdentifier, sqlValue, valueProvider);
      case NOT_LIKE:
        return getNotLikeCondition(database, columnIdentifier, sqlValue, valueProvider);
      case AT_LEAST:
        return columnIdentifier + " <= " + sqlValue;
      case AT_MOST:
        return columnIdentifier + " >= " + sqlValue;
      case WITHIN_RANGE:
        return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
      case OUTSIDE_RANGE:
        return "(" + columnIdentifier + " <= " + sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
    }

    throw new IllegalArgumentException("Unknown search type" + getSearchType());
  }

  private String getForeignKeyCriteriaString(final PropertyCriteria criteria, final Database database, final ValueProvider valueProvider) {
    if (criteria.getValueCount() > 1)
      return getMultipleColumnForeignKeyCriteriaString(database, valueProvider);

    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
    final Entity.Key entityKey = (Entity.Key) criteria.getValues().get(0);
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) criteria.getProperty()).getReferencedEntityID());
    for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
      set.addCriteria(new PropertyCriteria(
              ((Property.ForeignKeyProperty) criteria.getProperty()).getReferenceProperties().get(keyProperty.getIndex()),
              criteria.getSearchType(), entityKey == null ? null : entityKey.getValue(keyProperty.getPropertyID())));

    return set.asString(database, valueProvider);
  }

  private String getMultipleColumnForeignKeyCriteriaString(final Database database, final ValueProvider valueProvider) {
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) getProperty()).getReferencedEntityID());
    if (primaryKeyProperties.size() > 1) {
      final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR);
      for (final Object entityKey : getValues()) {
        final CriteriaSet pkSet = new CriteriaSet(CriteriaSet.Conjunction.AND);
        for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
          pkSet.addCriteria(new PropertyCriteria(
                  ((Property.ForeignKeyProperty) getProperty()).getReferenceProperties().get(keyProperty.getIndex()),
                  getSearchType(), ((Entity.Key) entityKey).getValue(keyProperty.getPropertyID())));

        set.addCriteria(pkSet);
      }

      return set.asString(database, valueProvider);
    }
    else
      return getInList(database, ((Property.ForeignKeyProperty) getProperty()).getReferenceProperties().get(0).getColumnName(),
              getSearchType() == SearchType.NOT_LIKE, valueProvider);
  }

  private String getInList(final Database database, final String whereColumn, final boolean notIn,
                           final ValueProvider valueProvider) {
    final StringBuilder stringBuilder = new StringBuilder("(").append(whereColumn).append((notIn ? " not in (" : " in ("));
    int cnt = 1;
    for (int i = 0; i < getValues().size(); i++) {
      final String sqlValue = valueProvider.getSQLString(database, getProperty(),
              getValues().get(i));
      if (getProperty().isType(String.class) && !isCaseSensitive())
        stringBuilder.append("upper(").append(sqlValue).append(")");
      else
        stringBuilder.append(sqlValue);
      if (cnt++ == 1000 && i < getValueCount() - 1) {//Oracle limit
        stringBuilder.append(notIn ? ") and " : ") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < getValueCount() - 1)
        stringBuilder.append(", ");
    }
    stringBuilder.append("))");

    return stringBuilder.toString();
  }

  private String getNotLikeCondition(final Database database, final String columnIdentifier, final String sqlValue,
                                     final ValueProvider valueProvider) {
    return getValueCount() > 1 ? getInList(database, columnIdentifier, true, valueProvider) :
            columnIdentifier + (getProperty().isType(String.class) && containsWildcard(sqlValue)
            ? " not like " + sqlValue : " <> " + sqlValue);
  }

  private String getLikeCondition(final Database database, final String columnIdentifier, final String sqlValue,
                                  final ValueProvider valueProvider) {
    return getValueCount() > 1 ? getInList(database, columnIdentifier, false, valueProvider) :
            columnIdentifier + (getProperty().isType(String.class) && containsWildcard(sqlValue)
            ? " like " + sqlValue : " = " + sqlValue);
  }

  private boolean containsWildcard(final String val) {
    return val != null && val.length() > 0 && val.indexOf(getWildcard()) > -1;
  }

  private String initializeColumnIdentifier(final boolean isNullCriteria) {
    String columnName;
    if (getProperty() instanceof Property.SubqueryProperty)
      columnName = "(" + ((Property.SubqueryProperty) getProperty()).getSubQuery() + ")";
    else
      columnName = getProperty().getColumnName();

    if (!isNullCriteria && getProperty().isType(String.class) && !isCaseSensitive())
      columnName = "upper(" + columnName + ")";

    return columnName;
  }
}
