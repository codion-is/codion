/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.ParseException;
import java.util.Objects;

public final class EntityFXUtil {

  public static TextField createTextField(final Property property, final EntityEditModel editModel) {
    return createTextField(property, editModel, null);
  }

  public static TextField createTextField(final Property property, final EntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<String> propertyValue = PropertyValues.stringPropertyValue(textField.textProperty());
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static TextField createIntegerField(final Property property, final EntityEditModel editModel) {
    return createIntegerField(property, editModel, null);
  }

  public static TextField createIntegerField(final Property property, final EntityEditModel editModel,
                                             final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static TextField createDoubleField(final Property property, final EntityEditModel editModel) {
    return createDoubleField(property, editModel, null);
  }

  public static TextField createDoubleField(final Property property, final EntityEditModel editModel,
                                            final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static void linkToEnabledState(final Node node, final StateObserver enabledState) {
    Objects.requireNonNull(node);
    Objects.requireNonNull(enabledState);
    node.setDisable(!enabledState.isActive());
    enabledState.addInfoListener(modified -> node.setDisable(!modified));
  }

  private static TextField createTextField(final Property property, final StateObserver enabledState) {
    final TextField textField = new TextField();
    textField.textProperty().addListener(new ValidationChangeListener(property, textField.textProperty()));
    if (enabledState != null) {
      linkToEnabledState(textField, enabledState);
    }

    return textField;
  }

  private static final class ValidationChangeListener implements ChangeListener<String> {

    private final Property property;
    private final StringProperty stringProperty;

    private boolean ignoreChange = false;

    private ValidationChangeListener(final Property property, final StringProperty stringProperty) {
      this.property = property;
      this.stringProperty = stringProperty;
    }

    @Override
    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
      if (ignoreChange) {
        return;
      }
      if (!isValid(property, newValue)) {
        try {
          ignoreChange = true;
          stringProperty.setValue(oldValue);
        }
        finally {
          ignoreChange = false;
        }
      }
    }
  }

  private static boolean isValid(final Property property, final String value) {
    final int maxLengt = property.getMaxLength();
    if (maxLengt > -1 && value != null && value.length() > maxLengt) {
      return false;
    }
    try {
      if (property.getFormat() != null && value != null) {
        property.getFormat().parseObject(value);
      }
    }
    catch (final NumberFormatException | ParseException e) {
      return false;
    }


    return true;
  }
}
