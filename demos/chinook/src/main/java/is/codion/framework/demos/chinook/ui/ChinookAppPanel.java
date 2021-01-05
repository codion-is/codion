/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.common.version.Version;
import is.codion.common.version.Versions;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.UiManagerDefaults;
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

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.worker.ProgressWorker.runWithProgressBar;

public final class ChinookAppPanel extends EntityApplicationPanel<ChinookApplicationModel> {

  private static final String LANGUAGE_PREFERENCES_KEY = ChinookAppPanel.class.getSimpleName() + ".language";
  private static final Locale LOCALE_IS = new Locale("is", "IS");
  private static final Locale LOCALE_EN = new Locale("en", "EN");
  private static final String LANGUAGE_IS = "is";
  private static final String LANGUAGE_EN = "en";

  private static final String SELECT_LANGUAGE = "select_language";
  private static final String UPDATE_TOTALS = "update_totals";
  private static final String UPDATING_TOTALS = "updating_totals";
  private static final String UPDATING_TOTALS_FAILED = "updating_totals_failed";
  private static final String TOTALS_UPDATED = "totals_updated";

  /* Non-static so this is not initialized before main(), which sets the locale */
  private final ResourceBundle bundle = ResourceBundle.getBundle(ChinookAppPanel.class.getName());

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
    final EntityPanelBuilder trackBuilder = new EntityPanelBuilder(Track.TYPE)
            .editPanelClass(TrackEditPanel.class)
            .tablePanelClass(TrackTablePanel.class);

    final EntityPanelBuilder customerBuilder = new EntityPanelBuilder(Customer.TYPE)
            .editPanelClass(CustomerEditPanel.class)
            .tablePanelClass(CustomerTablePanel.class);

    final EntityPanelBuilder genreBuilder = new EntityPanelBuilder(Genre.TYPE)
            .editPanelClass(GenreEditPanel.class)
            .detailPanelBuilder(trackBuilder)
            .detailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelBuilder mediaTypeBuilder = new EntityPanelBuilder(MediaType.TYPE)
            .editPanelClass(MediaTypeEditPanel.class)
            .detailPanelBuilder(trackBuilder)
            .detailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanelBuilder employeeBuilder = new EntityPanelBuilder(Employee.TYPE)
            .editPanelClass(EmployeeEditPanel.class)
            .detailPanelBuilder(customerBuilder).detailPanelState(EntityPanel.PanelState.HIDDEN);

    addSupportPanelBuilders(genreBuilder, mediaTypeBuilder, employeeBuilder);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final ChinookApplicationModel applicationModel) {
    final List<EntityPanel> panels = new ArrayList<>();

    final SwingEntityModel customerModel = applicationModel.getEntityModel(Customer.TYPE);
    final EntityPanel customerPanel = new EntityPanel(customerModel, new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    final EntityPanel invoicePanel = new EntityPanel(invoiceModel, new InvoiceEditPanel(invoiceModel.getEditModel()));
    invoicePanel.setIncludeDetailPanelTabPane(false);
    invoicePanel.setShowDetailPanelControls(false);

    final SwingEntityModel invoiceLineModel = invoiceModel.getDetailModel(InvoiceLine.TYPE);
    final EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel, new InvoiceLineEditPanel(invoiceLineModel.getEditModel()));
    final EntityTablePanel invoiceLineTablePanel = invoiceLinePanel.getTablePanel();
    invoiceLineTablePanel.setIncludeSouthPanel(false);
    invoiceLineTablePanel.setIncludeConditionPanel(false);
    invoiceLineTablePanel.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    invoiceLineTablePanel.setPreferredSize(new Dimension(360, 40));
    invoiceLineTablePanel.getTable().getModel().getColumnModel().hideColumn(
            getModel().getEntities().getDefinition(InvoiceLine.TYPE).getProperty(InvoiceLine.INVOICE_FK));
    invoiceLinePanel.setIncludeControlPanel(false);
    ((InvoiceLineEditPanel) invoiceLinePanel.getEditPanel()).setTableSearchFeld(invoiceLinePanel.getTablePanel().getTable().getSearchField());
    invoiceLinePanel.initializePanel();
    ((InvoiceEditPanel) invoicePanel.getEditPanel()).setInvoiceLinePanel(invoiceLinePanel);

    invoicePanel.addDetailPanel(invoiceLinePanel);
    customerPanel.addDetailPanel(invoicePanel);
    panels.add(customerPanel);

    final SwingEntityModel artistModel = applicationModel.getEntityModel(Artist.TYPE);
    final EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
    final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
    final SwingEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);
    final EntityPanel trackPanel = new EntityPanel(trackModel,
            new TrackEditPanel(trackModel.getEditModel()), new TrackTablePanel(trackModel.getTableModel()));

    albumPanel.addDetailPanel(trackPanel);
    artistPanel.addDetailPanel(albumPanel);
    panels.add(artistPanel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(Playlist.TYPE);
    final EntityPanel playlistPanel = new EntityPanel(playlistModel, new PlaylistEditPanel(playlistModel.getEditModel()));
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
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
  protected ControlList getViewControls() {
    final ControlList view = super.getViewControls();
    view.addSeparator();
    view.add(Controls.control(this::selectLanguage, bundle.getString(SELECT_LANGUAGE)));

    return view;
  }

  @Override
  protected ControlList getToolsControls() {
    final ControlList tools = super.getToolsControls();
    tools.addSeparator();
    tools.add(Controls.control(this::updateInvoiceTotals, bundle.getString(UPDATE_TOTALS)));

    return tools;
  }

  private void updateInvoiceTotals() {
    runWithProgressBar(this, getModel()::updateInvoiceTotals, bundle.getString(UPDATING_TOTALS),
            bundle.getString(TOTALS_UPDATED), bundle.getString(UPDATING_TOTALS_FAILED));
  }

  private void selectLanguage() {
    final String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    final JRadioButton enButton = new JRadioButton("English");
    final JRadioButton isButton = new JRadioButton("Íslenska");
    final ButtonGroup langButtonGroup = new ButtonGroup();
    langButtonGroup.add(enButton);
    langButtonGroup.add(isButton);
    final JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    buttonPanel.add(enButton);
    buttonPanel.add(isButton);
    enButton.setSelected(language.equals(LANGUAGE_EN));
    isButton.setSelected(language.equals(LANGUAGE_IS));
    JOptionPane.showMessageDialog(this, buttonPanel, "Language/Tungumál", JOptionPane.QUESTION_MESSAGE);
    final String newLanguage = isButton.isSelected() ? LANGUAGE_IS : LANGUAGE_EN;
    if (!language.equals(newLanguage)) {
      UserPreferences.putUserPreference(LANGUAGE_PREFERENCES_KEY, newLanguage);
      JOptionPane.showMessageDialog(this,
              "Language has been changed, restart the application to apply the changes.\n\n" +
                      "Tungumáli hefur verið breytt, endurræstu kerfið til að virkja breytingar");
    }
  }

  public static void main(final String[] args) throws CancelException {
    final String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    Locale.setDefault(LANGUAGE_IS.equals(language) ? LOCALE_IS : LOCALE_EN);
    UiManagerDefaults.initialize();
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
