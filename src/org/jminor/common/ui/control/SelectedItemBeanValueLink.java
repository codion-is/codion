/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Binds a JComboBox to an Object based bean property.
 */
public final class SelectedItemBeanValueLink extends AbstractBeanValueLink {

  private final ComboBoxModel comboBoxModel;

  public SelectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                   final Class propertyClass, final EventObserver valueChangeEvent) {
    this(box, owner, propertyName, propertyClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  public SelectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                   final Class propertyClass, final EventObserver valueChangeEvent,
                                   final LinkType linkType) {
    super(owner, propertyName, propertyClass, valueChangeEvent, linkType);
    this.comboBoxModel = box.getModel();
    updateUI();
    box.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateModel();
        }
      }
    });
  }

  @Override
  protected Object getUIValue() {
    if (comboBoxModel instanceof ItemComboBoxModel) {
      return ((Item) comboBoxModel.getSelectedItem()).getItem();
    }
    else {
      return comboBoxModel.getSelectedItem();
    }
  }

  @Override
  protected void setUIValue(final Object value) {
    comboBoxModel.setSelectedItem(value);
  }
}
