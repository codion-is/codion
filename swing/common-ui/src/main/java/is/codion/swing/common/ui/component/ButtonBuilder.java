/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.AbstractButton;
import javax.swing.Icon;

/**
 * Builds buttons.
 * @param <T> the value type
 * @param <C> the button type
 * @param <B> the builder type
 */
public interface ButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>> extends ComponentBuilder<T, C, B>{

  /**
   * @param caption the caption
   * @return this builder instance
   */
  B caption(String caption);

  /**
   * @param mnemonic the mnemonic
   * @return this builder instance
   */
  B mnemonic(int mnemonic);

  /**
   * @param includeCaption specifies whether a caption should be included
   * @return this builder instance
   */
  B includeCaption(boolean includeCaption);

  /**
   * @param icon the icon
   * @return this builder instance
   */
  B icon(Icon icon);
}
