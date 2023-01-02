/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

/**
 * A {@link Value.Validator} restricting the maximum length of a string value.
 */
final class StringLengthValidator implements Value.Validator<String> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(StringLengthValidator.class.getName());

  private int maximumLength;

  /**
   * @param maximumLength the maximum length, -1 for no limit
   */
  StringLengthValidator(int maximumLength) {
    this.maximumLength = maximumLength;
  }

  /**
   * @return the maximum length
   */
  int getMaximumLength() {
    return maximumLength;
  }

  /**
   * @param maximumLength the maximum length, -1 for no limit
   */
  void setMaximumLength(int maximumLength) {
    this.maximumLength = maximumLength < 0 ? -1 : maximumLength;
  }

  @Override
  public void validate(String text) {
    if (maximumLength >= 0 && text.length() > maximumLength) {
      throw new IllegalArgumentException(MESSAGES.getString("length_exceeds_maximum") + " " + maximumLength);
    }
  }
}
