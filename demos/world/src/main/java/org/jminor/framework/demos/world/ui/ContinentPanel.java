package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.model.ContinentModel;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createPieChart;

public final class ContinentPanel extends EntityPanel {

  public ContinentPanel(final ContinentModel entityModel) {
    super(entityModel);
    getTablePanel().getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = (ContinentModel) getModel();
    setLayout(Layouts.borderLayout());

    EntityTablePanel tablePanel = getTablePanel();
    tablePanel.setIncludeSouthPanel(false);
    tablePanel.setIncludePopupMenu(false);
    tablePanel.initializePanel();

    ChartPanel populationChartPanel = new ChartPanel(createPieChart("Population",
            model.getPopulationChartDataset()));
    populationChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel surfaceAreaChartPanel = new ChartPanel(createPieChart("Surface area",
            model.getSurfaceAreaChartDataset()));
    surfaceAreaChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel gnpAreaChartPanel = new ChartPanel(createPieChart("GNP",
            model.getGNPChartDataset()));
    gnpAreaChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel lifeExpectancyChartPanel = new ChartPanel(createBarChart("Life expectancy",
            "Continent", "Years", model.getLifeExpectancyDataset()));

    JPanel centerPanel = new JPanel(Layouts.gridLayout(1, 2));
    centerPanel.add(tablePanel);
    centerPanel.add(lifeExpectancyChartPanel);

    JPanel southChartPanel = new JPanel(Layouts.gridLayout(1, 3));
    southChartPanel.add(populationChartPanel);
    southChartPanel.add(surfaceAreaChartPanel);
    southChartPanel.add(gnpAreaChartPanel);

    add(centerPanel, BorderLayout.CENTER);
    add(southChartPanel, BorderLayout.SOUTH);

    initializeNavigation();
  }
}
