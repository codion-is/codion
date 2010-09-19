/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.db.exception.DbException;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.beans.ui.*;
import org.jminor.framework.demos.chinook.domain.Chinook;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;
import org.jminor.framework.domain.Entities;

import javax.swing.JTable;
import java.awt.Dimension;
import java.util.Locale;

public final class ChinookAppPanel extends EntityApplicationPanel {

  public ChinookAppPanel() {
    /* ARTIST
    *   ALBUM
    *     TRACK
    * PLAYLIST
    *   PLAYLISTTRACK
    * CUSTOMER
    *   INVOICE
    *     INVOICELINE
    */
    final EntityPanelProvider trackProvider = new EntityPanelProvider(T_TRACK);
    trackProvider.setEditPanelClass(TrackEditPanel.class);

    final EntityPanelProvider albumProvider = new EntityPanelProvider(T_ALBUM);
    albumProvider.setEditPanelClass(AlbumEditPanel.class);
    albumProvider.addDetailPanelProvider(trackProvider);

    final EntityPanelProvider  artistProvider = new EntityPanelProvider(T_ARTIST);
    artistProvider.setEditPanelClass(ArtistEditPanel.class);
    artistProvider.addDetailPanelProvider(albumProvider);
    artistProvider.setDetailSplitPanelResizeWeight(0.3);

    final EntityPanelProvider playlistTrackProvider = new EntityPanelProvider(T_PLAYLISTTRACK);
    playlistTrackProvider.setEditPanelClass(PlaylistTrackEditPanel.class);

    final EntityPanelProvider playlistProvider = new EntityPanelProvider(Chinook.T_PLAYLIST);
    playlistProvider.setEditPanelClass(PlaylistEditPanel.class);
    playlistProvider.addDetailPanelProvider(playlistTrackProvider);
    playlistProvider.setDetailSplitPanelResizeWeight(0.3);

    final EntityPanelProvider invoiceLineProvider = new EntityPanelProvider(T_INVOICELINE) {

      @Override
      protected void configureTableModel(final EntityTableModel tableModel) {
        tableModel.setQueryConfigurationAllowed(false);
      }

      @Override
      protected void configurePanel(final EntityPanel panel) {
        panel.setIncludeControlPanel(false);
        ((InvoiceLineEditPanel) panel.getEditPanel()).setTableSearchFeld(panel.getTablePanel().getSearchField());
      }

      @Override
      protected void configureTablePanel(final EntityTablePanel tablePanel) {
        tablePanel.setIncludeSouthPanel(false);
        tablePanel.setIncludeSearchPanel(false);
        tablePanel.getJTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablePanel.setPreferredSize(new Dimension(360, 40));
        tablePanel.getTableModel().setColumnVisible(Entities.getProperty(Chinook.T_INVOICELINE, Chinook.INVOICELINE_INVOICEID_FK), false);
      }
    };
    invoiceLineProvider.setEditPanelClass(InvoiceLineEditPanel.class);

    final EntityPanelProvider invoiceProvider = new EntityPanelProvider(T_INVOICE) {

      @Override
      protected void configurePanel(final EntityPanel panel) {
        panel.setIncludeDetailPanelTabPane(false);
        final EntityPanel invoiceLinePanel = panel.getDetailPanel(Chinook.T_INVOICELINE);
        invoiceLinePanel.initializePanel();
        panel.getModel().setLinkedDetailModels(panel.getModel().getDetailModel(Chinook.T_INVOICELINE));
        ((InvoiceEditPanel) panel.getEditPanel()).setInvoiceLinePanel(invoiceLinePanel);
      }
    };
    invoiceProvider.setEditPanelClass(InvoiceEditPanel.class);
    invoiceProvider.addDetailPanelProvider(invoiceLineProvider);

    final EntityPanelProvider customerProvider = new EntityPanelProvider(T_CUSTOMER);
    customerProvider.setEditPanelClass(CustomerEditPanel.class);
    customerProvider.setTablePanelClass(CustomerTablePanel.class);
    customerProvider.addDetailPanelProvider(invoiceProvider);

    final EntityPanelProvider genreProvider = new EntityPanelProvider(T_GENRE);
    genreProvider.setEditPanelClass(GenreEditPanel.class);
    genreProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.HIDDEN);

    final EntityPanelProvider mediaTypeProvider = new EntityPanelProvider(T_MEDIATYPE);
    mediaTypeProvider.setEditPanelClass(MediaTypeEditPanel.class);
    mediaTypeProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.HIDDEN);

    final EntityPanelProvider employeeProvider = new EntityPanelProvider(T_EMPLOYEE);
    employeeProvider.setEditPanelClass(EmployeeEditPanel.class);
    employeeProvider.addDetailPanelProvider(customerProvider).setDetailPanelState(EntityPanel.HIDDEN);

    addMainApplicationPanelProviders(artistProvider, playlistProvider, customerProvider);
    addSupportPanelProviders(genreProvider, mediaTypeProvider, employeeProvider);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new ChinookApplicationModel(dbProvider);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet tools = super.getToolsControlSet();
    tools.addSeparator();
    tools.add(Controls.methodControl(getModel(), "updateInvoiceTotals", "Update invoice totals"));

    return tools;
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("EN", "en"));
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
    new ChinookAppPanel().startApplication("Chinook", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }

  public static final class ChinookApplicationModel extends DefaultEntityApplicationModel {

    public ChinookApplicationModel(final EntityDbProvider dbProvider) {
      super(dbProvider);
    }

    public void updateInvoiceTotals() throws DbException {
      getDbProvider().getEntityDb().executeProcedure(Chinook.P_UDPATE_TOTALS, null);
    }

    @Override
    protected void loadDomainModel() {
      Chinook.init();
    }
  }
}
