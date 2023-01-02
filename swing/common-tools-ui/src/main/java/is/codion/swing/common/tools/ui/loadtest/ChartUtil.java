/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.loadtest;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;

final class ChartUtil {

  private ChartUtil() {}

  static void linkColors(JComponent parent, JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    setColors(parent, chart);
    parent.addPropertyChangeListener("background", evt -> setColors(parent, chart));
  }

  private static void setColors(JComponent parent, JFreeChart chart) {
    Color textFieldForeground = UIManager.getColor("TextField.foreground");
    chart.setBackgroundPaint(parent.getBackground());
    chart.getLegend().setBackgroundPaint(parent.getBackground());
    chart.getLegend().setItemPaint(textFieldForeground);
    setColors(chart.getXYPlot().getRangeAxis(), textFieldForeground);
    setColors(chart.getXYPlot().getDomainAxis(), textFieldForeground);
  }

  private static void setColors(ValueAxis axis, Color color) {
    axis.setLabelPaint(color);
    axis.setTickLabelPaint(color);
    axis.setAxisLinePaint(color);
    axis.setTickMarkPaint(color);
  }
}
