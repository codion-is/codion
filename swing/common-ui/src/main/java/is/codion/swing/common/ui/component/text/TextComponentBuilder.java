/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * A builder for text components.
 */
public interface TextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
        extends ComponentBuilder<T, C, B> {

  /**
   * @param editable false if the component should not be editable
   * @return this builder instance
   * @see JTextComponent#setEditable(boolean)
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
   * @see JTextComponent#setMargin(Insets)
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
   * @see JTextComponent#setDisabledTextColor(Color)
   */
  B disabledTextColor(Color disabledTextColor);

  /**
   * @param selectedTextColor the color used to render selected text
   * @return this builder instance
   * @see JTextComponent#setSelectedTextColor(Color)
   */
  B selectedTextColor(Color selectedTextColor);

  /**
   * @param selectionColor the color used to render selected text background
   * @return this builder instance
   * @see JTextComponent#setSelectionColor(Color)
   */
  B selectionColor(Color selectionColor);

  /**
   * Makes the text component select all when it gains focus
   * @param selectAllOnFocusGained if true the component will select contents on focus gained
   * @return this builder instance
   */
  B selectAllOnFocusGained(boolean selectAllOnFocusGained);

  /**
   * @param moveCaretToEndOnFocusGained true if the caret should be moved to the end on focus gained
   * @return this builder instance
   */
  B moveCaretToEndOnFocusGained(boolean moveCaretToEndOnFocusGained);

  /**
   * @param moveCaretToStartOnFocusGained true if the caret should be moved to the start on focus gained
   * @return this builder instance
   */
  B moveCaretToStartOnFocusGained(boolean moveCaretToStartOnFocusGained);

  /**
   * @param caretListener the caret listener to add
   * @return this builder instance
   */
  B caretListener(CaretListener caretListener);

  /**
   * @param onTextChanged called when the text changes
   * @return this builder instance
   */
  B onTextChanged(Consumer<String> onTextChanged);

  /**
   * @param dragEnabled true if automatic drag handling should be enabled
   * @return this builder instance
   * @see JTextComponent#setDragEnabled(boolean)
   */
  B dragEnabled(boolean dragEnabled);

  /**
   * @param focusAcceleratorKey the focus accelerator key
   * @return this builder instance
   * @see JTextComponent#setFocusAccelerator(char)
   */
  B focusAccelerator(char focusAcceleratorKey);
}
