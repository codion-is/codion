/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

public final class EntityTableColumn extends FXEntityListModel.PropertyColumn {

  private final PropertyCriteriaView criteriaView;

  public EntityTableColumn(final FXEntityListModel listModel, final Property property,
                           final EntityConnectionProvider connectionProvider,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    super(property);
    this.criteriaView = initializeCriteriaView(listModel);
    setCellValueFactory(cellValueFactory);
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
  }

  public void setCriteriaViewAdvanced(final boolean advanced) {
    if (criteriaView != null) {
      criteriaView.setAdvanced(advanced);
    }
  }

  public void setCriteriaViewVisible(final boolean visible) {
    if (criteriaView != null) {
      setGraphic(visible ? criteriaView : null);
    }
  }

  private PropertyCriteriaView initializeCriteriaView(final FXEntityListModel listModel) {
    if (!(getProperty() instanceof Property.SearchableProperty)) {
      return null;
    }
    final ColumnCriteriaModel<? extends Property.SearchableProperty> criteriaModel =
            listModel.getCriteriaModel().getPropertyCriteriaModel(getProperty().getPropertyID());
    final PropertyCriteriaView criteriaView = new PropertyCriteriaView(criteriaModel);
    criteriaView.prefWidthProperty().setValue(getWidth());
    widthProperty().addListener((observable, oldValue, newValue) -> {
      criteriaView.prefWidthProperty().set(newValue.doubleValue());
    });
    widthProperty().addListener((observable, oldValue, newValue) -> {
      criteriaView.prefWidthProperty().set(newValue.doubleValue());
    });

    return criteriaView;
  }

  private Pane createCenterPane(final FXEntityListModel listModel) {
    if (getProperty() instanceof Property.SearchableProperty) {
      return criteriaView;
    }

    return new Pane();
  }
}
