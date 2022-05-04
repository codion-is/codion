/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.ComponentBuilder;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Insets;
import java.awt.event.ActionListener;

import static java.util.Objects.requireNonNull;

/**
 * Builds buttons.
 * @param <T> the value type
 * @param <C> the button type
 * @param <B> the builder type
 */
public interface ButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>> extends ComponentBuilder<T, C, B> {

  /**
   * @param caption the caption
   * @return this builder instance
   * @see JButton#setText(String)
   */
  B caption(String caption);

  /**
   * @param mnemonic the mnemonic
   * @return this builder instance
   * @see JButton#setMnemonic(int)
   */
  B mnemonic(int mnemonic);

  /**
   * Note that setting this to false overrides caption from the action, if one is specified.
   * @param includeCaption specifies whether a caption should be included
   * @return this builder instance
   */
  B includeCaption(boolean includeCaption);

  /**
   * @param icon the icon
   * @return this builder instance
   * @see JButton#setIcon(Icon)
   */
  B icon(Icon icon);

  /**
   * @param insets the margin insets
   * @return this builder instance
   * @see JButton#setMargin(Insets)
   */
  B margin(Insets insets);

  /**
   * @param action the button action
   * @return this builder instance
   * @see JButton#setAction(Action)
   */
  B action(Action action);

  /**
   * @param actionListener the action listener
   * @return this builder instance
   * @see JButton#addActionListener(ActionListener)
   */
  B actionListener(ActionListener actionListener);

  /**
   * @param <B> the builder type
   * @return a builder for a JButton
   */
  static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> builder() {
    return new DefaultButtonBuilder<>(null);
  }

  /**
   * @param <B> the builder type
   * @param action the button action
   * @return a builder for a JButton
   */
  static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> builder(Action action) {
    return new DefaultButtonBuilder<>(requireNonNull(action));
  }
}
