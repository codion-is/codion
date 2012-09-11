/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Format;

/**
 * A class for linking a text component to a ValueMapEditModel text property value.
 */
public class TextValueLink<K> extends AbstractValueMapLink<K, Object> {

  private final Document document;

  /**
   * The format to use when presenting values in the linked text field
   */
  private final Format format;

  /**
   * Instantiates a new TextValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueMapEditModel instance
   * @param key the key to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  public TextValueLink(final JTextComponent textComponent, final ValueMapEditModel<K, Object> editModel,
                       final K key, final boolean immediateUpdate) {
    this(textComponent, editModel, key, immediateUpdate, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TextValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueMapEditModel instance
   * @param key the key to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   */
  public TextValueLink(final JTextComponent textComponent, final ValueMapEditModel<K, Object> editModel,
                       final K key, final boolean immediateUpdate, final LinkType linkType) {
    this(textComponent, editModel, key, immediateUpdate, linkType, null);
  }

  /**
   * Instantiates a new TextValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueMapEditModel instance
   * @param key the key to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   */
  public TextValueLink(final JTextComponent textComponent, final ValueMapEditModel<K, Object> editModel,
                       final K key, final boolean immediateUpdate, final LinkType linkType, final Format format) {
    super(editModel, key, linkType);
    this.document = textComponent.getDocument();
    this.format = format == null ? new Util.NullFormat() : format;
    if (!immediateUpdate) {
      textComponent.addFocusListener(new FocusAdapter() {
        /** {@inheritDoc} */
        @Override
        public void focusLost(final FocusEvent e) {
          updateModel();
        }
      });
    }
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEditable(false);
    }
    updateUI();
    this.document.addDocumentListener(new DocumentAdapter() {
      /** {@inheritDoc} */
      @Override
      public final void contentsChanged(final DocumentEvent e) {
        if (immediateUpdate) {
          updateModel();
        }
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getUIValue() {
    return getValueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected final void setUIValue(final Object value) {
    try {
      document.remove(0, document.getLength());
      if (value != null) {
        document.insertString(0, getValueAsText(value), null);
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
