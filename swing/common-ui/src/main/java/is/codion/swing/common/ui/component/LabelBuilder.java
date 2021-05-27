/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A builder for JLabel.
 */
public interface LabelBuilder extends ComponentBuilder<String, JLabel, LabelBuilder> {

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  LabelBuilder horizontalAlignment(int horizontalAlignment);

  /**
   * @param displayedMnemonic the label mnemonic
   * @return this builder instance
   */
  LabelBuilder displayedMnemonic(char displayedMnemonic);

  /**
   * @param component the component to associate with this label
   * @return this builder instance
   */
  LabelBuilder labelFor(final JComponent component);
}
