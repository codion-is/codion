/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.text.JTextComponent;

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
   * @param columns the number of colums in the text component
   * @return this builder instance
   */
  B columns(int columns);

  /**
   * @param upperCase if true the text component convert all lower case input to upper case
   * @return this builder instance
   */
  B upperCase(final boolean upperCase);

  /**
   * @param lowerCase if true the text component convert all upper case input to lower case
   * @return this builder instance
   */
  B lowerCase(final boolean lowerCase);

  /**
   * @param maximumLength the maximum text length
   * @return this builder instance
   */
  B maximumLength(int maximumLength);
}
