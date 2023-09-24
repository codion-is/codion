/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.framework.model.SwingEntityModel;
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
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_1;
import static java.awt.event.KeyEvent.VK_2;

final class ContinentPanel extends EntityPanel {

  private final EntityPanel countryPanel;

  ContinentPanel(SwingEntityModel continentModel) {
    super(continentModel, new ContinentTablePanel(continentModel.tableModel()),
            new ContinentPanelLayout());
    countryPanel = new EntityPanel(continentModel.detailModel(Country.TYPE));
    countryPanel.tablePanel().setIncludeConditionPanel(false);
  }

  private static final class ContinentPanelLayout implements PanelLayout {

    @Override
    public void layoutPanel(EntityPanel entityPanel) {
      ContinentPanel continentPanel = (ContinentPanel) entityPanel;
      ContinentModel model = entityPanel.model();

      EntityTablePanel tablePanel = entityPanel.tablePanel();
      tablePanel.setBorder(emptyBorder());
      tablePanel.initialize();
      setPreferredHeight(tablePanel, 200);

      ChartPanel populationChartPanel = createPieChartPanel(entityPanel, model.populationDataset(), "Population");
      ChartPanel surfaceAreaChartPanel = createPieChartPanel(entityPanel, model.surfaceAreaDataset(), "Surface area");
      ChartPanel gnpChartPanel = createPieChartPanel(entityPanel, model.gnpDataset(), "GNP");
      ChartPanel lifeExpectancyChartPanel = createBarChartPanel(entityPanel, model.lifeExpectancyDataset(), "Life expectancy", "Continent", "Years");
      lifeExpectancyChartPanel.setPreferredSize(new Dimension(lifeExpectancyChartPanel.getPreferredSize().width, 120));

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

      continentPanel.countryPanel.initialize();
      continentPanel.countryPanel.setPreferredSize(new Dimension(continentPanel.countryPanel.getPreferredSize().width, 100));

      JTabbedPane tabbedPane = tabbedPane()
              .tabBuilder("Charts", chartPanel)
              .mnemonic(VK_1)
              .add()
              .tabBuilder("Countries", continentPanel.countryPanel)
              .mnemonic(VK_2)
              .add()
              .build();

      continentPanel.setLayout(borderLayout());

      continentPanel.add(tablePanel, BorderLayout.CENTER);
      continentPanel.add(tabbedPane, BorderLayout.SOUTH);

      continentPanel.setupKeyboardActions();
      continentPanel.setupNavigation();
    }
  }
}
