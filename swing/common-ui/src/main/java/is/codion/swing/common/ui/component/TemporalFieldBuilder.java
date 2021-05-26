/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.temporal.Temporal;

/**
 * A builder for {@link TemporalField}.
 */
public interface TemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>, B extends TemporalFieldBuilder<T, C, B>>
        extends TextFieldBuilder<T, C, B> {

  /**
   * @param focusLostBehaviour the focus lost behaviour, JFormattedTextField.COMMIT by default
   * @return this builder instance
   * @see javax.swing.JFormattedTextField#COMMIT
   * @see javax.swing.JFormattedTextField#COMMIT_OR_REVERT
   * @see javax.swing.JFormattedTextField#REVERT
   * @see javax.swing.JFormattedTextField#PERSIST
   */
  B focusLostBehaviour(int focusLostBehaviour);
}
