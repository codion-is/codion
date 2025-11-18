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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.model.AnalyticsModel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.UIManager.getColor;
import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createLineChart;

final class AnalyticsPanel extends JPanel {

	private static final ResourceBundle BUNDLE = getBundle(AnalyticsPanel.class.getName());

	private final AnalyticsModel analytics;

	AnalyticsPanel(AnalyticsModel analytics) {
		super(borderLayout());
		this.analytics = analytics;
		add(tabbedPane()
						.tab(BUNDLE.getString("sales_comparison"), createSalesComparisonPanel())
						.tab(BUNDLE.getString("top_artists"), createTopArtistsPanel())
						.tab(BUNDLE.getString("artist_revenue"), createTopArtistRevenuePanel())
						.build(), BorderLayout.CENTER);
	}

	AnalyticsModel model() {
		return analytics;
	}

	private ChartPanel createSalesComparisonPanel() {
		return new LookAndFeelChartPanel(createLineChart(
						BUNDLE.getString("sales_comparison"),
						BUNDLE.getString("month"),
						BUNDLE.getString("revenue"),
						analytics.salesComparison().dataset()));
	}

	private JPanel createTopArtistsPanel() {
		JFreeChart chart = createBarChart(
						BUNDLE.getString("top_artists"),
						BUNDLE.getString("artist"),
						BUNDLE.getString("avarage_rating"),
						analytics.topArtists().dataset());

		chart.getCategoryPlot().getRangeAxis().setRange(0, 10);

		return borderLayoutPanel()
						.north(flowLayoutPanel(FlowLayout.LEADING)
										.add(borderLayoutPanel()
														.center(EntityComboBox.builder()
																		.model(analytics.topArtists().genreComboBoxModel())
																		.preferredWidth(180))
														.border(createTitledBorder(analytics.connectionProvider().entities()
																		.definition(Track.TYPE)
																		.attributes()
																		.definition(Track.GENRE_FK)
																		.caption()))))
						.center(new LookAndFeelChartPanel(chart))
						.build();
	}

	private ChartPanel createTopArtistRevenuePanel() {
		return new LookAndFeelChartPanel(createBarChart(
						BUNDLE.getString("artist_revenue"),
						BUNDLE.getString("artist"),
						BUNDLE.getString("total_revenue"),
						analytics.topArtistRevenue().dataset()));
	}

	private static final class LookAndFeelChartPanel extends ChartPanel {

		private LookAndFeelChartPanel(JFreeChart chart) {
			super(chart);
			addPropertyChangeListener(new BackgroundListener());
			updateColors();
		}

		private void updateColors() {
			Color background = getBackground();
			Color foreground = getColor("TextField.foreground");
			JFreeChart chart = getChart();
			chart.setBackgroundPaint(background);
			chart.getTitle().setPaint(foreground);
			Plot plot = chart.getPlot();
			plot.setBackgroundPaint(background);
			if (plot instanceof CategoryPlot categoryPlot) {
				categoryPlot.getDomainAxis().setLabelPaint(foreground);
				categoryPlot.getDomainAxis().setTickLabelPaint(foreground);
				categoryPlot.getRangeAxis().setLabelPaint(foreground);
				categoryPlot.getRangeAxis().setTickLabelPaint(foreground);
				categoryPlot.setRangeGridlinePaint(foreground);
				categoryPlot.setDomainGridlinePaint(foreground);
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
					updateColors();
				}
			}
		}
	}
}
