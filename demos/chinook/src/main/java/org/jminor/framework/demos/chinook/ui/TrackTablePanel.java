/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.framework.demos.chinook.model.TrackTableModel;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.value.ComponentValuePanel;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.math.BigDecimal;
import java.util.List;

public class TrackTablePanel extends EntityTablePanel {

  public TrackTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlSet getPopupControls(final List<ControlSet> additionalPopupControlSets) {
    final ControlSet controls = super.getPopupControls(additionalPopupControlSets);
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
}
