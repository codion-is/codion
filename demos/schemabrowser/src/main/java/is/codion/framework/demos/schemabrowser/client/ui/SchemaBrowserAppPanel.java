/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.client.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.TableColumn;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JTable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static is.codion.swing.framework.ui.TabbedDetailLayout.splitPaneResizeWeight;
import static is.codion.swing.framework.ui.WindowDetailLayout.windowDetailLayout;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";

  public SchemaBrowserAppPanel(SchemaBrowserApplicationModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    SwingEntityModel schemaModel = applicationModel().entityModel(Schema.TYPE);
    SwingEntityModel tableModel = schemaModel.detailModel(Table.TYPE);
    SwingEntityModel columnModel = tableModel.detailModel(TableColumn.TYPE);
    SwingEntityModel constraintModel = tableModel.detailModel(Constraint.TYPE);
    SwingEntityModel columnConstraintModel = constraintModel.detailModel(ConstraintColumn.TYPE);

    EntityPanel schemaPanel = new EntityPanel(schemaModel,
            config -> config.detailLayout(splitPaneResizeWeight(0.3)));
    EntityPanel tablePanel = new EntityPanel(tableModel,
            config -> config.detailLayout(windowDetailLayout()));
    EntityPanel columnPanel = new EntityPanel(columnModel);
    EntityPanel constraintPanel = new EntityPanel(constraintModel);
    EntityPanel columnConstraintPanel = new EntityPanel(columnConstraintModel);

    schemaPanel.addDetailPanel(tablePanel);
    tablePanel.addDetailPanels(columnPanel);
    tablePanel.addDetailPanel(constraintPanel);
    constraintPanel.addDetailPanel(columnConstraintPanel);

    schemaModel.tableModel().refresh();

    return Collections.singletonList(schemaPanel);
  }

  public static void main(String[] args) {
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    FilteredTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.Config.CONDITION_PANEL_VISIBLE.set(true);
    EntityApplicationPanel.builder(SchemaBrowserApplicationModel.class, SchemaBrowserAppPanel.class)
            .applicationName("Schema Browser")
            .domainType(SchemaBrowser.DOMAIN)
            .defaultLoginUser(User.parse("scott:tiger"))
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .start();
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
      SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
      SwingEntityModel columnModel = new SwingEntityModel(TableColumn.TYPE, connectionProvider);
      SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
      SwingEntityModel constraintColumnModel = new SwingEntityModel(ConstraintColumn.TYPE, connectionProvider);

      schemaModel.addDetailModel(tableModel);
      tableModel.addDetailModels(columnModel, constraintModel);
      constraintModel.addDetailModels(constraintColumnModel);

      addEntityModel(schemaModel);
    }
  }
}