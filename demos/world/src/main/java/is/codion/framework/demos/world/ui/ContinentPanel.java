package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;

import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.swing.common.ui.Sizes.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createPieChart;

final class ContinentPanel extends EntityPanel {

  ContinentPanel(SwingEntityModel continentModel) {
    super(continentModel, new ContinentTablePanel(continentModel.getTableModel()));
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = (ContinentModel) getModel();

    EntityTablePanel tablePanel = getTablePanel();
    tablePanel.initializePanel();
    setPreferredHeight(tablePanel, 200);

    ChartPanel populationChartPanel = createChartPanel("Population", model.getPopulationDataset());
    ChartPanel surfaceAreaChartPanel = createChartPanel("Surface area", model.getSurfaceAreaDataset());
    ChartPanel gnpChartPanel = createChartPanel("GNP", model.getGnpDataset());
    ChartPanel lifeExpectancyChartPanel = createLifeExpectancyChartPanel(model.getLifeExpectancyDataset());

    JPanel centerPanel = new JPanel(borderLayout());
    centerPanel.add(tablePanel, BorderLayout.NORTH);
    centerPanel.add(lifeExpectancyChartPanel, BorderLayout.CENTER);

    JPanel southChartPanel = new JPanel(gridLayout(1, 3));
    southChartPanel.add(populationChartPanel);
    southChartPanel.add(surfaceAreaChartPanel);
    southChartPanel.add(gnpChartPanel);

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(southChartPanel, BorderLayout.SOUTH);

    initializeKeyboardActions();
    initializeNavigation();
  }

  private ChartPanel createChartPanel(String title, PieDataset<String> dataset) {
    JFreeChart chart = createPieChart(title, dataset);
    chart.getPlot().setBackgroundPaint(UIManager.getColor("Table.background"));
    chart.setBackgroundPaint(this.getBackground());
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.getChart().removeLegend();
    chartPanel.setPreferredSize(new Dimension(300, 300));

    return chartPanel;
  }

  private ChartPanel createLifeExpectancyChartPanel(CategoryDataset dataset) {
    JFreeChart chart = createBarChart("Life expectancy", "Continent", "Years", dataset);
    chart.getPlot().setBackgroundPaint(UIManager.getColor("Table.background"));
    chart.setBackgroundPaint(this.getBackground());

    return new ChartPanel(chart);
  }
}
