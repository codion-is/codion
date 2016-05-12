/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

public final class PropertyInputDialog extends Dialog<PropertyInputDialog.InputResult> {

  private final Control control;

  public PropertyInputDialog(final Property property, final Object defaultValue,
                             final EntityConnectionProvider connectionProvider) {
    setTitle(property.getCaption());
    this.control = FXUiUtil.createControl(property, connectionProvider);
    final Value value = FXUiUtil.createValue(property, control, defaultValue);
    if (control instanceof TextField) {
      ((TextField) control).selectAll();
    }
    initializeUI(control);
    setResultConverter(dialogButton -> new InputResult(dialogButton != null &&
            dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE, value.get()));
  }

  public Control getControl() {
    return control;
  }

  private void initializeUI(final Control control) {
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    getDialogPane().setContent(control);
  }

  public static final class InputResult {
    private final boolean inputAccepted;
    private final Object value;

    public InputResult(final boolean inputAccepted, final Object value) {
      this.inputAccepted = inputAccepted;
      this.value = value;
    }

    public boolean isInputAccepted() {
      return inputAccepted;
    }

    public Object getValue() {
      return value;
    }
  }
}
