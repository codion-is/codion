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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
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
		if (plot instanceof CategoryPlot) {
			CategoryPlot categoryPlot = (CategoryPlot) plot;
			categoryPlot.getDomainAxis().setLabelPaint(textFieldForeground);
			categoryPlot.getDomainAxis().setTickLabelPaint(textFieldForeground);
			categoryPlot.getRangeAxis().setTickLabelPaint(textFieldForeground);
			categoryPlot.getRangeAxis().setLabelPaint(textFieldForeground);
		}
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setBackgroundPaint(backgroundColor);
			legend.setItemPaint(textFieldForeground);
		}
		chart.getTitle().setPaint(textFieldForeground);
	}
}
