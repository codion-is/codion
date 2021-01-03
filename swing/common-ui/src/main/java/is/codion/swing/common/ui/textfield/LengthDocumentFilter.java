/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

/**
 * A DocumentFilter restricting the maximum length of the string the document can contain
 */
public class LengthDocumentFilter extends ParsingDocumentFilter<String> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LengthDocumentFilter.class.getName());

  private int maxLength;

  /**
   */
  public LengthDocumentFilter() {
    this(-1);
  }

  /**
   * @param maxLength the maximum length of the string the document can contain
   */
  public LengthDocumentFilter(final int maxLength) {
    setMaxLength(maxLength);
    addValidator(new LengthValidator());
  }

  /**
   * @return the max length used by this filter
   */
  public final int getMaxLength() {
    return maxLength;
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public final void setMaxLength(final int maxLength) {
    this.maxLength = maxLength < 0 ? -1 : maxLength;
  }

  @Override
  protected ParseResult<String> parse(final String text) {
    return parseResult(text, text, 0, true);
  }

  private final class LengthValidator implements Value.Validator<String> {
    @Override
    public void validate(final String text) throws IllegalArgumentException {
      if (maxLength >= 0 && text.length() > maxLength) {
        throw new IllegalArgumentException(MESSAGES.getString("length_exceeds_maximum") + " " + maxLength);
      }
    }
  }
}
