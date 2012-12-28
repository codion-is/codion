/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.StateObserver;

import javax.swing.ButtonModel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Binds a ButtonModel to a boolean based bean property.
 */
public class ToggleBeanValueLink extends AbstractValueLink {

  private final ButtonModel buttonModel;

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param buttonModel the button model to link with the value
   * @param modelValue the model value
   * @param caption the check box caption, if any
   * @param linkType the link type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  ToggleBeanValueLink(final ButtonModel buttonModel, final ModelValue modelValue, final String caption,
                      final LinkType linkType, final StateObserver enabledObserver) {
    super(modelValue, linkType, enabledObserver);
    this.buttonModel = buttonModel;
    this.buttonModel.addItemListener(new ItemListener() {
      /** {@inheritDoc} */
      @Override
      public void itemStateChanged(final ItemEvent e) {
        updateModel();
      }
    });
    setName(caption);
    updateUI();
  }

  /**
   * @return the button model
   */
  public final ButtonModel getButtonModel() {
    return buttonModel;
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    final Boolean booleanValue = (Boolean) value;
    buttonModel.setSelected(booleanValue != null && booleanValue);
  }
}
