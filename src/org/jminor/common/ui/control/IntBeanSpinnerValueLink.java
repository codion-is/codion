/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Binds a SpinnerModel to a int based bean property.
 */
final class IntBeanSpinnerValueLink extends AbstractValueLink {

  private final SpinnerNumberModel spinnerModel;

  /**
   * Instantiates a new IntBeanSpinnerValueLink.
   * @param modelValue the model value
   * @param linkType the link type
   * @param spinnerModel the spinner model to use
   */
  IntBeanSpinnerValueLink(final ModelValue modelValue, final LinkType linkType, final SpinnerNumberModel spinnerModel) {
    super(modelValue, linkType);
    this.spinnerModel = spinnerModel;
    this.spinnerModel.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      @Override
      public void stateChanged(final ChangeEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return spinnerModel.getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    spinnerModel.setValue(value);
  }
}
