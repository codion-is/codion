/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A Document implementation which allows for setting the max text length, a numerical range
 * for allowed values, and automatic conversion to upper case and lower case.
 */
public class SizedDocument extends PlainDocument {

  private boolean upperCase = false;
  private boolean lowerCase = false;
  private int maxLength = -1;

  /**
   * @param upperCase true if this text field should automatically convert text to uppercase
   */
  public final void setUpperCase(final boolean upperCase) {
    this.upperCase = upperCase;
    this.lowerCase = false;
  }

  /**
   * @return true if this document converts all input to upper case
   */
  public final boolean isUpperCase() {
    return upperCase;
  }

  /**
   * @param lowerCase true if this text field should automatically convert text to lowercase
   */
  public void setLowerCase(final boolean lowerCase) {
    this.lowerCase = lowerCase;
    this.upperCase = false;
  }

  /**
   * @return true if this document converts all input to lower case
   */
  public boolean isLowerCase() {
    return lowerCase;
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public final int getMaxLength() {
    return maxLength;
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public final void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  /** {@inheritDoc} */
  @Override
  public void insertString(final int offset, final String string, final AttributeSet attributeSet) throws BadLocationException {
    if (getMaxLength() > 0 && getLength() + string.length() > getMaxLength()) {
      return;
    }
    final String documentText = getText(0, getLength());
    String toInsert = prepareString(string, documentText, offset);
    if (upperCase) {
      toInsert = string.toUpperCase(Locale.getDefault());
    }
    if (lowerCase) {
      toInsert = string.toLowerCase(Locale.getDefault());
    }
    if (validValue(toInsert, documentText, offset)) {
      super.insertString(offset, toInsert, attributeSet);
    }
    postInsert();
  }

  protected void postInsert() {/*Provided for subclasses*/}

  /**
   * @param string the string to check
   * @param documentText the text currently in the document
   * @param offset the offset at which the string is being added
   * @return true if the document value is valid after the given text has been added to it
   */
  protected boolean validValue(final String string, final String documentText, final int offset) {
    return true;
  }

  /**
   * Allows for manipulation of the string being inserted
   * @param string the string being added to this document
   * @param documentText the text currently in the document
   * @param offset the offset at which the string is being added
   * @return the string to actually insert
   */
  protected String prepareString(final String string, final String documentText, final int offset) {
    return string;
  }
}
