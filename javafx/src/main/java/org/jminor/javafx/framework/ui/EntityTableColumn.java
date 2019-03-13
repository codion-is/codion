/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

/**
 * A table column based on properties via {@link FXEntityListModel.PropertyTableColumn}
 */
public final class EntityTableColumn extends FXEntityListModel.PropertyTableColumn {

  private final PropertyConditionView conditionView;

  /**
   * Instantiates a new table column for the given list model based on the given property
   * @param listModel the list model this column is for
   * @param property the property
   * @param cellValueFactory the cell value factory for this column
   */
  public EntityTableColumn(final FXEntityListModel listModel, final Property property,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    super(property);
    this.conditionView = initializeConditionView(listModel);
    setCellValueFactory(cellValueFactory);
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
  }

  /**
   * Toggles the advanced property condition view
   * @param advanced the toggle value
   */
  public void setConditionViewAdvanced(final boolean advanced) {
    if (conditionView != null) {
      conditionView.setAdvanced(advanced);
    }
  }

  /**
   * Toggles the visibility of the property condition view
   * @param visible the toggle values
   */
  public void setConditionViewVisible(final boolean visible) {
    if (conditionView != null) {
      setGraphic(visible ? conditionView : null);
    }
  }

  private PropertyConditionView initializeConditionView(final FXEntityListModel listModel) {
    final Property property = getProperty();
    if (property instanceof Property.ColumnProperty || property instanceof Property.ForeignKeyProperty) {
      final ColumnConditionModel<? extends Property> conditionModel =
              listModel.getConditionModel().getPropertyConditionModel(getProperty().getPropertyId());
      if (conditionModel != null) {
        final PropertyConditionView view = new PropertyConditionView(conditionModel);
        view.prefWidthProperty().setValue(getWidth());
        widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));
        widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));

        return view;
      }
    }

    return null;
  }
}
