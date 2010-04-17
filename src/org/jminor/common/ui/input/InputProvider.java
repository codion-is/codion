/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * Specifies a generic class for providing a value via a UI component.
 */
public interface InputProvider<T> {

  /**
   * @return the input component
   */
  JComponent getInputComponent();

  /**
   * @return the value according to the input component
   */
  T getValue();
}
