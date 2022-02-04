package is.codion.framework.demos.world.ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;

import javax.swing.JComponent;
import java.awt.Color;

import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createPieChart;

final class ChartPanels {

  private ChartPanels() {}

  static ChartPanel createPieChartPanel(JComponent parent, PieDataset<String> dataset, String title) {
    JFreeChart chart = createPieChart(title, dataset);
    chart.removeLegend();
    linkColors(parent, chart);

    return new ChartPanel(chart);
  }

  static ChartPanel createBarChartPanel(JComponent parent, CategoryDataset dataset, String title,
                                        String categoryLabel, String valueLabel) {
    JFreeChart chart = createBarChart(title, categoryLabel, valueLabel, dataset);
    linkColors(parent, chart);

    return new ChartPanel(chart);
  }

  private static void linkColors(final JComponent parent, final JFreeChart chart) {
    chart.setBackgroundPaint(parent.getBackground());
    chart.getPlot().setBackgroundPaint(parent.getBackground());
    parent.addPropertyChangeListener("background", evt -> {
      Color newValue = (Color) evt.getNewValue();
      chart.setBackgroundPaint(newValue);
      chart.getPlot().setBackgroundPaint(newValue);
    });
  }
}
