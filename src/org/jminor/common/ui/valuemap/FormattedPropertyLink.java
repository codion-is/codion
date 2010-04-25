/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.ParseException;

/**
 * A class for linking a JFormattedTextField to a ChangeValueMapEditModel property value.
 */
public class FormattedPropertyLink extends TextPropertyLink {

  private final Format format;
  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedTextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the ChangeValueMapEditModel instance
   * @param key the key to link
   * @param format the format
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * @param linkType the link type
   */
  public FormattedPropertyLink(final JFormattedTextField textComponent, final ChangeValueMapEditModel<String, Object> editModel,
                               final String key, final Format format, final boolean immediateUpdate,
                               final LinkType linkType) {
    super(textComponent, editModel, key, immediateUpdate, linkType);
    this.format = format;
    this.formatter = textComponent.getFormatter();
    updateUI();
  }

  /**
   * @return the format, if any
   */
  public Format getFormat() {
    return format;
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    if (text == null)
      return null;

    try {
      return format == null ? text : format.parseObject(text);
    }
    catch (ParseException nf) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected String getValueAsString(final Object value) {
    if (isNull(value))
      return null;

    return format == null ? value.toString() : format.format(value);
  }

  /** {@inheritDoc} */
  @Override
  protected String getText() {
    final String value = super.getText();
    if (value == null)
      return null;
    try {
      return (String) formatter.stringToValue((String) value);
    }
    catch (ParseException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void addValidator(final JTextComponent textComponent, final ChangeValueMapEditModel<String, Object> editModel) {
    final Color defaultTextFieldBackground = textComponent.getBackground();
    final Color invalidBackgroundColor = Color.LIGHT_GRAY;
    final String defaultToolTip = textComponent.getToolTipText();
    final String maskString = textComponent.getText();
    textComponent.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(final DocumentEvent e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
      public void removeUpdate(final DocumentEvent e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
      public void changedUpdate(final DocumentEvent e) {}
    });
    editModel.getPropertyChangeEvent(getKey()).addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
    });
  }

  private void updateValidityInfo(final JTextComponent textComponent, final ChangeValueMapEditModel<String, Object> editModel,
                                  final String maskString, final Color validBackground, final Color invalidBackground,
                                  final String defaultToolTip) {
    final boolean validInput = !isModelPropertyValueNull() || (textComponent.getText().equals(maskString) && isNullable());
    final String validationMessage = getValidationMessage(editModel);
    textComponent.setBackground(validInput && validationMessage == null ? validBackground : invalidBackground);
    textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
            (defaultToolTip != null && defaultToolTip.length() > 0 ? defaultToolTip + ": " : "") + validationMessage);
  }
}
