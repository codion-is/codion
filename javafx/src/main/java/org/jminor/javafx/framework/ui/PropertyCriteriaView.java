/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ForeignKeyCriteriaModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
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

  public CheckBox getEnabledCheckBox() {
    return enabledBox;
  }

  private CheckBox createEnabledBox() {
    return FXUiUtil.createCheckBox(model.getEnabledState());
  }

  private Control createUpperBoundControl() {
    final Control control = createControl();
    final Value value = FXUiUtil.createValue(model.getProperty(), control, null);
    Values.link(model.getUpperBoundValue(), value);

    return control;
  }

  private Control createLowerBoundControl() {
    final Control control = createControl();
    final Value value = FXUiUtil.createValue(model.getProperty(), control, null);
    Values.link(model.getLowerBoundValue(), value);

    return control;
  }

  private Control createControl() {
    final Control control;
    if (model instanceof ForeignKeyCriteriaModel) {
      control = FXUiUtil.createControl(model.getProperty(), ((ForeignKeyCriteriaModel) model).getConnectionProvider());
    }
    else {
      control = FXUiUtil.createControl(model.getProperty(), null);
    }
    control.setOnKeyReleased(event -> {
      if (event.getCode().equals(KeyCode.ENTER)) {
        model.getEnabledState().setActive(!model.getEnabledState().isActive());
      }
    });

    return control;
  }

  private void initializeUI() {
    setCenter(upperBoundControl);
  }
}
