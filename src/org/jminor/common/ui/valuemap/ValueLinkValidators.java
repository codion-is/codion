/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;

import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A factory class for adding validators to components.
 */
final class ValueLinkValidators {

  private ValueLinkValidators() {}

  static <K> void addValidator(final TextValueLink<K> valueLink, final JTextComponent textComponent,
                               final ValueChangeMapEditModel<K, Object> editModel) {
    addValidator(valueLink, textComponent, editModel, textComponent.getBackground(), Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  static <K> void addValidator(final TextValueLink<K> valueLink, final JTextComponent textComponent,
                               final ValueChangeMapEditModel<K, Object> editModel,
                               final Color validBackgroundColor, final Color invalidBackgroundColor, final String defaultToolTip) {
    if (valueLink instanceof FormattedValueLink) {
      new FormattedTextValidator<K>((FormattedValueLink<K>) valueLink, textComponent, editModel).updateValidityInfo();
    }
    else {
      new TextValidator<K>(valueLink, textComponent, editModel, validBackgroundColor, invalidBackgroundColor, defaultToolTip).updateValidityInfo();
    }
  }

  private static class TextValidator<K> {

    private final TextValueLink<K> link;
    private final JTextComponent textComponent;
    private final ValueChangeMapEditModel<K, Object> editModel;
    private final Color validBackgroundColor;
    private final Color invalidBackgroundColor;
    private final String defaultToolTip;

    /**
     * Instantiates a new ValidatorImpl
     * @param link the value link, which value to validate
     * @param textComponent the text component bound to the value
     * @param editModel the edit model handling the value editing
     */
    protected TextValidator(final TextValueLink<K> link, final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel) {
      this(link, textComponent, editModel, textComponent.getBackground(), Color.LIGHT_GRAY, textComponent.getToolTipText());
    }

    /**
     * Instantiates a new ValidatorImpl
     * @param link the value link, which value to validate
     * @param textComponent the text component bound to the value
     * @param editModel the edit model handling the value editing
     * @param validBackgroundColor the background color to use when the field value is valid
     * @param invalidBackgroundColor the background color to use when the field value is invalid
     * @param defaultToolTip the default tooltip to show when the field value is valid
     */
    protected TextValidator(final TextValueLink<K> link, final JTextComponent textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                            final Color validBackgroundColor, final Color invalidBackgroundColor, final String defaultToolTip) {
      this.link = link;
      this.defaultToolTip = defaultToolTip;
      this.editModel = editModel;
      this.invalidBackgroundColor = invalidBackgroundColor;
      this.textComponent = textComponent;
      this.validBackgroundColor = validBackgroundColor;
      if (invalidBackgroundColor.equals(validBackgroundColor)) {
        throw new IllegalArgumentException("Invalid background color is the same as the current text component background");
      }
      editModel.addValueListener(link.getKey(), new ActionListener() {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          updateValidityInfo();
        }
      });
    }

    /**
     * @return the text component associated with the value being validated
     */
    protected final JTextComponent getTextComponent() {
      return textComponent;
    }

    /**
     * @return the underlying value link
     */
    protected final TextValueLink<K> getValueLink() {
      return link;
    }

    /**
     * @return the underlying edit model
     */
    protected final ValueChangeMapEditModel<K, Object> getEditModel() {
      return editModel;
    }

    /**
     * @return the default tooltip to show when the field value is valid
     */
    protected final String getDefaultToolTip() {
      return defaultToolTip;
    }

    /**
     * @return the background color to use when the field value is invalid
     */
    protected final Color getInvalidBackgroundColor() {
      return invalidBackgroundColor;
    }

    /**
     * @return the background color to use when the field value is valid
     */
    protected final Color getValidBackgroundColor() {
      return validBackgroundColor;
    }

    /** {@inheritDoc} */
    void updateValidityInfo() {
      final String validationMessage = link.getValidationMessage(editModel);
      textComponent.setBackground(validationMessage == null ? validBackgroundColor : invalidBackgroundColor);
      textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
              (!Util.nullOrEmpty(defaultToolTip) ? defaultToolTip + ": " : "") + validationMessage);
    }
  }

  private static final class FormattedTextValidator<K> extends TextValidator<K> {

    private final String maskString;

    private FormattedTextValidator(final FormattedValueLink<K> textValueLink, final JTextComponent textComponent,
                               final ValueChangeMapEditModel<K, Object> editModel) {
      super(textValueLink, textComponent, editModel);
      this.maskString = textComponent.getText();
    }

    /** {@inheritDoc} */
    @Override
    void updateValidityInfo() {
      final JTextComponent textComponent = getTextComponent();
      final TextValueLink<K> valueLink = getValueLink();
      final boolean stringEqualsMask = textComponent.getText().equals(maskString);
      final boolean validInput = !valueLink.isModelValueNull() || (stringEqualsMask && valueLink.isNullable());
      final String validationMessage = valueLink.getValidationMessage(getEditModel());
      if (validInput && validationMessage == null) {
        textComponent.setBackground(getValidBackgroundColor());
      }
      else {
        textComponent.setBackground(getInvalidBackgroundColor());
      }
      if (validationMessage != null) {
        textComponent.setToolTipText(validationMessage);
      }

      final String defaultToolTip = getDefaultToolTip();
      String tooltip;
      if (validationMessage == null) {
        tooltip = defaultToolTip;
      }
      else {
        if (Util.nullOrEmpty(defaultToolTip)) {
          tooltip = validationMessage;
        }
        else {
          tooltip = defaultToolTip + ": "  + validationMessage;
        }
      }

      textComponent.setToolTipText(tooltip);
    }
  }
}
