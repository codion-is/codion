package dev.codion.framework.demos.world.ui;

import dev.codion.common.model.table.ColumnSummary;
import dev.codion.framework.demos.world.domain.World;
import dev.codion.framework.demos.world.model.CountryCustomModel;
import dev.codion.framework.demos.world.model.CountryEditModel;
import dev.codion.framework.demos.world.model.CountryTableModel;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.common.ui.dialog.Modal;
import dev.codion.swing.framework.model.SwingEntityModel;
import dev.codion.swing.framework.ui.EntityPanel;

import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static dev.codion.swing.common.ui.dialog.Dialogs.displayInDialog;
import static dev.codion.swing.common.ui.layout.Layouts.borderLayout;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CountryCustomPanel extends EntityPanel {

  public CountryCustomPanel(final CountryCustomModel entityModel) {
    super(entityModel,
            new CountryEditPanel((CountryEditModel) entityModel.getEditModel()),
            new CountryTablePanel((CountryTableModel) entityModel.getTableModel()));
  }

  @Override
  protected void initializeUI() {
    SwingEntityModel countryModel = getModel();
    countryModel.getTableModel().getColumnSummaryModel(World.COUNTRY_POPULATION).setSummary(ColumnSummary.SUM);
    SwingEntityModel cityModel = countryModel.getDetailModel(World.T_CITY);
    cityModel.getTableModel().getColumnSummaryModel(World.CITY_POPULATION).setSummary(ColumnSummary.SUM);
    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(World.T_COUNTRYLANGUAGE);

    countryModel.addLinkedDetailModel(cityModel);
    countryModel.addLinkedDetailModel(countryLanguageModel);

    CountryEditPanel countryEditPanel = (CountryEditPanel) getEditPanel();
    CountryTablePanel countryTablePanel = (CountryTablePanel) getTablePanel();
    countryTablePanel.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    countryTablePanel.getTable().setDoubleClickAction(Controls.control(this::displayEditPanel));
    countryTablePanel.setSummaryPanelVisible(true);

    EntityPanel cityPanel = new EntityPanel(cityModel);
    cityPanel.setBorder(BorderFactory.createTitledBorder("Cities"));
    cityPanel.getTablePanel().getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    cityPanel.getTablePanel().setSummaryPanelVisible(true);
    cityPanel.getTablePanel().setIncludeSouthPanel(false);

    EntityPanel languagePanel = new EntityPanel(countryLanguageModel);
    languagePanel.setBorder(BorderFactory.createTitledBorder("Languages"));
    languagePanel.getTablePanel().getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    languagePanel.getTablePanel().setIncludeSouthPanel(false);

    addDetailPanels(cityPanel, languagePanel);

    countryEditPanel.initializePanel();
    countryTablePanel.initializePanel();
    cityPanel.initializePanel();
    languagePanel.initializePanel();

    ChartPanel cityChartPanel = countryTablePanel.getCityChartPanel();
    cityChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel languageChartPanel = countryTablePanel.getLanguageChartPanel();
    languageChartPanel.setPreferredSize(new Dimension(300, 300));

    JPanel southTablePanel = new JPanel(gridLayout(1, 2));
    southTablePanel.add(cityPanel);
    southTablePanel.add(languagePanel);

    JPanel southChartPanel = new JPanel(gridLayout(1, 2));
    southChartPanel.add(cityChartPanel);
    southChartPanel.add(languageChartPanel);

    JTabbedPane southTabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
    southTabbedPane.addTab("Tables", southTablePanel);
    southTabbedPane.addTab("Charts", southChartPanel);
    southTabbedPane.setMnemonicAt(0, 'T');
    southTabbedPane.setMnemonicAt(1, 'C');

    setLayout(borderLayout());

    add(countryTablePanel, BorderLayout.CENTER);
    add(southTabbedPane, BorderLayout.SOUTH);

    initializeEditControlPanel();
    initializeKeyboardActions();
    initializeNavigation();
  }

  private void displayEditPanel() {
    final JPanel editPanel = getEditControlPanel();
    if (!editPanel.isShowing()) {
      displayInDialog(this, editPanel, Modal.NO);
    }
    getEditPanel().requestInitialFocus();
  }
}
