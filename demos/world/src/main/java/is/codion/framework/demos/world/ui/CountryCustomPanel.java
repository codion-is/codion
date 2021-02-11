package is.codion.framework.demos.world.ui;

import is.codion.common.model.table.ColumnSummary;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CountryCustomModel;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.demos.world.model.CountryTableModel;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.swing.common.ui.dialog.Dialogs.displayInDialog;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CountryCustomPanel extends EntityPanel {

  public CountryCustomPanel(CountryCustomModel entityModel) {
    super(entityModel,
            new CountryEditPanel((CountryEditModel) entityModel.getEditModel()),
            new CountryTablePanel((CountryTableModel) entityModel.getTableModel()));
  }

  @Override
  protected void initializeUI() {
    SwingEntityModel countryModel = getModel();
    countryModel.getTableModel().getColumnSummaryModel(Country.POPULATION).setSummary(ColumnSummary.SUM);
    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    cityModel.getTableModel().getColumnSummaryModel(City.POPULATION).setSummary(ColumnSummary.SUM);
    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);

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

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, countryTablePanel, southTabbedPane);
    splitPane.setContinuousLayout(true);
    splitPane.setResizeWeight(0.33);
    add(splitPane, BorderLayout.CENTER);

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
