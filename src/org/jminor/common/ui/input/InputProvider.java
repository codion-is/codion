package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * User: darri
 * Date: 16.4.2010
 * Time: 15:58:09
 */
public interface InputProvider<T> {

  JComponent getInputComponent();

  T getValue();
}
