/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

/**
 * A {@link Validator} restricting the maximum length of a string value.
 */
public final class StringLengthValidator implements Value.Validator<String> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(StringLengthValidator.class.getName());

  private int maxLength;

  private StringLengthValidator(final int maxLength) {
    setMaxLength(maxLength);
  }

  /**
   * @return the max length used by this validator
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * @param maxLength the maximum length of the string to allow, -1 if unlimited
   */
  public void setMaxLength(final int maxLength) {
    this.maxLength = maxLength < 0 ? -1 : maxLength;
  }

  @Override
  public void validate(final String text) throws IllegalArgumentException {
    if (maxLength >= 0 && text.length() > maxLength) {
      throw new IllegalArgumentException(MESSAGES.getString("length_exceeds_maximum") + " " + maxLength);
    }
  }

  /**
   * @return a new StringLengthValidator instance
   */
  public static StringLengthValidator stringLengthValidator() {
    return new StringLengthValidator(-1);
  }

  /**
   * @param maxLength the maximum string length
   * @return a new StringLengthValidator instance
   */
  public static StringLengthValidator stringLengthValidator(final int maxLength) {
    return new StringLengthValidator(maxLength);
  }
}
