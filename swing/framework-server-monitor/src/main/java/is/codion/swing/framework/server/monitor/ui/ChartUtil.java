/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.title.TextTitle;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;

final class ChartUtil {

  private ChartUtil() {}

  static void linkColors(final JComponent parent, final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    setColors(parent, chart);
    parent.addPropertyChangeListener("background", evt -> setColors(parent, chart));
  }

  private static void setColors(final JComponent parent, final JFreeChart chart) {
    final Color textFieldForeground = UIManager.getColor("TextField.foreground");
    final TextTitle title = chart.getTitle();
    if (title != null) {
      title.setPaint(textFieldForeground);
    }
    chart.setBackgroundPaint(parent.getBackground());
    chart.getLegend().setBackgroundPaint(parent.getBackground());
    chart.getLegend().setItemPaint(textFieldForeground);
    setColors(chart.getXYPlot().getRangeAxis(), textFieldForeground);
    setColors(chart.getXYPlot().getDomainAxis(), textFieldForeground);
  }

  private static void setColors(final ValueAxis axis, final Color color) {
    axis.setLabelPaint(color);
    axis.setTickLabelPaint(color);
    axis.setAxisLinePaint(color);
    axis.setTickMarkPaint(color);
  }
}