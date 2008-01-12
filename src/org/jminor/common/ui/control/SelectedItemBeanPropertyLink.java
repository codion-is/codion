/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SelectedItemBeanPropertyLink extends BeanPropertyLink implements ItemListener {

  private final ComboBoxModel comboBoxModel;

  public SelectedItemBeanPropertyLink(final JComboBox box, final Object owner, final String propertyName,
                                      final Class propertyClass, final Event propertyChangeEvent,
                                      final String text, final LinkType linkType, final State enabledState) {
    super(owner, propertyName, propertyClass, propertyChangeEvent, text, linkType, enabledState);
    this.comboBoxModel = box.getModel();
    refreshUI();
    box.addItemListener(this);
  }

  /** {@inheritDoc} */
  public void itemStateChanged(final ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED)
      refreshProperty();
  }

  /** {@inheritDoc} */
  protected void updateProperty() {
    if (comboBoxModel instanceof ItemComboBoxModel)
      setPropertyValue(((ItemComboBoxModel.IItem) comboBoxModel.getSelectedItem()).getItem());
    else
      setPropertyValue(comboBoxModel.getSelectedItem());
  }

  /** {@inheritDoc} */
  protected void updateUI() {
    comboBoxModel.setSelectedItem(getPropertyValue());
  }
}
