/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchModel;
import org.jminor.framework.domain.Property;

import java.sql.Timestamp;
import java.util.Date;

/**
 * A class for filtering a set of entities based on a property.
 */
@SuppressWarnings({"unchecked"})
public interface PropertyFilterModel extends SearchModel<Property> {

  /**
   * @param value the upper bound
   */
  void setUpperBound(final String value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Double value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Integer value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final boolean value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final char value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Boolean value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Timestamp value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Date value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final String value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Double value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Integer value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final boolean value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final char value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Boolean value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Timestamp value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Date value);

  boolean include(final Comparable comparable);

  void setLikeValue(final Comparable value);
}