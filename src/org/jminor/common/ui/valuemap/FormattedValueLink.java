/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import javax.swing.text.JTextComponent;
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
    new FormattedValidator<K>(this, textComponent, editModel).updateValidityInfo();
    updateUI();
  }

  /**
   * @return the format, if any
   */
  public final Format getFormat() {
    return format;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  protected final String getValueAsString(final Object value) {
    if (value == null) {
      return null;
    }

    return format == null ? value.toString() : format.format(value);
  }

  /** {@inheritDoc} */
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

  private static final class FormattedValidator<K> extends ValidatorImpl<K> {

    private final String maskString;

    private FormattedValidator(final TextValueLink<K> textValueLink, final JTextComponent textComponent,
                               final ValueChangeMapEditModel<K, Object> editModel) {
      super(textValueLink, textComponent, editModel);
      this.maskString = textComponent.getText();
    }

    /** {@inheritDoc} */
    @Override
    public void updateValidityInfo() {
      final boolean validInput = !getLink().isModelValueNull() || (getTextComponent().getText().equals(maskString) && getLink().isNullable());
      final String validationMessage = getLink().getValidationMessage(getEditModel());
      getTextComponent().setBackground(validInput && validationMessage == null ? getValidBackgroundColor() : getInvalidBackgroundColor());
      getTextComponent().setToolTipText(validationMessage == null ? getDefaultToolTip() :
              (!Util.nullOrEmpty(getDefaultToolTip()) ? getDefaultToolTip() + ": " : "") + validationMessage);
    }
  }
}
