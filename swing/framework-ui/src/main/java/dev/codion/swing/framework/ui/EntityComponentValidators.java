/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.EntityEditModel;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A factory class for adding validators to components.
 */
public final class EntityComponentValidators {

  private EntityComponentValidators() {}

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param property the property of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   */
  public static void addValidator(final Property property, final JTextComponent textComponent, final EntityEditModel editModel) {
    addValidator(property, textComponent, editModel, Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param property the property of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   */
  public static void addFormattedValidator(final Property property, final JTextComponent textComponent,
                                           final EntityEditModel editModel) {
    addFormattedValidator(property, textComponent, editModel, Color.LIGHT_GRAY, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param property the property of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param invalidBackgroundColor the background color indicating in invalid value
   * @param defaultToolTip the tooltip to use while the value is valid
   */
  public static void addValidator(final Property property, final JTextComponent textComponent,
                                  final EntityEditModel editModel, final Color invalidBackgroundColor,
                                  final String defaultToolTip) {
    new TextValidator(property, textComponent, editModel, invalidBackgroundColor, defaultToolTip).validate();
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param property the property of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param invalidBackgroundColor the background color indicating in invalid value
   * @param defaultToolTip the tooltip to use while the value is valid
   */
  public static void addFormattedValidator(final Property property, final JTextComponent textComponent,
                                           final EntityEditModel editModel, final Color invalidBackgroundColor,
                                           final String defaultToolTip) {
    new FormattedTextValidator(property, textComponent, editModel, invalidBackgroundColor, defaultToolTip).validate();
  }

  private abstract static class AbstractValidator {

    private final Property property;
    private final JComponent component;
    private final EntityEditModel editModel;
    private final String defaultToolTip;

    private AbstractValidator(final Property property, final JComponent component, final EntityEditModel editModel,
                              final String defaultToolTip) {
      this.property = property;
      this.component = component;
      this.editModel = editModel;
      this.defaultToolTip = defaultToolTip;
      this.editModel.getValidator().addRevalidationListener(this::validate);
      this.editModel.addValueListener(property.getPropertyId(), valueChange -> validate());
    }

    /**
     * If the current value is invalid this method returns a string describing the nature of
     * the invalidity, if the value is valid this method returns null
     * @return a validation string if the value is invalid, null otherwise
     */
    protected final String getValidationMessage() {
      try {
        editModel.validate(property);
        return null;
      }
      catch (final ValidationException e) {
        return e.getMessage();
      }
    }

    protected boolean isNullable() {
      return editModel.isNullable(property);
    }

    protected boolean isNull() {
      return editModel.isNull(property.getPropertyId());
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

    protected void setToolTipText(final String validationMessage) {
      if (validationMessage == null) {
        component.setToolTipText(defaultToolTip);
      }
      else if (nullOrEmpty(defaultToolTip)) {
        component.setToolTipText(validationMessage);
      }
      else {
        component.setToolTipText(validationMessage + ": " + defaultToolTip);
      }
    }

    /**
     * Updates the underlying component indicating the validity of the value being displayed
     */
    protected abstract void validate();
  }

  private static class TextValidator extends AbstractValidator {

    protected static final Color VALID_ENABLED_BACKGROUND_COLOR;
    protected static final Color VALID_DISABLED_BACKGROUND_COLOR;

    static {
      final JTextField textField = new JTextField();
      VALID_ENABLED_BACKGROUND_COLOR = textField.getBackground();
      textField.setEnabled(false);
      VALID_DISABLED_BACKGROUND_COLOR = textField.getBackground();
    }

    private final Color invalidBackgroundColor;

    /**
     * Instantiates a new TextValidator
     * @param property the property of the value to validate
     * @param textComponent the text component bound to the value
     * @param editModel the edit model handling the value editing
     * @param invalidBackgroundColor the background color to use when the field value is invalid
     * @param defaultToolTip the default tooltip to show when the field value is valid
     */
    protected TextValidator(final Property property, final JTextComponent textComponent, final EntityEditModel editModel,
                            final Color invalidBackgroundColor, final String defaultToolTip) {
      super(property, textComponent, editModel, defaultToolTip);
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
      final JComponent component = getComponent();
      final boolean enabled = component.isEnabled();
      final String validationMessage = getValidationMessage();
      component.setBackground(validationMessage == null ?
              (enabled ? VALID_ENABLED_BACKGROUND_COLOR : VALID_DISABLED_BACKGROUND_COLOR) : invalidBackgroundColor);
      setToolTipText(validationMessage);
    }
  }

  private static final class FormattedTextValidator extends TextValidator {

    private final String maskString;

    private FormattedTextValidator(final Property property, final JTextComponent textComponent, final EntityEditModel editModel,
                                   final Color invalidBackgroundColor, final String defaultToolTip) {
      super(property, textComponent, editModel, invalidBackgroundColor, defaultToolTip);
      this.maskString = textComponent.getText();
    }

    @Override
    protected void validate() {
      final JTextComponent textComponent = (JTextComponent) getComponent();
      final boolean enabled = textComponent.isEnabled();
      final boolean stringEqualsMask = textComponent.getText().equals(maskString);
      final boolean validInputString = !isNull() || (stringEqualsMask && isNullable());
      final String validationMessage = getValidationMessage();
      if (validInputString && validationMessage == null) {
        textComponent.setBackground(enabled ? VALID_ENABLED_BACKGROUND_COLOR : VALID_DISABLED_BACKGROUND_COLOR);
      }
      else {
        textComponent.setBackground(getInvalidBackgroundColor());
      }
      setToolTipText(validationMessage);
    }
  }
}
