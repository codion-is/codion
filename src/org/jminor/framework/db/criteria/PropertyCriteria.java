/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.dbms.Database;
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

  /** {@inheritDoc} */
  public String asString(final Database database) {
    if (property instanceof Property.ForeignKeyProperty)
      return getForeignKeyCriteriaString(database);

    final boolean isNullCriteria = values.size() == 1 && Entity.isValueNull(property.getPropertyType(), values.get(0));
    final String columnIdentifier = initColumnIdentifier(isNullCriteria);
    if (isNullCriteria)
      return columnIdentifier + (searchType == SearchType.LIKE ? " is null" : " is not null");

    final String sqlValue = getSqlValue(EntityUtil.getSQLStringValue(database, property, values.get(0)));
    final String sqlValue2 = values.size() == 2 ? getSqlValue(EntityUtil.getSQLStringValue(database, property, values.get(1))) : null;

    switch(searchType) {
      case LIKE:
        return getLikeCondition(database, columnIdentifier, sqlValue);
      case NOT_LIKE:
        return getNotLikeCondition(database, columnIdentifier, sqlValue);
      case AT_LEAST:
        return columnIdentifier + " <= " + sqlValue;
      case AT_MOST:
        return columnIdentifier + " >= " + sqlValue;
      case WITHIN_RANGE:
        return "(" + columnIdentifier + " >= " + sqlValue + " and " + columnIdentifier +  " <= " + sqlValue2 + ")";
      case OUTSIDE_RANGE:
        return "(" + columnIdentifier + " <= "+ sqlValue + " or " + columnIdentifier + " >= " + sqlValue2 + ")";
    }

    throw new IllegalArgumentException("Unknown search type" + searchType);
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

  private String getForeignKeyCriteriaString(final Database database) {
    if (values.size() > 1)
      return getMultipleColumnForeignKeyCriteriaString(database);

    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
    final Entity.Key entityKey = (Entity.Key) values.get(0);
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) property).getReferencedEntityID());
    for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
      set.addCriteria(new PropertyCriteria(
              ((Property.ForeignKeyProperty) property).getReferenceProperties().get(keyProperty.getIndex()),
              searchType, entityKey == null ? null : entityKey.getValue(keyProperty.getPropertyID())));

    return set.asString(database);
  }

  private String getMultipleColumnForeignKeyCriteriaString(final Database database) {
    final Collection<Property.PrimaryKeyProperty > primaryKeyProperties =
            EntityRepository.getPrimaryKeyProperties(((Property.ForeignKeyProperty) property).getReferencedEntityID());
    if (primaryKeyProperties.size() > 1) {
      final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR);
      for (final Object entityKey : values) {
        final CriteriaSet pkSet = new CriteriaSet(CriteriaSet.Conjunction.AND);
        for (final Property.PrimaryKeyProperty keyProperty : primaryKeyProperties)
          pkSet.addCriteria(new PropertyCriteria(
                  ((Property.ForeignKeyProperty) property).getReferenceProperties().get(keyProperty.getIndex()),
                  searchType, ((Entity.Key) entityKey).getValue(keyProperty.getPropertyID())));

        set.addCriteria(pkSet);
      }

      return set.asString(database);
    }
    else
      return getInList(database, ((Property.ForeignKeyProperty) property).getReferenceProperties().get(0).getPropertyID(),
              searchType == SearchType.NOT_LIKE);
  }

  private String getNotLikeCondition(final Database database, final String columnIdentifier, final String sqlValue) {
    return values.size() > 1 ? getInList(database, columnIdentifier, true) :
            columnIdentifier + (property.getPropertyType() == Type.STRING && containsWildcard(sqlValue)
            ? " not like " + sqlValue : " <> " + sqlValue);
  }

  private String getLikeCondition(final Database database, final String columnIdentifier, final String sqlValue) {
    return values.size() > 1 ? getInList(database, columnIdentifier, false) :
            columnIdentifier + (property.getPropertyType() == Type.STRING && containsWildcard(sqlValue)
            ? " like " + sqlValue : " = " + sqlValue);
  }

  private boolean containsWildcard(final String val) {
    return val != null && val.length() > 0 && val.indexOf(wildcard) > -1;
  }

  private String getInList(final Database database, final String whereColumn, final boolean notIn) {
    final StringBuilder stringBuilder = new StringBuilder("(").append(whereColumn).append((notIn ? " not in (" : " in ("));
    int cnt = 1;
    for (int i = 0; i < values.size(); i++) {
      final String sqlValue = EntityUtil.getSQLStringValue(database, property, values.get(i));
      if (property.getPropertyType() == Type.STRING && !caseSensitive)
        stringBuilder.append("upper(").append(sqlValue).append(")");
      else
        stringBuilder.append(sqlValue);
      if (cnt++ == 1000 && i < values.size()-1) {//Oracle limit
        stringBuilder.append(notIn ? ") and " : ") or ").append(whereColumn).append(" in (");
        cnt = 1;
      }
      else if (i < values.size()-1)
        stringBuilder.append(", ");
    }
    stringBuilder.append("))");

    return stringBuilder.toString();
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
      ret.addAll(getValues(values));

    return ret;
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

  private String initColumnIdentifier(final boolean isNullCriteria) {
    String columnName;
    if (property instanceof Property.SubqueryProperty)
      columnName = "("+((Property.SubqueryProperty)property).getSubQuery()+")";
    else
      columnName = property.getPropertyID();

    if (!isNullCriteria && property.getPropertyType() == Type.STRING && !caseSensitive)
      columnName = "upper(" + columnName + ")";

    return columnName;
  }

  private String getSqlValue(final String sqlStringValue) {
    return property.getPropertyType() == Type.STRING && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
  }
}
