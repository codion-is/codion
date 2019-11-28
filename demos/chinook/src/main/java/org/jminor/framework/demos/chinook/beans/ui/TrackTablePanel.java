/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.input.BigDecimalInputProvider;
import org.jminor.swing.common.ui.input.InputProviderPanel;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.math.BigDecimal;
import java.util.List;

import static org.jminor.framework.demos.chinook.domain.Chinook.F_RAISE_PRICE;
import static org.jminor.framework.demos.chinook.domain.Chinook.TRACK_TRACKID;

public class TrackTablePanel extends EntityTablePanel {

  public TrackTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlSet getPopupControls(final List<ControlSet> additionalPopupControlSets) {
    final ControlSet controls = super.getPopupControls(additionalPopupControlSets);
    controls.addAt(Controls.control(this::raisePriceOfSelected, "Raise price...",
            getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver()), 0);
    controls.addSeparatorAt(1);

    return controls;
  }

  private void raisePriceOfSelected() throws DatabaseException {
    final SwingEntityTableModel tableModel = getEntityTableModel();

    final BigDecimal increase = getAmountFromUser();
    final List<Object> trackIds = Entities.getValues(TRACK_TRACKID,
            tableModel.getSelectionModel().getSelectedItems());

    final List<Entity> result = tableModel.getConnectionProvider().getConnection()
            .executeFunction(F_RAISE_PRICE, trackIds, increase);
    tableModel.replaceEntities(result);
  }

  private BigDecimal getAmountFromUser() {
    final InputProviderPanel<BigDecimal, DecimalField> inputPanel =
            new InputProviderPanel<>("Amount",
                    new BigDecimalInputProvider(null));
    UiUtil.displayInDialog(this, inputPanel, "Price Raise", true,
            inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted() && inputPanel.getValue() != null) {
      return inputPanel.getValue();
    }

    throw new CancelException();
  }
}
