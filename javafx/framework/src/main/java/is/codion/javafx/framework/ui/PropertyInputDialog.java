/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.property.Property;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

/**
 * A {@link Dialog} implementation for receiving property values as input from user
 * @param <T> the input value type
 */
public final class PropertyInputDialog<T> extends Dialog<PropertyInputDialog.InputResult<T>> {

  private final Control control;

  /**
   * @param property the property
   * @param defaultValue the default value to present to the user
   * @param connectionProvider the connection provider
   */
  public PropertyInputDialog(Property<T> property, T defaultValue,
                             EntityConnectionProvider connectionProvider) {
    setTitle(property.getCaption());
    this.control = FXUiUtil.createControl(property, connectionProvider);
    Value<T> value = FXUiUtil.createValue(property, control, defaultValue);
    if (control instanceof TextField) {
      ((TextField) control).selectAll();
    }
    initializeUI(control);
    setResultConverter(dialogButton -> new InputResult<>(dialogButton != null &&
            dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE, value.get()));
  }

  /**
   * @return the input control used by this input dialog
   */
  public Control getControl() {
    return control;
  }

  private void initializeUI(Control control) {
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    getDialogPane().setContent(control);
  }

  /**
   * The result from a InputDialog
   * @param <T> the value type
   */
  public static final class InputResult<T> {

    private final boolean inputAccepted;
    private final T value;

    /**
     * @param inputAccepted true if the user accepted the input value
     * @param value the input value
     */
    public InputResult(boolean inputAccepted, T value) {
      this.inputAccepted = inputAccepted;
      this.value = value;
    }

    /**
     * @return true if the user accepted the input value
     */
    public boolean isInputAccepted() {
      return inputAccepted;
    }

    /**
     * @return the value
     */
    public T getValue() {
      return value;
    }
  }
}
