/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.ParseException;

/**
 * A class for linking a JFormattedTextField to a ValueChangeMapEditModel property value.
 */
public class FormattedValueLink<K> extends TextValueLink<K> {

  private final Format format;
  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key to link
   * @param format the format
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * @param linkType the link type
   */
  public FormattedValueLink(final JFormattedTextField textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                            final K key, final Format format, final boolean immediateUpdate,
                            final LinkType linkType) {
    super(textComponent, editModel, key, immediateUpdate, linkType);
    this.format = format;
    this.formatter = textComponent.getFormatter();
    updateUI();
  }

  /**
   * @return the format, if any
   */
  public final Format getFormat() {
    return format;
  }

  @Override
  protected final Object valueFromText(final String text) {
    if (text == null) {
      return null;
    }

    try {
      return format == null ? text : translate(format.parseObject(text));
    }
    catch (ParseException nf) {
      return null;
    }
  }

  @Override
  protected final String getValueAsString(final Object value) {
    if (value == null) {
      return null;
    }

    return format == null ? value.toString() : format.format(value);
  }

  @Override
  protected String translate(final String text) {
    try {
      return (String) formatter.stringToValue(text);
    }
    catch (ParseException e) {
      return null;
    }
  }

  protected Object translate(final Object parsedValue) {
    return parsedValue;
  }

  @Override
  protected void addValidator(final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel) {
    final Color defaultTextFieldBackground = textComponent.getBackground();
    final Color invalidBackgroundColor = Color.LIGHT_GRAY;
    final String defaultToolTip = textComponent.getToolTipText();
    final String maskString = textComponent.getText();
    textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void insertOrUpdate(final DocumentEvent e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
    });
    editModel.addValueListener(getKey(), new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateValidityInfo(textComponent, editModel, maskString, defaultTextFieldBackground, invalidBackgroundColor, defaultToolTip);
      }
    });
  }

  private void updateValidityInfo(final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                                  final String maskString, final Color validBackground, final Color invalidBackground,
                                  final String defaultToolTip) {
    final boolean validInput = !isModelValueNull() || (textComponent.getText().equals(maskString) && isNullable());
    final String validationMessage = getValidationMessage(editModel);
    textComponent.setBackground(validInput && validationMessage == null ? validBackground : invalidBackground);
    textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
            (defaultToolTip != null && !defaultToolTip.isEmpty() ? defaultToolTip + ": " : "") + validationMessage);
  }
}
