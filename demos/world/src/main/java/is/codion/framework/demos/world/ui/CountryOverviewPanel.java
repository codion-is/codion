package is.codion.framework.demos.world.ui;

import is.codion.common.model.table.ColumnSummary;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CountryOverviewModel;
import is.codion.framework.demos.world.model.CountryOverviewTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.PieDataset;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static org.jfree.chart.ChartFactory.createPieChart;

public final class CountryOverviewPanel extends EntityPanel {

  private final ChartPanel cityChartPanel;
  private final ChartPanel languageChartPanel;

  public CountryOverviewPanel(CountryOverviewModel countryOverviewModel) {
    super(countryOverviewModel, new CountryEditPanel(countryOverviewModel.getEditModel()));
    CountryOverviewTableModel tableModel = (CountryOverviewTableModel) countryOverviewModel.getTableModel();
    cityChartPanel = createChartPanel("Cities", tableModel.getCitiesDataset());
    languageChartPanel = createChartPanel("Languages", tableModel.getLanguagesDataset());
    setCaption("Country Overview");
  }

  @Override
  protected void initializeUI() {
    SwingEntityModel countryModel = getModel();
    countryModel.getTableModel().getColumnSummaryModel(Country.POPULATION)
            .ifPresent(summaryModel -> summaryModel.setSummary(ColumnSummary.SUM));
    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    cityModel.getTableModel().getColumnSummaryModel(City.POPULATION)
            .ifPresent(summaryModel -> summaryModel.setSummary(ColumnSummary.SUM));
    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);

    countryModel.addLinkedDetailModel(cityModel);
    countryModel.addLinkedDetailModel(countryLanguageModel);

    EntityTablePanel countryTablePanel = getTablePanel();
    countryTablePanel.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    countryTablePanel.getTable().setDoubleClickAction(Control.control(this::displayEditPanel));
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

    EntityEditPanel countryEditPanel = getEditPanel();

    countryEditPanel.initializePanel();
    countryTablePanel.initializePanel();
    cityPanel.initializePanel();
    languagePanel.initializePanel();

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
    JPanel editPanel = getEditControlPanel();
    if (!editPanel.isShowing()) {
      Dialogs.componentDialog(editPanel)
              .owner(this)
              .modal(false)
              .show();
    }
    getEditPanel().requestInitialFocus();
  }

  private ChartPanel createChartPanel(String title, PieDataset<String> dataset) {
    JFreeChart languagesChart = createPieChart(title, dataset);
    languagesChart.getPlot().setBackgroundPaint(UIManager.getColor("Table.background"));
    languagesChart.setBackgroundPaint(this.getBackground());
    ChartPanel chartPanel = new ChartPanel(languagesChart);
    chartPanel.getChart().removeLegend();
    chartPanel.setPreferredSize(new Dimension(300, 300));

    return chartPanel;
  }
}
