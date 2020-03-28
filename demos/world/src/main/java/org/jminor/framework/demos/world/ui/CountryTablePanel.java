package org.jminor.framework.demos.world.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.demos.world.model.CountryTableModel;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JTabbedPane;

import static org.jfree.chart.ChartFactory.createPieChart;

public final class CountryTablePanel extends EntityTablePanel {

  private final JTabbedPane pieChartPane = new JTabbedPane();
  private final ChartPanel cityChartPanel;
  private final ChartPanel languageChartPanel;

  public CountryTablePanel(CountryTableModel tableModel) {
    super(tableModel);
    cityChartPanel = new ChartPanel(createPieChart("Cities", tableModel.getCitiesDataset()));
    cityChartPanel.getChart().removeLegend();
    languageChartPanel = new ChartPanel(createPieChart("Languages", tableModel.getLanguagesDataset()));
    languageChartPanel.getChart().removeLegend();
    pieChartPane.addTab("Cities", cityChartPanel);
    pieChartPane.addTab("Languages", languageChartPanel);
    getTable().setDoubleClickAction(Controls.control(this::displayChartPanel,
            tableModel.getSelectionModel().getSelectionNotEmptyObserver()));
  }

  public ChartPanel getCityChartPanel() {
    return cityChartPanel;
  }

  public ChartPanel getLanguageChartPanel() {
    return languageChartPanel;
  }

  private void displayChartPanel() throws DatabaseException {
    if (!pieChartPane.isShowing()) {
      Dialogs.displayInDialog(this, pieChartPane, Modal.NO);
    }
  }
}
