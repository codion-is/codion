/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.valuemap;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;

/**
 * A factory class for adding validators to components.
 */
public final class ValueLinkValidators {

  private ValueLinkValidators() {}

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param key the key of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addValidator(final K key, final JTextComponent textComponent, final ValueMapEditModel<K, ?> editModel) {
    addValidator(key, textComponent, editModel, Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param key the key of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addFormattedValidator(final K key, final JTextComponent textComponent,
                                               final ValueMapEditModel<K, Object> editModel) {
    addFormattedValidator(key, textComponent, editModel, Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param key the key of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param invalidBackgroundColor the background color indicating in invalid value
   * @param defaultToolTip the tooltip to use while the value is valid
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addValidator(final K key, final JTextComponent textComponent,
                                      final ValueMapEditModel<K, ?> editModel, final Color invalidBackgroundColor,
                                      final String defaultToolTip) {
    new TextValidator<>(key, textComponent, editModel, invalidBackgroundColor, defaultToolTip).validate();
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param key the key of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param invalidBackgroundColor the background color indicating in invalid value
   * @param defaultToolTip the tooltip to use while the value is valid
   * @param <K> the type of the edit model value keys
   */
  public static <K> void addFormattedValidator(final K key, final JTextComponent textComponent,
                                               final ValueMapEditModel<K, ?> editModel, final Color invalidBackgroundColor,
                                               final String defaultToolTip) {
    new FormattedTextValidator<>(key, textComponent, editModel, invalidBackgroundColor, defaultToolTip).validate();
  }

  private abstract static class AbstractValidator<K> {

    private final K key;
    private final JComponent component;
    private final ValueMapEditModel<K, ?> editModel;
    private final String defaultToolTip;

    private AbstractValidator(final K key, final JComponent component, final ValueMapEditModel<K, ?> editModel,
                              final String defaultToolTip) {
      this.key = key;
      this.component = component;
      this.editModel = editModel;
      this.defaultToolTip = defaultToolTip;
      this.editModel.getValidator().addRevalidationListener(new EventListener() {
        @Override
        public void eventOccurred() {
          validate();
        }
      });
      this.editModel.addValueListener(key, new EventInfoListener<ValueChange<K, ?>>() {
        @Override
        public void eventOccurred(final ValueChange info) {
          validate();
        }
      });
    }

    /**
     * If the current value is invalid this method returns a string describing the nature of
     * the invalidity, if the value is valid this method returns null
     * @return a validation string if the value is invalid, null otherwise
     */
    protected final String getValidationMessage() {
      try {
        editModel.validate(key);
        return null;
      }
      catch (final ValidationException e) {
        return e.getMessage();
      }
    }

    protected boolean isNullable() {
      return editModel.isNullable(key);
    }

    protected boolean isModelValueNull() {
      return editModel.isValueNull(key);
    }

    /**
     * @return the component associated with the value being validated
     */
    protected final JComponent getComponent() {
      return component;
    }

    /**
     * @return the default tooltip to show when the field value is valid
     */
    protected final String getDefaultToolTip() {
      return defaultToolTip;
    }

    /**
     * Updates the underlying component indicating the validity of the value being displayed
     */
    protected abstract void validate();
  }

  private static class TextValidator<K> extends AbstractValidator<K> {

    protected static final Color VALID_ENABLED_BACKGROUND_COLOR;
    protected static final Color VALID_DISABLED_BACKGROUND_COLOR;

    static {
      final JTextField txtField = new JTextField();
      VALID_ENABLED_BACKGROUND_COLOR = txtField.getBackground();
      txtField.setEnabled(false);
      VALID_DISABLED_BACKGROUND_COLOR = txtField.getBackground();
    }

    private final Color invalidBackgroundColor;

    /**
     * Instantiates a new TextValidator
     * @param key the key of the value to validate
     * @param textComponent the text component bound to the value
     * @param editModel the edit model handling the value editing
     * @param invalidBackgroundColor the background color to use when the field value is invalid
     * @param defaultToolTip the default tooltip to show when the field value is valid
     */
    protected TextValidator(final K key, final JTextComponent textComponent, final ValueMapEditModel<K, ?> editModel,
                            final Color invalidBackgroundColor, final String defaultToolTip) {
      super(key, textComponent, editModel, defaultToolTip);
      if (invalidBackgroundColor.equals(VALID_ENABLED_BACKGROUND_COLOR)) {
        throw new IllegalArgumentException("Invalid background color is the same as the valid background color");
      }
      this.invalidBackgroundColor = invalidBackgroundColor;
    }

    /**
     * @return the background color to use when the field value is invalid
     */
    protected final Color getInvalidBackgroundColor() {
      return invalidBackgroundColor;
    }

    @Override
    protected void validate() {
      final boolean enabled = getComponent().isEnabled();
      final String validationMessage = getValidationMessage();
      getComponent().setBackground(validationMessage == null ?
              (enabled ? VALID_ENABLED_BACKGROUND_COLOR : VALID_DISABLED_BACKGROUND_COLOR) : invalidBackgroundColor);
      getComponent().setToolTipText(validationMessage == null ? getDefaultToolTip() :
              (!Util.nullOrEmpty(getDefaultToolTip()) ? getDefaultToolTip() + ": " : "") + validationMessage);
    }
  }

  private static final class FormattedTextValidator<K> extends TextValidator<K> {

    private final String maskString;

    private FormattedTextValidator(final K key, final JTextComponent textComponent, final ValueMapEditModel<K, ?> editModel,
                                   final Color invalidBackgroundColor, final String defaultToolTip) {
      super(key, textComponent, editModel, invalidBackgroundColor, defaultToolTip);
      this.maskString = textComponent.getText();
    }

    @Override
    protected void validate() {
      final JTextComponent textComponent = (JTextComponent) getComponent();
      final boolean enabled = textComponent.isEnabled();
      final boolean stringEqualsMask = textComponent.getText().equals(maskString);
      final boolean validInput = !isModelValueNull() || (stringEqualsMask && isNullable());
      final String validationMessage = getValidationMessage();
      if (validInput && validationMessage == null) {
        textComponent.setBackground(enabled ? VALID_ENABLED_BACKGROUND_COLOR : VALID_DISABLED_BACKGROUND_COLOR);
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
