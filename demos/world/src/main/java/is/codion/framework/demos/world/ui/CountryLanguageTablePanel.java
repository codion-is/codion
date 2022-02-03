package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CountryLanguageTableModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.PieDataset;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static org.jfree.chart.ChartFactory.createPieChart;

final class CountryLanguageTablePanel extends EntityTablePanel {

  CountryLanguageTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    JPanel tableViewPanel = new JPanel(borderLayout());
    tableViewPanel.add(tablePanel, BorderLayout.CENTER);
    tableViewPanel.add(southPanel, BorderLayout.SOUTH);
    ChartPanel cityChartPanel = createChartPanel("Languages", ((CountryLanguageTableModel) getTableModel()).getChartDataset());
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Table", tableViewPanel);
    tabbedPane.addTab("Chart", cityChartPanel);
    setLayout(borderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  private ChartPanel createChartPanel(String title, PieDataset<String> dataset) {
    JFreeChart languagesChart = createPieChart(title, dataset);
    languagesChart.getPlot().setBackgroundPaint(UIManager.getColor("Table.background"));
    languagesChart.setBackgroundPaint(getBackground());
    ChartPanel chartPanel = new ChartPanel(languagesChart);
    chartPanel.getChart().removeLegend();

    return chartPanel;
  }
}
