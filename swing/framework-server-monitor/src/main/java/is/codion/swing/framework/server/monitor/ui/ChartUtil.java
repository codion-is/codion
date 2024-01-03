/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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

  static void linkColors(JComponent parent, JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    setColors(parent, chart);
    parent.addPropertyChangeListener("background", evt -> setColors(parent, chart));
  }

  private static void setColors(JComponent parent, JFreeChart chart) {
    Color textFieldForeground = UIManager.getColor("TextField.foreground");
    TextTitle title = chart.getTitle();
    if (title != null) {
      title.setPaint(textFieldForeground);
    }
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
