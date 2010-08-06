/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.domain.Property;

import javax.swing.table.TableColumnModel;
import java.util.List;

/**
 * An interface describing a column model for entity tables
 */
public interface EntityTableColumnModel extends TableColumnModel {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the properties to base the columns on
   */
  List<Property> getColumnProperties();
}
