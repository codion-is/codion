/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.user.Users;
import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.model.ChinookApplicationModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.icons.Icons;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.ReferentialIntegrityErrorHandling;
import org.jminor.swing.framework.ui.icons.FrameworkIcons;
import org.jminor.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import org.jminor.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;
import static org.jminor.swing.common.ui.worker.ProgressWorker.runWithProgressBar;

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
  protected void setupEntityPanelBuilders() {
    final EntityPanelBuilder trackBuilder = new EntityPanelBuilder(T_TRACK);
    trackBuilder.setEditPanelClass(TrackEditPanel.class).setTablePanelClass(TrackTablePanel.class);

    final EntityPanelBuilder customerBuilder = new EntityPanelBuilder(T_CUSTOMER);
    customerBuilder.setEditPanelClass(CustomerEditPanel.class);
    customerBuilder.setTablePanelClass(CustomerTablePanel.class);

    final EntityPanelBuilder genreBuilder = new EntityPanelBuilder(T_GENRE);
    genreBuilder.setEditPanelClass(GenreEditPanel.class);
    genreBuilder.addDetailPanelBuilder(trackBuilder).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelBuilder mediaTypeBuilder = new EntityPanelBuilder(T_MEDIATYPE);
    mediaTypeBuilder.setEditPanelClass(MediaTypeEditPanel.class);
    mediaTypeBuilder.addDetailPanelBuilder(trackBuilder).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelBuilder employeeBuilder = new EntityPanelBuilder(T_EMPLOYEE);
    employeeBuilder.setEditPanelClass(EmployeeEditPanel.class);
    employeeBuilder.addDetailPanelBuilder(customerBuilder).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    addSupportPanelBuilders(genreBuilder, mediaTypeBuilder, employeeBuilder);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final ChinookApplicationModel applicationModel) {
    final List<EntityPanel> panels = new ArrayList<>();

    final SwingEntityModel artistModel = applicationModel.getEntityModel(T_ARTIST);
    final EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
    final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
    final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
    final SwingEntityModel trackModel = albumModel.getDetailModel(T_TRACK);
    final EntityPanel trackPanel = new EntityPanel(trackModel,
            new TrackEditPanel(trackModel.getEditModel()), new TrackTablePanel(trackModel.getTableModel()));

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
    invoiceLineTablePanel.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    invoiceLineTablePanel.setPreferredSize(new Dimension(360, 40));
    invoiceLineTablePanel.getTable().getModel().getColumnModel().hideColumn(
            getModel().getEntities().getDefinition(T_INVOICELINE).getProperty(INVOICELINE_INVOICE_FK));
    invoiceLinePanel.setIncludeControlPanel(false);
    ((InvoiceLineEditPanel) invoiceLinePanel.getEditPanel()).setTableSearchFeld(invoiceLinePanel.getTablePanel().getTable().getSearchField());
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
  protected Version getClientVersion() {
    return Versions.version(0, 1, 0);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet tools = super.getToolsControlSet();
    tools.addSeparator();
    tools.add(Controls.control(this::updateInvoiceTotals, "Update invoice totals"));

    return tools;
  }

  private void updateInvoiceTotals() {
    runWithProgressBar(this, "Updating totals...",
            "Totals updated", "Updating totals failed",
            getModel()::updateInvoiceTotals);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    Icons.ICONS_CLASSNAME.set(IkonliFoundationIcons.class.getName());
    FrameworkIcons.FRAMEWORK_ICONS_CLASSNAME.set(IkonliFoundationFrameworkIcons.class.getName());
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.chinook.domain.impl.ChinookImpl");
    new ChinookAppPanel().startApplication("Chinook", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
