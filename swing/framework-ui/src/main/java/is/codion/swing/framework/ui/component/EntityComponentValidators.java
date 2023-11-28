/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.Colors.darker;

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
  public static <T> void addValidator(Attribute<T> attribute, JTextComponent textComponent, EntityEditModel editModel) {
    addValidator(attribute, textComponent, editModel, textComponent.getToolTipText());
  }

  /**
   * Adds a validator to the given text component, based on the given value link and edit model
   * @param attribute the attribute of the value to validate
   * @param textComponent the text component
   * @param editModel the edit model
   * @param <T> the value type
   */
  public static <T> void addFormattedValidator(Attribute<T> attribute, JTextComponent textComponent,
                                               EntityEditModel editModel) {
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
  public static <T> void addValidator(Attribute<T> attribute, JTextComponent textComponent,
                                      EntityEditModel editModel, String defaultToolTip) {
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
  public static <T> void addFormattedValidator(Attribute<T> attribute, JTextComponent textComponent,
                                               EntityEditModel editModel, String defaultToolTip) {
    new FormattedTextValidator<>(attribute, textComponent, editModel, defaultToolTip).validate();
  }

  private static class DefaultValidator<T> {

    private final Attribute<T> attribute;
    private final JComponent component;
    private final EntityEditModel editModel;
    private final String defaultToolTip;

    private DefaultValidator(Attribute<T> attribute, JComponent component, EntityEditModel editModel,
                             String defaultToolTip) {
      this.attribute = attribute;
      this.component = component;
      this.editModel = editModel;
      this.defaultToolTip = defaultToolTip;
    }

    /**
     * If the current value is invalid this method returns a string describing the nature of
     * the invalidity, if the value is valid this method returns null
     * @return a validation string if the value is invalid, null otherwise
     */
    protected final String validationMessage() {
      try {
        editModel.validate(attribute);
        return null;
      }
      catch (ValidationException e) {
        return e.getMessage();
      }
    }

    protected boolean nullable() {
      return editModel.nullable(attribute);
    }

    protected boolean isNull() {
      return editModel.isNull(attribute).get();
    }

    /**
     * @return the component associated with the value being validated
     */
    protected final JComponent component() {
      return component;
    }

    protected void setToolTipText(String validationMessage) {
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
  }

  private static class TextValidator<T> extends DefaultValidator<T> {

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
    protected TextValidator(Attribute<T> attribute, JTextComponent textComponent, EntityEditModel editModel,
                            String defaultToolTip) {
      super(attribute, textComponent, editModel, defaultToolTip);
      editModel.value(attribute).addListener(this::validate);
      configureColors();
      textComponent.addPropertyChangeListener("UI", event -> configureColors());
    }

    /**
     * Updates the underlying component indicating the validity of the value being displayed
     */
    protected void validate() {
      JComponent component = component();
      boolean enabled = component.isEnabled();
      boolean stringValid = valid();
      String validationMessage = validationMessage();
      if (stringValid && validationMessage == null) {
        component.setBackground(enabled ? backgroundColor : inactiveBackgroundColor);
      }
      else {
        component.setBackground(invalidBackgroundColor);
      }
      setToolTipText(validationMessage);
    }

    protected boolean valid() {
      return !isNull() || nullable();
    }

    private void configureColors() {
      this.backgroundColor = UIManager.getColor("TextField.background");
      this.inactiveBackgroundColor = UIManager.getColor("TextField.inactiveBackground");
      this.invalidBackgroundColor = darker(backgroundColor);
      validate();
    }
  }

  private static final class FormattedTextValidator<T> extends TextValidator<T> {

    private final String maskString;

    private FormattedTextValidator(Attribute<T> attribute, JTextComponent textComponent,
                                   EntityEditModel editModel, String defaultToolTip) {
      super(attribute, textComponent, editModel, defaultToolTip);
      this.maskString = textComponent.getText();
    }

    @Override
    protected boolean valid() {
      boolean stringEqualsMask = ((JTextComponent) component()).getText().equals(maskString);

      return !isNull() || (stringEqualsMask && nullable());
    }
  }
}
