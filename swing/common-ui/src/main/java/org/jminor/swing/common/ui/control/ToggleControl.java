/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.StateObserver;

import javax.swing.ButtonModel;
import java.beans.PropertyChangeEvent;

import static java.util.Objects.requireNonNull;

/**
 * A Control for toggling a button model
 */
public final class ToggleControl extends Control {

  private final ButtonModel buttonModel;

  /**
   * @param name the name
   * @param buttonModel the button model
   * @param enabledObserver an observer indicating when this control should be enabled
   */
  public ToggleControl(final String name, final ButtonModel buttonModel, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.buttonModel = requireNonNull(buttonModel);
    if (enabledObserver != null) {
      enabledObserver.addDataListener(buttonModel::setEnabled);
      buttonModel.setEnabled(enabledObserver.get());
    }
    addPropertyChangeListener(this::onPropertyChange);
  }

  private void onPropertyChange(final PropertyChangeEvent changeEvent) {
    if (MNEMONIC_KEY.equals(changeEvent.getPropertyName())) {
      buttonModel.setMnemonic((Integer) changeEvent.getNewValue());
    }
  }

  /**
   * @return the button model
   */
  public ButtonModel getButtonModel() {
    return buttonModel;
  }
}
