/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.profiling;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;
import org.jminor.framework.profiling.ProfilingModel;
import org.jminor.framework.profiling.ui.ProfilingPanel;
import org.jminor.framework.server.EntityDbRemoteProvider;

import javax.swing.UIManager;

/**
 * User: Björn Darri
 * Date: 30.11.2007
 * Time: 04:00:43
 */
public class SchemaBrowserProfiling extends ProfilingModel {

  private static final User user = new User("scott", "tiger");

  /** Constructs a new SchemaBrowserProfiling.
   * @param user the user to use for database access during profiling
   */
  public SchemaBrowserProfiling(final User user) {
    super(user);
  }

  /** {@inheritDoc} */
  @Override
  protected void loadDomainModel() {
    new SchemaBrowser();
  }

  /** {@inheritDoc} */
  @Override
  protected void performWork(EntityApplicationModel applicationModel) {
    final EntityModel schemaModel = applicationModel.getMainApplicationModels().iterator().next();
    try {
      schemaModel.getTableModel().refresh();
      selectRandomRow(schemaModel.getTableModel());
      selectRandomRow(schemaModel.getDetailModels().get(0).getTableModel());
      selectRandomRow(schemaModel.getDetailModels().get(0).getDetailModels().get(0).getTableModel());
    }
    catch (UserException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  @Override
  protected EntityApplicationModel initializeApplicationModel() throws UserException, UserCancelException {
    final EntityApplicationModel ret =
            new SchemaBrowserAppModel(new EntityDbRemoteProvider(getUser(), user+"@"+new Object(), getClass().getSimpleName()));
    final EntityModel schemaModel = ret.getMainApplicationModels().iterator().next();
    schemaModel.setLinkedDetailModel(schemaModel.getDetailModels().get(0));
    final EntityModel dbObjectModel = schemaModel.getDetailModels().get(0);
    dbObjectModel.setLinkedDetailModel(dbObjectModel.getDetailModels().get(0));

    return ret;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new ProfilingPanel(new SchemaBrowserProfiling(user));
  }
}
