/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.domain.Property;

import javax.swing.table.TableColumnModel;
import java.util.List;

/**
 * User: darri
 * Date: 29.6.2010
 * Time: 10:05:38
 */
public interface EntityTableColumnModel extends TableColumnModel {

  String getEntityID();

  List<Property> getColumnProperties();
}
