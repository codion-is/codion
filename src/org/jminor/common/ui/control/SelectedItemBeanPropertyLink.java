/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SelectedItemBeanPropertyLink extends BeanPropertyLink implements ItemListener {

  private final ComboBoxModel comboBoxModel;

  public SelectedItemBeanPropertyLink(final JComboBox box, final Object owner, final String propertyName,
                                      final Class propertyClass, final Event propertyChangeEvent,
                                      final String text) {
    this(box, owner, propertyName, propertyClass, propertyChangeEvent, text, LinkType.READ_WRITE);
  }

  public SelectedItemBeanPropertyLink(final JComboBox box, final Object owner, final String propertyName,
                                      final Class propertyClass, final Event propertyChangeEvent,
                                      final String text, final LinkType linkType) {
    super(owner, propertyName, propertyClass, propertyChangeEvent, text, linkType);
    this.comboBoxModel = box.getModel();
    updateUI();
    box.addItemListener(this);
  }

  /** {@inheritDoc} */
  public void itemStateChanged(final ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED)
      updateModel();
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    if (comboBoxModel instanceof ItemComboBoxModel)
      return ((ItemComboBoxModel.IItem) comboBoxModel.getSelectedItem()).getItem();
    else
      return comboBoxModel.getSelectedItem();
  }

  /** {@inheritDoc} */
  protected void setUIPropertyValue(final Object propertyValue) {
    comboBoxModel.setSelectedItem(propertyValue);
  }
}
