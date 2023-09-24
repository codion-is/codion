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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.JTextComponent;
import java.text.Format;
import java.text.ParseException;

import static is.codion.common.NullOrEmpty.nullOrEmpty;

final class DefaultTextComponentValue<T, C extends JTextComponent> extends AbstractTextComponentValue<T, C> {

  private final Format format;

  DefaultTextComponentValue(C textComponent, Format format, UpdateOn updateOn) {
    super(textComponent, null, updateOn);
    this.format = format;
  }

  @Override
  protected T getComponentValue() {
    String text = component().getText();
    if (nullOrEmpty(text)) {
      return null;
    }

    return parseValueFromText(text);
  }

  @Override
  protected void setComponentValue(T value) {
    component().setText(value == null ? "" : (format == null ? value.toString() : format.format(value)));
  }

  /**
   * Returns a value based on the given text, if the text can not be parsed into a valid value, null is returned.
   * Only called for non-null text.
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  private T parseValueFromText(String text) {
    try {
      return (T) (format == null ? text : format.parseObject(text));
    }
    catch (ParseException e) {
      return null;
    }
  }
}
