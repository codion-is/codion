/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ForeignKeyCriteriaModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

public final class PropertyCriteriaView extends BorderPane {

  private final PropertyCriteriaModel<? extends Property.SearchableProperty> model;
  private final Label header;
  private final ToggleButton enabledButton;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;

  public PropertyCriteriaView(final Label header, final PropertyCriteriaModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.header = header;
    this.enabledButton = createEnabledButton();
    this.upperBoundControl = createUpperBoundControl();
    this.lowerBoundControl = createLowerBoundControl();
    initializeUI();
  }

  private ToggleButton createEnabledButton() {
    final ToggleButton button = FXUiUtil.createToggleButton(model.getEnabledState());
    button.setText("On");

    return button;
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
    setTop(header);
    setCenter(upperBoundControl);
    setRight(enabledButton);
  }
}
