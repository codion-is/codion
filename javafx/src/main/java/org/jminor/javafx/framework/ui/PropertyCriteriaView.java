/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ForeignKeyCriteriaModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;

public final class PropertyCriteriaView extends BorderPane {

  private final PropertyCriteriaModel<? extends Property.SearchableProperty> model;
  private final CheckBox enabledBox;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;

  public PropertyCriteriaView(final PropertyCriteriaModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.enabledBox = createEnabledBox();
    this.upperBoundControl = createUpperBoundControl();
    this.lowerBoundControl = createLowerBoundControl();
    initializeUI();
  }

  private CheckBox createEnabledBox() {
    final CheckBox box = FXUiUtil.createCheckBox(model.getEnabledState());

    return box;
  }

  private Control createUpperBoundControl() {
    return createControl();
  }

  private Control createLowerBoundControl() {
    return createControl();
  }

  private Control createControl() {
    final Control control;
    if (model instanceof ForeignKeyCriteriaModel) {
      control = FXUiUtil.createControl(model.getProperty(), ((ForeignKeyCriteriaModel) model).getConnectionProvider());
    }
    else {
      control = FXUiUtil.createControl(model.getProperty(), null);
    }
    control.maxWidthProperty().set(Double.MAX_VALUE);

    return control;
  }

  private void initializeUI() {
    setCenter(upperBoundControl);
    final BorderPane checkBoxPane = new BorderPane();
    checkBoxPane.setCenter(enabledBox);
    setRight(checkBoxPane);
    setMargin(checkBoxPane, new Insets(0, 0, 0, 5));
  }
}
