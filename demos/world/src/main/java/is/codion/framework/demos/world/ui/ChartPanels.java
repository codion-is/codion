package is.codion.framework.demos.world.ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;

import javax.swing.JComponent;
import javax.swing.UIManager;
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

  private static void linkColors(JComponent parent, JFreeChart chart) {
    setColors(chart, parent.getBackground());
    parent.addPropertyChangeListener("background", evt ->
            setColors(chart, (Color) evt.getNewValue()));
  }

  private static void setColors(JFreeChart chart, Color backgroundColor) {
    chart.setBackgroundPaint(backgroundColor);
    Plot plot = chart.getPlot();
    plot.setBackgroundPaint(backgroundColor);
    Color textFieldForeground = UIManager.getColor("TextField.foreground");
    if (plot instanceof PiePlot) {
      PiePlot<?> piePlot = (PiePlot<?>) plot;
      piePlot.setLabelBackgroundPaint(textFieldForeground);
      piePlot.setLabelPaint(backgroundColor);
    }
    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(backgroundColor);
      legend.setItemPaint(textFieldForeground);
    }
  }
}
