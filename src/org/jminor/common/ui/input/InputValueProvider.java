/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * Specifies a generic class for providing a value via a UI component.
 */
public abstract class InputValueProvider<T> {

  private final JComponent inputComponent;

  public InputValueProvider(final JComponent inputComponent) {
    this.inputComponent = inputComponent;
  }

  public JComponent getInputComponent() {
    return this.inputComponent;
  }

  public abstract T getValue();
}
