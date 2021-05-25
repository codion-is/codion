/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JLabel;

/**
 * A builder for JLabel.
 */
public interface LabelBuilder extends ComponentBuilder<String, JLabel, LabelBuilder> {

  /**
   * @param text the label text
   * @return
   */
  LabelBuilder text(String text);
}
