/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * A combo box model based on a single entity property.
 */
public class PropertyComboBoxModel extends FilteredComboBoxModel {

  /**
   * The ID of the entity
   */
  private final String entityID;

  /**
   * The EntityDbProvider instance to use when populating this combo box model
   */
  private final EntityDbProvider dbProvider;

  /**
   * The property which values should be shown in this combo box model
   */
  private final Property property;

  /**
   * @param entityID the ID of the underlying entity
   * @param property the underlying property
   * @param dbProvider a EntityDbProvider instance
   * @param nullValue the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public PropertyComboBoxModel(final String entityID, final Property property, final EntityDbProvider dbProvider,
                               final String nullValue, final Event refreshEvent) {
    super(true, nullValue);
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(dbProvider);
    Util.rejectNullValue(property);
    if (property instanceof Property.ForeignKeyProperty) {
      throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel for a reference property "
              + property.getPropertyID() + ",\nuse an EntityComboBoxModel instead!");
    }
    if (property.isNullable()) {
      setNullValueString("");
    }
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.property = property;
    if (refreshEvent != null) {
      refreshEvent.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          refresh();
        }
      });
    }
  }

  @Override
  protected List<?> getContents() {
    try {
      return dbProvider.getEntityDb().selectPropertyValues(entityID, property.getPropertyID(), true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
