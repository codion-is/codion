/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.swing.common.ui.Utilities.darker;

/**
 * A factory class for adding validators to components.
 */
public final class EntityComponentValidators {

  private EntityComponentValidators() {}

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param attribute the attribute of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <T> the value type
   */
  public static <T> void addValidator(final Attribute<T> attribute, final JTextComponent textComponent, final EntityEditModel editModel) {
    addValidator(attribute, textComponent, editModel, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param attribute the attribute of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <T> the value type
   */
  public static <T> void addFormattedValidator(final Attribute<T> attribute, final JTextComponent textComponent,
                                               final EntityEditModel editModel) {
    addFormattedValidator(attribute, textComponent, editModel, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param attribute the attribute of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param defaultToolTip the tooltip to use while the value is valid
   * @param <T> the value type
   */
  public static <T> void addValidator(final Attribute<T> attribute, final JTextComponent textComponent,
                                      final EntityEditModel editModel, final String defaultToolTip) {
    new TextValidator<>(attribute, textComponent, editModel, defaultToolTip).validate();
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param attribute the attribute of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param defaultToolTip the tooltip to use while the value is valid
   * @param <T> the value type
   */
  public static <T> void addFormattedValidator(final Attribute<T> attribute, final JTextComponent textComponent,
                                               final EntityEditModel editModel, final String defaultToolTip) {
    new FormattedTextValidator<>(attribute, textComponent, editModel, defaultToolTip).validate();
  }

  private abstract static class AbstractValidator<T> {

    private final Attribute<T> attribute;
    private final JComponent component;
    private final EntityEditModel editModel;
    private final String defaultToolTip;

    private AbstractValidator(final Attribute<T> attribute, final JComponent component, final EntityEditModel editModel,
                              final String defaultToolTip) {
      this.attribute = attribute;
      this.component = component;
      this.editModel = editModel;
      this.defaultToolTip = defaultToolTip;
      this.editModel.addValueListener(attribute, valueChange -> validate());
    }

    /**
     * If the current value is invalid this method returns a string describing the nature of
     * the invalidity, if the value is valid this method returns null
     * @return a validation string if the value is invalid, null otherwise
     */
    protected final String getValidationMessage() {
      try {
        editModel.validate(attribute);
        return null;
      }
      catch (final ValidationException e) {
        return e.getMessage();
      }
    }

    protected boolean isNullable() {
      return editModel.isNullable(attribute);
    }

    protected boolean isNull() {
      return editModel.isNull(attribute);
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

  private static class TextValidator<T> extends AbstractValidator<T> {

    protected Color backgroundColor;
    protected Color inactiveBackgroundColor;
    protected Color invalidBackgroundColor;

    /**
     * Instantiates a new TextValidator
     * @param attribute the attribute of the value to validate
     * @param textComponent the text component bound to the value
     * @param editModel the edit model handling the value editing
     * @param defaultToolTip the default tooltip to show when the field value is valid
     */
    protected TextValidator(final Attribute<T> attribute, final JTextComponent textComponent, final EntityEditModel editModel,
                            final String defaultToolTip) {
      super(attribute, textComponent, editModel, defaultToolTip);
      configureColors();
      textComponent.addPropertyChangeListener("UI", event -> configureColors());
    }

    @Override
    protected void validate() {
      final JComponent component = getComponent();
      final boolean enabled = component.isEnabled();
      final String validationMessage = getValidationMessage();
      component.setBackground(validationMessage == null ?
              (enabled ? backgroundColor : inactiveBackgroundColor) : invalidBackgroundColor);
      setToolTipText(validationMessage);
    }

    private void configureColors() {
      final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
      this.backgroundColor = lookAndFeel.getDefaults().getColor("TextField.background");
      this.inactiveBackgroundColor = lookAndFeel.getDefaults().getColor("TextField.inactiveBackground");
      this.invalidBackgroundColor = darker(backgroundColor);
      validate();
    }
  }

  private static final class FormattedTextValidator<T> extends TextValidator<T> {

    private final String maskString;

    private FormattedTextValidator(final Attribute<T> attribute, final JTextComponent textComponent,
                                   final EntityEditModel editModel, final String defaultToolTip) {
      super(attribute, textComponent, editModel, defaultToolTip);
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
        textComponent.setBackground(enabled ? backgroundColor : inactiveBackgroundColor);
      }
      else {
        textComponent.setBackground(invalidBackgroundColor);
      }
      setToolTipText(validationMessage);
    }
  }
}
