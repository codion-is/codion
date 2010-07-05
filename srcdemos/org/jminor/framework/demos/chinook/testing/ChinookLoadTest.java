/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;

public class ChinookLoadTest extends EntityLoadTestModel {

  public ChinookLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  @Override
  protected void loadDomainModel() {
    new Chinook();
  }

  @Override
  protected Collection<UsageScenario> initializeUsageScenarios() {
    final UsageScenario viewGenre = new UsageScenario("viewGenre") {
      @Override
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel genreModel = new DefaultEntityModel(Chinook.T_GENRE, model.getDbProvider());
        genreModel.setLinkedDetailModels(genreModel.getDetailModels().iterator().next());
        genreModel.refresh();
        selectRandomRow(genreModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 3;
      }
    };
    final UsageScenario viewInvoice = new UsageScenario("viewInvoice") {
      @Override
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel customerModel = model.getMainApplicationModel(Chinook.T_CUSTOMER);
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
        selectRandomRow(invoiceModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 2;
      }
    };
    final UsageScenario viewAlbum = new UsageScenario("viewAlbum") {
      @Override
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel artistModel = model.getMainApplicationModel(Chinook.T_ARTIST);
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
        selectRandomRow(albumModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 5;
      }
    };

    return Arrays.asList(viewGenre, viewInvoice, viewAlbum);
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel appModel = new EntityApplicationModel(EntityDbProviderFactory.createEntityDbProvider(getUser(), ChinookLoadTest.class.getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        new Chinook();
      }
    };
    appModel.refresh();

    EntityModel model = appModel.getMainApplicationModel(Chinook.T_ARTIST);
    model.setLinkedDetailModels(model.getDetailModels().iterator().next());

    model = appModel.getMainApplicationModel(Chinook.T_CUSTOMER);
    model.setLinkedDetailModels(model.getDetailModels().iterator().next());
    model = model.getDetailModel(Chinook.T_INVOICE);
    model.setLinkedDetailModels(model.getDetailModels().iterator().next());

    model = appModel.getMainApplicationModel(Chinook.T_PLAYLIST);
    model.setLinkedDetailModels(model.getDetailModels().iterator().next());

    return appModel;
  }

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new LoadTestPanel(new ChinookLoadTest()).showFrame();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
