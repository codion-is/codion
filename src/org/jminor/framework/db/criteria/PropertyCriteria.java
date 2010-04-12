/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.SearchType;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

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
  public String asString(final Database database) {
    return CriteriaUtil.getConditionString(this, database);
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

  public String getSqlValue(final String sqlStringValue) {
    return property.getPropertyType() == Type.STRING && !caseSensitive ? "upper(" + sqlStringValue + ")" : sqlStringValue;
  }
}
