/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.text.Format;
import java.text.ParseException;

public class FormattedPropertyLink extends TextPropertyLink {

  private final Format format;
  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedTextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param format the format
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * @param linkType the link type
   */
  public FormattedPropertyLink(final JFormattedTextField textComponent, final EntityEditModel editModel,
                               final Property property, final Format format, final boolean immediateUpdate,
                               final LinkType linkType) {
    super(textComponent, editModel, property, immediateUpdate, linkType);
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
    if (Entity.isValueNull(getProperty().getPropertyType(), value))
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
  protected void addValidator(final JTextComponent textComponent, final EntityEditModel editModel) {
    final Color defaultTextFieldBackground = textComponent.getBackground();
    final Color invalidBackgroundColor = (Color) Configuration.getValue(Configuration.INVALID_VALUE_BACKGROUND_COLOR);
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
    editModel.getPropertyChangeEvent(getProperty()).addListener(new Property.Listener() {
      @Override
      protected void propertyChanged(final Property.Event e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
    });
  }

  private void updateValidityInfo(final JTextComponent textComponent, final EntityEditModel editModel,
                                  final String maskString, final Color validBackground, final Color invalidBackground,
                                  final String defaultToolTip) {
    final boolean validInput = !isModelPropertyValueNull() || (textComponent.getText().equals(maskString) && getProperty().isNullable());
    final String validationMessage = getValidationMessage(editModel);
    textComponent.setBackground(validInput && validationMessage == null ? validBackground : invalidBackground);
    textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
            (defaultToolTip != null && defaultToolTip.length() > 0 ? defaultToolTip + ": " : "") + validationMessage);
  }
}
