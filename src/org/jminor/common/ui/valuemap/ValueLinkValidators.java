/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A factory class for adding validators to components.
 */
public final class ValueLinkValidators {

  private ValueLinkValidators() {}

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param valueLink the value link
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addValidator(final TextValueLink<K> valueLink, final JTextComponent textComponent,
                                      final ValueChangeMapEditModel<K, Object> editModel) {
    addValidator(valueLink, textComponent, editModel, textComponent.getBackground(), Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param valueLink the value link
   * @param textComponent the text component
   * @param editModel the edit model
   * @param validBackgroundColor the background color indicating a valid value
   * @param invalidBackgroundColor the background color indicating in invalid value
   * @param defaultToolTip the tooltip to use while the value is valid
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addValidator(final TextValueLink<K> valueLink, final JTextComponent textComponent,
                                      final ValueChangeMapEditModel<K, Object> editModel,
                                      final Color validBackgroundColor, final Color invalidBackgroundColor, final String defaultToolTip) {
    if (valueLink instanceof FormattedValueLink) {
      new FormattedTextValidator<K>((FormattedValueLink<K>) valueLink, textComponent, editModel).updateValidityInfo();
    }
    else {
      new TextValidator<K>(valueLink, textComponent, editModel, validBackgroundColor, invalidBackgroundColor, defaultToolTip).updateValidityInfo();
    }
  }

  private abstract static class AbstractValidator<K> {

    private final AbstractValueMapLink<K, Object> link;
    private final JComponent component;
    private final ValueChangeMapEditModel<K, Object> editModel;
    private final String defaultToolTip;

    private AbstractValidator(final AbstractValueMapLink<K, Object> link, final JComponent component,
                              final ValueChangeMapEditModel<K, Object> editModel,
                              final String defaultToolTip) {
      this.link = link;
      this.component = component;
      this.editModel = editModel;
      this.defaultToolTip = defaultToolTip;
      editModel.addValueListener(link.getKey(), new ActionListener() {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          updateValidityInfo();
        }
      });
    }

    /**
     * @return the component associated with the value being validated
     */
    protected final JComponent getComponent() {
      return component;
    }

    /**
     * @return the underlying value link
     */
    protected final AbstractValueMapLink<K, Object> getValueLink() {
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
     * Updates the underlying component indicating the validity of the value being shown
     */
    protected abstract void updateValidityInfo();
  }

  private static class TextValidator<K> extends AbstractValidator<K> {

    private final Color validBackgroundColor;
    private final Color invalidBackgroundColor;

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
      super(link, textComponent, editModel, defaultToolTip);
      if (invalidBackgroundColor.equals(validBackgroundColor)) {
        throw new IllegalArgumentException("Invalid background color is the same as the current text component background");
      }
      this.validBackgroundColor = validBackgroundColor;
      this.invalidBackgroundColor = invalidBackgroundColor;
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
    @Override
    protected void updateValidityInfo() {
      final String validationMessage = getValueLink().getValidationMessage(getEditModel());
      getComponent().setBackground(validationMessage == null ? validBackgroundColor : invalidBackgroundColor);
      getComponent().setToolTipText(validationMessage == null ? getDefaultToolTip() :
              (!Util.nullOrEmpty(getDefaultToolTip()) ? getDefaultToolTip() + ": " : "") + validationMessage);
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
    protected void updateValidityInfo() {
      final JTextComponent textComponent = (JTextComponent) getComponent();
      final TextValueLink<K> valueLink = (TextValueLink<K>) getValueLink();
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
      final String tooltip;
      if (validationMessage == null) {
        tooltip = defaultToolTip;
      }
      else {
        if (Util.nullOrEmpty(defaultToolTip)) {
          tooltip = validationMessage;
        }
        else {
          tooltip = validationMessage + ": "  + defaultToolTip;
        }
      }

      textComponent.setToolTipText(tooltip);
    }
  }
}
