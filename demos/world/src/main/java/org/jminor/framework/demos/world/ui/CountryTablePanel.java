/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.demos.world.model.CountryTableModel;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

public final class CountryTablePanel extends EntityTablePanel {

  private final JFreeChart cityPieChart;
  private final ChartPanel cityChartPanel;

  public CountryTablePanel(CountryTableModel tableModel) {
    super(tableModel);
    cityPieChart = ChartFactory.createPieChart("Cities", ((CountryTableModel) getTableModel()).getCityChartDataset());
    cityChartPanel = new ChartPanel(cityPieChart);
    getTable().setDoubleClickAction(Controls.control(this::displayCityPieChart,
            "displayPieChart", tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver()));
  }

  private void displayCityPieChart() throws DatabaseException {
    if (!cityChartPanel.isShowing()) {
      Dialogs.displayInDialog(this, cityChartPanel, null, false);
    }
  }
}
