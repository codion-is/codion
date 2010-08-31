/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.common.db.exception.DbException;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class ChinookLoadTest extends EntityLoadTestModel {

  public ChinookLoadTest() {
    super(User.UNIT_TEST_USER, new LoadTestModel.AbstractUsageScenario("viewGenre") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel genreModel = model.getMainApplicationModel(Chinook.T_GENRE);
        selectRandomRow(genreModel.getTableModel());
        final EntityModel trackModel = genreModel.getDetailModel(Chinook.T_TRACK);
        selectRandomRows(trackModel.getTableModel(), 2);
        try {
          genreModel.getDbProvider().getEntityDb().selectDependentEntities(trackModel.getTableModel().getSelectedItems());
        }
        catch (DbException e) {
          throw new ScenarioException(e);
        }
      }

      @Override
      public int getDefaultWeight() {
        return 3;
      }
    }, new LoadTestModel.AbstractUsageScenario("viewInvoice") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel customerModel = model.getMainApplicationModel(Chinook.T_CUSTOMER);
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
        selectRandomRow(invoiceModel.getTableModel());
      }

      @Override
      public int getDefaultWeight() {
        return 2;
      }
    }, new LoadTestModel.AbstractUsageScenario("viewAlbum") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel artistModel = model.getMainApplicationModel(Chinook.T_ARTIST);
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
        selectRandomRow(albumModel.getTableModel());
      }

      @Override
      public int getDefaultWeight() {
        return 5;
      }
    });
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel appModel = new DefaultEntityApplicationModel(
            EntityDbProviderFactory.createEntityDbProvider(getUser(), ChinookLoadTest.class.getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        Chinook.init();
      }
    };
    /* ARTIST
    *   ALBUM
    *     TRACK
    * PLAYLIST
    *   PLAYLISTTRACK
    * CUSTOMER
    *   INVOICE
    *     INVOICELINE
    */
    final EntityModel artistModel = appModel.getMainApplicationModel(Chinook.T_ARTIST);
    final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
    final EntityModel trackModel = albumModel.getDetailModel(Chinook.T_TRACK);
    artistModel.setLinkedDetailModels(albumModel);
    albumModel.setLinkedDetailModels(trackModel);

    final EntityModel genreModel = appModel.getMainApplicationModel(Chinook.T_GENRE);
    final EntityModel genreTrackModel = genreModel.getDetailModel(Chinook.T_TRACK);
    genreModel.setLinkedDetailModels(genreTrackModel);

    final EntityModel playlistModel = appModel.getMainApplicationModel(Chinook.T_PLAYLIST);
    final EntityModel playlistTrackModel = playlistModel.getDetailModel(Chinook.T_PLAYLISTTRACK);
    playlistModel.setLinkedDetailModels(playlistTrackModel);

    final EntityModel customerModel = appModel.getMainApplicationModel(Chinook.T_CUSTOMER);
    final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    final EntityModel invoicelineModel = invoiceModel.getDetailModel(Chinook.T_INVOICELINE);
    customerModel.setLinkedDetailModels(invoiceModel);
    invoiceModel.setLinkedDetailModels(invoicelineModel);
    try {
      appModel.refresh();
    }
    catch (Exception e) {}

    return appModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    public void run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LoadTestPanel(new ChinookLoadTest()).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
