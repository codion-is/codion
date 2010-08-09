/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A class for linking a text component to a ValueChangeMapEditModel text property value.
 */
public class TextValueLink<K> extends AbstractValueMapLink<K, Object> implements DocumentListener {

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
        @Override
        public void focusLost(final FocusEvent e) {
          updateModel();
        }
      });
    }
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEnabled(false);
    }
    new ValidatorImpl<K>(this, textComponent, editModel).updateValidityInfo();
    updateUI();
    this.document.addDocumentListener(this);
  }

  /**
   * @return true if the underlying property should be updated on each keystroke
   */
  public final boolean isImmediateUpdate() {
    return immediateUpdate;
  }

  /** {@inheritDoc} */
  public final void insertUpdate(final DocumentEvent e) {
    if (immediateUpdate) {
      updateModel();
    }
  }

  /** {@inheritDoc} */
  public final void removeUpdate(final DocumentEvent e) {
    if (immediateUpdate) {
      updateModel();
    }
  }

  /** {@inheritDoc} */
  public final void changedUpdate(final DocumentEvent e) {}

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

  public interface Validator {
    void updateValidityInfo();
  }

  protected static class ValidatorImpl<K> implements Validator {

    private final TextValueLink<K> link;
    private final JTextComponent textComponent;
    private final ValueChangeMapEditModel<K, Object> editModel;
    private final Color validBackgroundColor;
    private final Color invalidBackgroundColor;
    private final String defaultToolTip;

    protected ValidatorImpl(final TextValueLink<K> link, final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel) {
      this(link, textComponent, editModel, textComponent.getBackground(), Color.LIGHT_GRAY, textComponent.getToolTipText());
    }

    protected ValidatorImpl(final TextValueLink<K> link, final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                            final Color validBackgroundColor, final Color invalidBackgroundColor, final String defaultToolTip) {
      this.link = link;
      this.defaultToolTip = defaultToolTip;
      this.editModel = editModel;
      this.invalidBackgroundColor = invalidBackgroundColor;
      this.textComponent = textComponent;
      this.validBackgroundColor = validBackgroundColor;
      editModel.addValueListener(link.getKey(), new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          updateValidityInfo();
        }
      });
      textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        public void insertOrUpdate(final DocumentEvent e) {
          updateValidityInfo();
        }
      });
    }

    public final JTextComponent getTextComponent() {
      return textComponent;
    }

    public final TextValueLink<K> getLink() {
      return link;
    }

    public final ValueChangeMapEditModel<K, Object> getEditModel() {
      return editModel;
    }

    public final String getDefaultToolTip() {
      return defaultToolTip;
    }

    public final Color getInvalidBackgroundColor() {
      return invalidBackgroundColor;
    }

    public final Color getValidBackgroundColor() {
      return validBackgroundColor;
    }

    public void updateValidityInfo() {
      final String validationMessage = link.getValidationMessage(editModel);
      textComponent.setBackground(validationMessage == null ? validBackgroundColor : invalidBackgroundColor);
      textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
              (!Util.nullOrEmpty(defaultToolTip) ? defaultToolTip + ": " : "") + validationMessage);
    }
  }
}
