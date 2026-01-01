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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static javax.swing.UIManager.getColor;
import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createPieChart;

final class ChartPanels {

	private ChartPanels() {}

	static ChartPanel createPieChartPanel(PieDataset<String> dataset, String title) {
		return new ColorChartPanel(createPieChart(title, dataset, false, false, false));
	}

	static ChartPanel createBarChartPanel(CategoryDataset dataset, String title,
																				String categoryLabel, String valueLabel) {
		return new ColorChartPanel(createBarChart(title, categoryLabel, valueLabel, dataset));
	}

	private static final class ColorChartPanel extends ChartPanel {

		private ColorChartPanel(JFreeChart chart) {
			super(chart);
			addPropertyChangeListener(new BackgroundListener());
			setColors();
		}

		private void setColors() {
			Color background = getBackground();
			Color foreground = getColor("TextField.foreground");
			JFreeChart chart = getChart();
			chart.setBackgroundPaint(background);
			chart.getTitle().setPaint(foreground);
			Plot plot = chart.getPlot();
			plot.setBackgroundPaint(background);
			if (plot instanceof PiePlot<?> piePlot) {
				piePlot.setLabelPaint(background);
				piePlot.setLabelBackgroundPaint(foreground);
			}
			if (plot instanceof CategoryPlot categoryPlot) {
				categoryPlot.getDomainAxis().setLabelPaint(foreground);
				categoryPlot.getDomainAxis().setTickLabelPaint(foreground);
				categoryPlot.getRangeAxis().setLabelPaint(foreground);
				categoryPlot.getRangeAxis().setTickLabelPaint(foreground);
			}
			LegendTitle legend = chart.getLegend();
			if (legend != null) {
				legend.setBackgroundPaint(background);
				legend.setItemPaint(foreground);
			}
		}

		private final class BackgroundListener implements PropertyChangeListener {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getPropertyName().equals("background")) {
					setColors();
				}
			}
		}
	}
}
