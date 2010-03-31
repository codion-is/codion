/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Binds a JComboBox to an Object based bean property.
 */
public class SelectedItemBeanPropertyLink extends BeanPropertyLink {

  private final ComboBoxModel comboBoxModel;

  public SelectedItemBeanPropertyLink(final JComboBox box, final Object owner, final String propertyName,
                                      final Class propertyClass, final Event propertyChangeEvent) {
    this(box, owner, propertyName, propertyClass, propertyChangeEvent, LinkType.READ_WRITE);
  }

  public SelectedItemBeanPropertyLink(final JComboBox box, final Object owner, final String propertyName,
                                      final Class propertyClass, final Event propertyChangeEvent,
                                      final LinkType linkType) {
    super(owner, propertyName, propertyClass, propertyChangeEvent, linkType);
    this.comboBoxModel = box.getModel();
    updateUI();
    box.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
          updateModel();
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    if (comboBoxModel instanceof ItemComboBoxModel)
      return ((ItemComboBoxModel.Item) comboBoxModel.getSelectedItem()).getItem();
    else
      return comboBoxModel.getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    comboBoxModel.setSelectedItem(propertyValue);
  }
}
