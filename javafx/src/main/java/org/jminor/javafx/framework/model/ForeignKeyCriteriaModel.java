/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.List;

public final class ForeignKeyCriteriaModel extends PropertyCriteriaModel<Property.ForeignKeyProperty> {

  private final EntityConnectionProvider connectionProvider;

  public ForeignKeyCriteriaModel(final Property.ForeignKeyProperty foreignKeyProperty,
                                 final EntityConnectionProvider connectionProvider) {
    super(foreignKeyProperty);
    this.connectionProvider = connectionProvider;
  }

  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public void setCriteria(final List<Entity> entities) {
    getUpperBoundValue().set(entities);
    getSearchTypeValue().set(SearchType.LIKE);
    getEnabledState().setActive(!entities.isEmpty());
  }
}
