/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.framework.domain.Property;

/**
 * A base interface for searching a set of entities based on a property.
 */
public interface PropertySearchModel<T extends Property.SearchableProperty> extends ColumnSearchModel<T> {

  /**
   * @return a criteria object based on this search model
   */
  Criteria<Property.ColumnProperty> getCriteria();
}
