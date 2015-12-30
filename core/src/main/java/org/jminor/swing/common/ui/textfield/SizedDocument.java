/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A Document implementation which allows for setting the max text length
 * and automatic conversion to upper case or lower case.
 */
public class SizedDocument extends PlainDocument {

  /**
   * Instantiates a new SizedDocument
   */
  public SizedDocument() {
    setDocumentFilter(new SizedDocumentFilter());
  }

  /**
   * @param upperCase true if this text field should automatically convert text to uppercase
   */
  public final void setUpperCase(final boolean value) {
    ((SizedDocumentFilter) getDocumentFilter()).setUpperCase(value);
  }

  /**
   * @return true if this document converts all input to upper case
   */
  public final boolean isUpperCase() {
    return ((SizedDocumentFilter) getDocumentFilter()).isUpperCase();
  }

  /**
   * @param lowerCase true if this text field should automatically convert text to lowercase
   */
  public final void setLowerCase(final boolean value) {
    ((SizedDocumentFilter) getDocumentFilter()).setLowerCase(value);
  }

  /**
   * @return true if this document converts all input to lower case
   */
  public final boolean isLowerCase() {
    return ((SizedDocumentFilter) getDocumentFilter()).isLowerCase();
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public final int getMaxLength() {
    return ((SizedDocumentFilter) getDocumentFilter()).getMaxLength();
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public final void setMaxLength(final int value) {
    ((SizedDocumentFilter) getDocumentFilter()).setMaxLength(value);
  }

  /**
   * A DocumentFilter controlling both case and maximum length of the document content
   */
  protected static class SizedDocumentFilter extends DocumentFilter {

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

    @Override
    public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attributeSet) throws BadLocationException {
      final StringBuilder builder = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
      builder.insert(offset, string);
      if (getMaxLength() > 0 && builder.length() > getMaxLength()) {
        return;
      }
      final String valueAfterInsert = prepareString(builder.toString());
      if (validValue(valueAfterInsert)) {
        setText(fb, valueAfterInsert, attributeSet);
      }
    }

    @Override
    public void replace(final FilterBypass fb, final int offset, final int length, final String string, final AttributeSet attributeSet) throws BadLocationException {
      final Document document = fb.getDocument();
      final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
      builder.replace(offset, offset + length, string);
      if (getMaxLength() > 0 && builder.length() > getMaxLength()) {
        return;
      }
      final String valueAfterReplace = prepareString(builder.toString());
      if (validValue(valueAfterReplace)) {
        setText(fb, valueAfterReplace, attributeSet);
      }
    }

    private void setText(final FilterBypass fb, final String text, final AttributeSet attributeSet) throws BadLocationException {
      super.replace(fb, 0, fb.getDocument().getLength(), text, attributeSet);
    }

    protected boolean validValue(final String value) {
      return true;
    }

    /**
     * Prepares the string before it is added to the document, removing invalid characters and such.
     * @param string the string to prepare
     * @return the prepared string
     */
    protected String prepareString(final String string) {
      if (upperCase) {
        return string.toUpperCase(Locale.getDefault());
      }
      if (lowerCase) {
        return string.toLowerCase(Locale.getDefault());
      }

      return string;
    }
  }
}
