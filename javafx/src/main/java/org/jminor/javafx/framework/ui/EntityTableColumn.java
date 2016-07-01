/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

public final class EntityTableColumn extends FXEntityListModel.PropertyTableColumn {

  private final PropertyConditionView conditionView;

  public EntityTableColumn(final FXEntityListModel listModel, final Property property,
                           final EntityConnectionProvider connectionProvider,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    super(property);
    this.conditionView = initializeConditionView(listModel);
    setCellValueFactory(cellValueFactory);
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
  }

  public void setConditionViewAdvanced(final boolean advanced) {
    if (conditionView != null) {
      conditionView.setAdvanced(advanced);
    }
  }

  public void setConditionViewVisible(final boolean visible) {
    if (conditionView != null) {
      setGraphic(visible ? conditionView : null);
    }
  }

  private PropertyConditionView initializeConditionView(final FXEntityListModel listModel) {
    if (!(getProperty() instanceof Property.SearchableProperty)) {
      return null;
    }
    final ColumnConditionModel<? extends Property.SearchableProperty> conditionModel =
            listModel.getConditionModel().getPropertyConditionModel(getProperty().getPropertyID());
    final PropertyConditionView view = new PropertyConditionView(conditionModel);
    view.prefWidthProperty().setValue(getWidth());
    widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));
    widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));

    return view;
  }
}
