/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.DefaultFilteredComboBoxModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * A combo box model based on a single entity property.
 */
public class DefaultPropertyComboBoxModel extends DefaultFilteredComboBoxModel implements PropertyComboBoxModel {

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
  private final Property.ColumnProperty property;

  /**
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance
   * @param property the underlying property
   * @param nullValueString the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final String entityID, final EntityDbProvider dbProvider,
                                      final Property.ColumnProperty property, final String nullValueString,
                                      final Event refreshEvent) {
    super(nullValueString);
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(dbProvider, "dbProvider");
    Util.rejectNullValue(property, "property");
    if (property.isNullable()) {
      setNullValueString("");
    }
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.property = property;
    if (refreshEvent != null) {
      refreshEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          refresh();
        }
      });
    }
  }

  public final Property.ColumnProperty getProperty() {
    return property;
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  @Override
  protected List<?> initializeContents() {
    try {
      return dbProvider.getEntityDb().selectPropertyValues(entityID, property.getPropertyID(), true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
