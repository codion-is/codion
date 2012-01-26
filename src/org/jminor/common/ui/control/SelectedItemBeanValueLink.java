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

  /**
   * Instantiates a new SelectedItemBeanValueLink.
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public SelectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                   final Class valueClass, final EventObserver valueChangeEvent) {
    this(box, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new SelectedItemBeanValueLink.
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public SelectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                   final Class valueClass, final EventObserver valueChangeEvent,
                                   final LinkType linkType) {
    super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    this.comboBoxModel = box.getModel();
    updateUI();
    box.addItemListener(new ItemListener() {
      /** {@inheritDoc} */
      @Override
      public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateModel();
        }
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    if (comboBoxModel instanceof ItemComboBoxModel) {
      return ((Item) comboBoxModel.getSelectedItem()).getItem();
    }
    else {
      return comboBoxModel.getSelectedItem();
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    comboBoxModel.setSelectedItem(value);
  }
}
