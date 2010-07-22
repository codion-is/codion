/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Property;

/**
 * A combo box model based on a single entity property.
 */
public interface PropertyComboBoxModel extends FilteredComboBoxModel {

  /**
   * @return the column property this combo box model is based on
   */
  Property.ColumnProperty getProperty();

  /**
   * @return the db provider instance used by this combo box model
   */
  EntityDbProvider getDbProvider();
}
