/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

/**
 * A {@link Value.Validator} restricting the maximum length of a string value.
 */
public final class StringLengthValidator implements Value.Validator<String> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(StringLengthValidator.class.getName());

  private int maximumLength;

  /**
   * @param maximumLength the maximum length, -1 for no limit
   */
  public StringLengthValidator(final int maximumLength) {
    this.maximumLength = maximumLength;
  }

  /**
   * @return the maximum length
   */
  public int getMaximumLength() {
    return maximumLength;
  }

  /**
   * @param maximumLength the maximum length, -1 for no limit
   */
  public void setMaximumLength(final int maximumLength) {
    this.maximumLength = maximumLength < 0 ? -1 : maximumLength;
  }

  @Override
  public void validate(final String text) {
    if (maximumLength >= 0 && text.length() > maximumLength) {
      throw new IllegalArgumentException(MESSAGES.getString("length_exceeds_maximum") + " " + maximumLength);
    }
  }
}
