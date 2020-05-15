/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.common.version.Version;
import is.codion.common.version.Versions;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icons.FrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.worker.ProgressWorker.runWithProgressBar;

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

    final SwingEntityModel customerModel = applicationModel.getEntityModel(T_CUSTOMER);
    final EntityPanel customerPanel = new EntityPanel(customerModel, new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
    final EntityPanel invoicePanel = new EntityPanel(invoiceModel, new InvoiceEditPanel(invoiceModel.getEditModel()));
    invoicePanel.setIncludeDetailPanelTabPane(false);
    invoicePanel.setShowDetailPanelControls(false);

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
  protected ControlList getToolsControls() {
    final ControlList tools = super.getToolsControls();
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
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.chinook.domain.impl.ChinookImpl");
    new ChinookAppPanel().startApplication("Chinook", null, MaximizeFrame.NO,
            new Dimension(1280, 720), Users.parseUser("scott:tiger"));
  }
}
