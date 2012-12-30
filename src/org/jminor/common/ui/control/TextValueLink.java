/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Format;

/**
 * Binds a JTextComponent to a text based property.
 */
class TextValueLink extends AbstractValueLink<Object> {

  private final Document document;

  /**
   * The format to use when presenting values in the linked text field
   */
  private final Format format;

  /**
   * Instantiates a new TextValueLink.
   * @param textComponent the text component to link with the value
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  TextValueLink(final JTextComponent textComponent, final Value modelValue, final LinkType linkType,
                final Format format, final boolean immediateUpdate) {
    super(modelValue, linkType, null);
    this.document = textComponent.getDocument();
    this.format = format == null ? new Util.NullFormat() : format;
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEditable(false);
    }
    updateUI();
    if (immediateUpdate) {
      this.document.addDocumentListener(new DocumentAdapter() {
        /** {@inheritDoc} */
        @Override
        public final void contentsChanged(final DocumentEvent e) {
          updateModel();
        }
      });
    }
    else {
      textComponent.addFocusListener(new FocusAdapter() {
        /** {@inheritDoc} */
        @Override
        public void focusLost(final FocusEvent e) {
          updateModel();
        }
      });
    }
  }

  /**
   * @return the value from the UI component
   */
  @Override
  protected final Object getUIValue() {
    return getValueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected final void setUIValue(final Object value) {
    try {
      synchronized (document) {
        document.remove(0, document.getLength());
        if (value != null) {
          document.insertString(0, getValueAsText(value), null);
        }
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the text from the linked text component
   */
  protected final String getText() {
    try {
      return translate(document.getText(0, document.getLength()));
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a String representation of the given value object, using the format,
   * an empty string is returned in case of a null value
   * @param value the value to return as String
   * @return a formatted String representation of the given value, an empty string if the value is null
   */
  protected final String getValueAsText(final Object value) {
    return value == null ? "" : format.format(value);
  }

  /**
   * @return the format object being used by this value link
   */
  protected final Format getFormat() {
    return format;
  }

  /**
   * Provides a hook into the value setting mechanism.
   * @param text the value returned from the UI component
   * @return the translated value
   */
  protected String translate(final String text) {
    return text;
  }

  /**
   * Returns a property value based on the given text, if the text can not
   * be parsed into a valid value, null is returned
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  protected Object getValueFromText(final String text) {
    if (text != null && text.isEmpty()) {
      return null;
    }

    return text;
  }
}
