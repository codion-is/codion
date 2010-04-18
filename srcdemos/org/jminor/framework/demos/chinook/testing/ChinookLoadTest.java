/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.chinook.beans.AlbumModel;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import org.jminor.framework.demos.chinook.beans.GenreModel;
import org.jminor.framework.demos.chinook.beans.InvoiceModel;
import org.jminor.framework.demos.chinook.client.ChinookAppModel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 21:32:40
 */
public class ChinookLoadTest extends EntityLoadTestModel {

  public ChinookLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  protected void loadDomainModel() {
    new Chinook();
  }

  @Override
  protected Collection<UsageScenario> initializeUsageScenarios() {
    final UsageScenario viewGenre = new UsageScenario("selectGenre") {
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel genreModel = model.getMainApplicationModel(GenreModel.class);
        selectRandomRow(genreModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 3;
      }
    };
    final UsageScenario viewInvoice = new UsageScenario("viewInvoice") {
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel customerModel = model.getMainApplicationModel(CustomerModel.class);
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(InvoiceModel.class);
        selectRandomRow(invoiceModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 2;
      }
    };
    final UsageScenario viewAlbum = new UsageScenario("viewAlbum") {
      protected void performScenario(Object application) throws Exception {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel albumModel = model.getMainApplicationModel(AlbumModel.class);
        selectRandomRow(albumModel.getTableModel());
      }

      @Override
      protected int getDefaultWeight() {
        return 5;
      }
    };

    return Arrays.asList(viewGenre, viewInvoice, viewAlbum);
  }

  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel appModel = new ChinookAppModel(User.UNIT_TEST_USER);
    appModel.refreshAll();

    EntityModel model = appModel.getMainApplicationModel(GenreModel.class);
    model.setLinkedDetailModel(model.getDetailModels().get(0));

    model = appModel.getMainApplicationModel(CustomerModel.class);
    model.setLinkedDetailModel(model.getDetailModels().get(0));

    model = model.getDetailModel(InvoiceModel.class);
    model.setLinkedDetailModel(model.getDetailModels().get(0));

    model = appModel.getMainApplicationModel(AlbumModel.class);
    model.setLinkedDetailModel(model.getDetailModels().get(0));

    return appModel;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new LoadTestPanel(new ChinookLoadTest()).showFrame();
  }
}
