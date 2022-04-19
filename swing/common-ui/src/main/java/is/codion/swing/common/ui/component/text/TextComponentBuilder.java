/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.ComponentBuilder;

import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Insets;

/**
 * A builder for text components.
 */
public interface TextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
        extends ComponentBuilder<T, C, B> {

  /**
   * @param editable false if the component should not be editable
   * @return this builder instance
   */
  B editable(boolean editable);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  B updateOn(UpdateOn updateOn);

  /**
   * @param upperCase if true the text component convert all lower case input to upper case
   * @return this builder instance
   */
  B upperCase(boolean upperCase);

  /**
   * @param lowerCase if true the text component convert all upper case input to lower case
   * @return this builder instance
   */
  B lowerCase(boolean lowerCase);

  /**
   * @param maximumLength the maximum text length
   * @return this builder instance
   */
  B maximumLength(int maximumLength);

  /**
   * @param margin the margin
   * @return this builder instance
   */
  B margin(Insets margin);

  /**
   * @param controlDeleteWord true if CTRL-DEL and CTRL-BACKSPACE should delete the next/previous word
   * @return this builder instance
   */
  B controlDeleteWord(boolean controlDeleteWord);

  /**
   * @param disabledTextColor the color used to render disabled text
   * @return this builder instance
   */
  B disabledTextColor(Color disabledTextColor);
}
