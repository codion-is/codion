/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.combobox.DefaultFilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A combo box model based on a single entity property.
 */
public class DefaultPropertyComboBoxModel extends DefaultFilteredComboBoxModel {

  private final ValueCollectionProvider<Object> valueProvider;

  /**
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance
   * @param property the underlying property
   * @param nullValueString the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final String entityID, final EntityDbProvider dbProvider,
                                      final Property.ColumnProperty property, final String nullValueString,
                                      final EventObserver refreshEvent) {
    this(new ValueCollectionProvider<Object>() {
      public Collection<Object> getValues() {
        try {
          return dbProvider.getEntityDb().selectPropertyValues(entityID, property.getPropertyID(), true);
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, nullValueString == null ? (property.isNullable() ? "" : null) : nullValueString, refreshEvent);
  }

  /**
   * @param valueProvider provides the values to show in this combo box model
   * @param nullValueString the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final ValueCollectionProvider<Object> valueProvider, final String nullValueString,
                                      final EventObserver refreshEvent) {
    super(nullValueString);
    this.valueProvider = valueProvider;
    if (refreshEvent != null) {
      refreshEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          refresh();
        }
      });
    }
  }

  @Override
  protected final List<?> initializeContents() {
    try {
      return new ArrayList<Object>(valueProvider.getValues());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
