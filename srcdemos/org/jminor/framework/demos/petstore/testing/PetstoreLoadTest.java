/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.client.PetstoreAppModel;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
public class PetstoreLoadTest extends EntityLoadTestModel {

  public PetstoreLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  @Override
  protected void loadDomainModel() {
    new Petstore();
  }

  @Override
  protected Collection<UsageScenario> initializeUsageScenarios() {
    final UsageScenario scenario = new UsageScenario("selectRecords") {
      @Override
      protected void performScenario(final Object application) throws Exception {
        final EntityModel categoryModel = ((EntityApplicationModel) application).getMainApplicationModels().iterator().next();
        categoryModel.getTableModel().clearSelection();
        categoryModel.refresh();
        selectRandomRow(categoryModel.getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
      }
    };
    return Arrays.asList(scenario);
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel =
            new PetstoreAppModel(new EntityDbRemoteProvider(getUser(), "scott@"+new Object(), getClass().getSimpleName()));
    final EntityModel categoryModel = applicationModel.getMainApplicationModels().iterator().next();
    categoryModel.setLinkedDetailModels(categoryModel.getDetailModels().iterator().next());
    final EntityModel productModel = categoryModel.getDetailModels().iterator().next();
    productModel.setLinkedDetailModels(productModel.getDetailModels().iterator().next());
    final EntityModel itemModel = productModel.getDetailModels().iterator().next();
    itemModel.setLinkedDetailModels(itemModel.getDetailModels().iterator().next());

    return applicationModel;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new LoadTestPanel(new PetstoreLoadTest()).showFrame();
  }
}