/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.model.CancelException;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.framework.demos.chinook.model.TrackTableModel;
import dev.codion.framework.domain.property.Property;
import dev.codion.swing.common.ui.control.ControlList;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.common.ui.dialog.Dialogs;
import dev.codion.swing.common.ui.dialog.Modal;
import dev.codion.swing.common.ui.textfield.DecimalField;
import dev.codion.swing.common.ui.value.ComponentValue;
import dev.codion.swing.common.ui.value.ComponentValuePanel;
import dev.codion.swing.common.ui.value.NumericalValues;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.model.SwingEntityTableModel;
import dev.codion.swing.framework.ui.EntityComponentValues;
import dev.codion.swing.framework.ui.EntityTableConditionPanel;
import dev.codion.swing.framework.ui.EntityTablePanel;

import java.math.BigDecimal;
import java.util.List;

public class TrackTablePanel extends EntityTablePanel {

  public TrackTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel, new TrackComponentValues(), new EntityTableConditionPanel(tableModel));
  }

  @Override
  protected ControlList getPopupControls(final List<ControlList> additionalPopupControls) {
    final ControlList controls = super.getPopupControls(additionalPopupControls);
    controls.addAt(0, Controls.control(this::raisePriceOfSelected, "Raise price...",
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver()));
    controls.addSeparatorAt(1);

    return controls;
  }

  private void raisePriceOfSelected() throws DatabaseException {
    final TrackTableModel tableModel = (TrackTableModel) getTableModel();

    tableModel.raisePriceOfSelected(getAmountFromUser());
  }

  private BigDecimal getAmountFromUser() {
    final ComponentValuePanel<BigDecimal, DecimalField> inputPanel =
            new ComponentValuePanel<>("Amount",
                    NumericalValues.bigDecimalValue(new DecimalField()));
    Dialogs.displayInDialog(this, inputPanel, "Price Raise", Modal.YES,
            inputPanel.getOkAction(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted() && inputPanel.getValue() != null) {
      return inputPanel.getValue();
    }

    throw new CancelException();
  }

  private static final class TrackComponentValues extends EntityComponentValues {

    @Override
    public ComponentValue createComponentValue(final Property property,
                                               final SwingEntityEditModel editModel,
                                               final Object initialValue) {
      if (property.is(Chinook.TRACK_MILLISECONDS)) {
        return new MinutesSecondsPanelValue();
      }

      return super.createComponentValue(property, editModel, initialValue);
    }
  }
}
