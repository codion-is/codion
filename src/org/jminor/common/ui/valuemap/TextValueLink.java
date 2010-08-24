/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A class for linking a text component to a ValueChangeMapEditModel text property value.
 */
public class TextValueLink<K> extends AbstractValueMapLink<K, Object> {

  private final Document document;
  /**
   * If true the model value is updated on each keystroke, otherwise it is updated on focus lost and action performed
   */
  private final boolean immediateUpdate;

  /**
   * Instantiates a new TextValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   */
  public TextValueLink(final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                       final K key, final boolean immediateUpdate) {
    this(textComponent, editModel, key, immediateUpdate, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TextValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   */
  public TextValueLink(final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                       final K key, final boolean immediateUpdate, final LinkType linkType) {
    super(editModel, key, linkType);
    this.document = textComponent.getDocument();
    this.immediateUpdate = immediateUpdate;
    if (!this.immediateUpdate) {
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
    ValueLinkValidators.addValidator(this, textComponent, editModel);
    updateUI();
    this.document.addDocumentListener(new DocumentAdapter() {
      /** {@inheritDoc} */
      @Override
      public final void insertOrRemoveUpdate(final DocumentEvent e) {
        if (immediateUpdate) {
          updateModel();
        }
      }
    });
  }

  /**
   * @return true if the underlying property should be updated on each keystroke
   */
  public final boolean isImmediateUpdate() {
    return immediateUpdate;
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getUIValue() {
    return valueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected final void setUIValue(final Object value) {
    try {
      document.remove(0, document.getLength());
      if (value != null) {
        document.insertString(0, getValueAsString(value), null);
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
   * @return a value, null if the input text has zero length or if it does not yield a valid value
   */
  protected Object valueFromText(final String text) {
    if (text != null && text.isEmpty()) {
      return null;
    }

    return text;
  }

  /**
   * Returns a String representation of the given value object, null is returned in case of a null value
   * @param value the value to return as String
   * @return a String representation of the given value, null if the value is null
   */
  protected String getValueAsString(final Object value) {
    return value == null ? null : value.toString();
  }
}
