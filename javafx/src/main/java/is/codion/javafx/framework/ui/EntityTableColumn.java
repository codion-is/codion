/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.javafx.framework.model.FXEntityListModel;

import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

/**
 * A table column based on attributes via {@link FXEntityListModel.AttributeTableColumn}
 * @param <T> the column value type
 */
public final class EntityTableColumn<T> extends FXEntityListModel.AttributeTableColumn<T> {

  private final PropertyConditionView<T> conditionView;

  /**
   * Instantiates a new table column for the given list model based on the given property
   * @param listModel the list model this column is for
   * @param property the property
   * @param cellValueFactory the cell value factory for this column
   */
  public EntityTableColumn(final FXEntityListModel listModel, final Property<T> property,
                           final Callback<CellDataFeatures<Entity, T>, ObservableValue<T>> cellValueFactory) {
    super(property.getAttribute(), property.getCaption());
    this.conditionView = initializeConditionView(listModel, property);
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

  private PropertyConditionView<T> initializeConditionView(final FXEntityListModel listModel, final Property<T> property) {
    if (property instanceof ColumnProperty || property instanceof ForeignKeyProperty) {
      return listModel.getTableConditionModel().getConditionModelOptional(property.getAttribute()).map(conditionModel -> {
                final PropertyConditionView<T> view = new PropertyConditionView<>(conditionModel, property);
                view.prefWidthProperty().setValue(getWidth());
                widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));
                widthProperty().addListener((observable, oldValue, newValue) -> view.prefWidthProperty().set(newValue.doubleValue()));

                return view;
              })
              .orElse(null);
    }

    return null;
  }
}
