/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.framework.demos.world.ui.ChartPanels.createBarChartPanel;
import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.Sizes.setPreferredHeight;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_1;
import static java.awt.event.KeyEvent.VK_2;

final class ContinentPanel extends EntityPanel {

  ContinentPanel(ContinentModel continentModel) {
    super(continentModel, new ContinentTablePanel(continentModel.tableModel()));
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = model();

    ChartPanel populationChartPanel = createPieChartPanel(this, model.populationDataset(), "Population");
    ChartPanel surfaceAreaChartPanel = createPieChartPanel(this, model.surfaceAreaDataset(), "Surface area");
    ChartPanel gnpChartPanel = createPieChartPanel(this, model.gnpDataset(), "GNP");
    ChartPanel lifeExpectancyChartPanel = createBarChartPanel(this, model.lifeExpectancyDataset(), "Life expectancy", "Continent", "Years");
    setPreferredHeight(lifeExpectancyChartPanel, 120);

    Dimension pieChartSize = new Dimension(260, 260);
    populationChartPanel.setPreferredSize(pieChartSize);
    surfaceAreaChartPanel.setPreferredSize(pieChartSize);
    gnpChartPanel.setPreferredSize(pieChartSize);

    JPanel pieChartChartPanel = gridLayoutPanel(1, 3)
            .add(populationChartPanel)
            .add(surfaceAreaChartPanel)
            .add(gnpChartPanel)
            .build();

    JPanel chartPanel = borderLayoutPanel()
            .northComponent(lifeExpectancyChartPanel)
            .centerComponent(pieChartChartPanel)
            .build();

    EntityTablePanel countryTablePanel =
            new EntityTablePanel(model.detailModel(Country.TYPE).tableModel(),
                    config -> config.includeConditionPanel(false));
    setPreferredHeight(countryTablePanel, 300);

    JTabbedPane tabbedPane = tabbedPane()
            .tabBuilder("Charts", chartPanel)
            .mnemonic(VK_1)
            .add()
            .tabBuilder("Countries", countryTablePanel.initialize())
            .mnemonic(VK_2)
            .add()
            .build();

    setLayout(borderLayout());

    add(tablePanel().initialize(), BorderLayout.CENTER);
    add(tabbedPane, BorderLayout.SOUTH);

    setupKeyboardActions();
    setupNavigation();
  }
}
