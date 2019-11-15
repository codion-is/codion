/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client.ui;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.beans.ui.AlbumEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.ArtistEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.CustomerEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.CustomerTablePanel;
import org.jminor.framework.demos.chinook.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.GenreEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.InvoiceEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.InvoiceLineEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.MediaTypeEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.PlaylistEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.PlaylistTrackEditPanel;
import org.jminor.framework.demos.chinook.beans.ui.TrackEditPanel;
import org.jminor.framework.demos.chinook.client.ChinookApplicationModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookAppPanel extends EntityApplicationPanel<ChinookApplicationModel> {

  /* ARTIST
   *   ALBUM
   *     TRACK
   * PLAYLIST
   *   PLAYLISTTRACK
   * CUSTOMER
   *   INVOICE
   *     INVOICELINE
   */
  @Override
  protected void setupEntityPanelProviders() {
    final EntityPanelProvider trackProvider = new EntityPanelProvider(T_TRACK);
    trackProvider.setEditPanelClass(TrackEditPanel.class);

    final EntityPanelProvider customerProvider = new EntityPanelProvider(T_CUSTOMER);
    customerProvider.setEditPanelClass(CustomerEditPanel.class);
    customerProvider.setTablePanelClass(CustomerTablePanel.class);

    final EntityPanelProvider genreProvider = new EntityPanelProvider(T_GENRE);
    genreProvider.setEditPanelClass(GenreEditPanel.class);
    genreProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelProvider mediaTypeProvider = new EntityPanelProvider(T_MEDIATYPE);
    mediaTypeProvider.setEditPanelClass(MediaTypeEditPanel.class);
    mediaTypeProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelProvider employeeProvider = new EntityPanelProvider(T_EMPLOYEE);
    employeeProvider.setEditPanelClass(EmployeeEditPanel.class);
    employeeProvider.addDetailPanelProvider(customerProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    addSupportPanelProviders(genreProvider, mediaTypeProvider, employeeProvider);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final ChinookApplicationModel applicationModel) {
    final List<EntityPanel> panels = new ArrayList<>();

    final SwingEntityModel artistModel = applicationModel.getEntityModel(T_ARTIST);
    final EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
    final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
    final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
    final SwingEntityModel trackModel = albumModel.getDetailModel(T_TRACK);
    final EntityPanel trackPanel = new EntityPanel(trackModel, new TrackEditPanel(trackModel.getEditModel()));

    albumPanel.addDetailPanel(trackPanel);
    artistPanel.addDetailPanel(albumPanel);
    panels.add(artistPanel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(T_PLAYLIST);
    final EntityPanel playlistPanel = new EntityPanel(playlistModel, new PlaylistEditPanel(playlistModel.getEditModel()));
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(T_PLAYLISTTRACK);
    final EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel, new PlaylistTrackEditPanel(playlistTrackModel.getEditModel()));

    playlistPanel.addDetailPanel(playlistTrackPanel);
    panels.add(playlistPanel);

    final SwingEntityModel customerModel = applicationModel.getEntityModel(T_CUSTOMER);
    final EntityPanel customerPanel = new EntityPanel(customerModel, new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
    final EntityPanel invoicePanel = new EntityPanel(invoiceModel, new InvoiceEditPanel(invoiceModel.getEditModel()));
    invoicePanel.setIncludeDetailPanelTabPane(false);

    final SwingEntityModel invoiceLineModel = invoiceModel.getDetailModel(T_INVOICELINE);
    final EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel, new InvoiceLineEditPanel(invoiceLineModel.getEditModel()));
    final EntityTablePanel invoiceLineTablePanel = invoiceLinePanel.getTablePanel();
    invoiceLineTablePanel.setIncludeSouthPanel(false);
    invoiceLineTablePanel.setIncludeConditionPanel(false);
    invoiceLineTablePanel.getJTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    invoiceLineTablePanel.setPreferredSize(new Dimension(360, 40));
    invoiceLineTablePanel.getTableModel().getColumnModel().setColumnVisible(
            getModel().getDomain().getDefinition(T_INVOICELINE).getProperty(INVOICELINE_INVOICE_FK), false);
    invoiceLinePanel.setIncludeControlPanel(false);
    ((InvoiceLineEditPanel) invoiceLinePanel.getEditPanel()).setTableSearchFeld(invoiceLinePanel.getTablePanel().getSearchField());
    invoiceLinePanel.initializePanel();
    ((InvoiceEditPanel) invoicePanel.getEditPanel()).setInvoiceLinePanel(invoiceLinePanel);

    invoicePanel.addDetailPanel(invoiceLinePanel);
    customerPanel.addDetailPanel(invoicePanel);
    panels.add(customerPanel);

    return panels;
  }

  @Override
  protected ChinookApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new ChinookApplicationModel(connectionProvider);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet tools = super.getToolsControlSet();
    tools.addSeparator();
    tools.add(Controls.control(getModel()::updateInvoiceTotals, "Update invoice totals"));

    return tools;
  }

  @Override
  protected Version getClientVersion() {
    return new Version(0, 1, 0);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    EntityTablePanel.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(EntityTablePanel.ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.chinook.domain.impl.ChinookImpl");
    new ChinookAppPanel().startApplication("Chinook", null, false,
            UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger".toCharArray()));
  }
}
