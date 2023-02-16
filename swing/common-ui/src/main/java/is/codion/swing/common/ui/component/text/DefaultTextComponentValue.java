/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
