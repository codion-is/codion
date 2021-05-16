/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityInputComponents;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icons.FrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.Components.addLookAndFeelProvider;
import static is.codion.swing.common.ui.Components.lookAndFeelProvider;
import static javax.swing.JOptionPane.showMessageDialog;

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

  public ChinookAppPanel() {
    super("Chinook");
  }

  @Override
  protected List<EntityPanel.Builder> initializeSupportEntityPanelBuilders(final ChinookApplicationModel applicationModel) {
    final EntityPanel.Builder trackBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Track.TYPE))
                    .editPanelClass(TrackEditPanel.class)
                    .tablePanelClass(TrackTablePanel.class);

    final EntityPanel.Builder customerBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Customer.TYPE))
                    .editPanelClass(CustomerEditPanel.class)
                    .tablePanelClass(CustomerTablePanel.class);

    final EntityPanel.Builder genreBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Genre.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(Track.TYPE)))
                    .editPanelClass(GenreEditPanel.class)
                    .detailPanelBuilder(trackBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanel.Builder mediaTypeBuilder =
            EntityPanel.builder(SwingEntityModel.builder(MediaType.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(Track.TYPE)))
                    .editPanelClass(MediaTypeEditPanel.class)
                    .detailPanelBuilder(trackBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN);

    final EntityPanel.Builder employeeBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Employee.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(Customer.TYPE))
                    .tableModelClass(EmployeeTableModel.class))
                    .editPanelClass(EmployeeEditPanel.class)
                    .tablePanelClass(EmployeeTablePanel.class)
                    .detailPanelBuilder(customerBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN);

    return Arrays.asList(genreBuilder, mediaTypeBuilder, employeeBuilder);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final ChinookApplicationModel applicationModel) {
    final SwingEntityModel customerModel = applicationModel.getEntityModel(Customer.TYPE);
    final EntityPanel customerPanel = new EntityPanel(customerModel, new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    final EntityPanel invoicePanel = new EntityPanel(invoiceModel, new InvoiceEditPanel(invoiceModel.getEditModel()));
    invoicePanel.setIncludeDetailPanelTabPane(false);
    invoicePanel.setShowDetailPanelControls(false);

    final SwingEntityModel invoiceLineModel = invoiceModel.getDetailModel(InvoiceLine.TYPE);
    final InvoiceLineTablePanel invoiceLineTablePanel = new InvoiceLineTablePanel(invoiceLineModel.getTableModel());
    final InvoiceLineEditPanel invoiceLineEditPanel = new InvoiceLineEditPanel(invoiceLineModel.getEditModel(),
            invoiceLineTablePanel.getTable().getSearchField());
    final EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel, invoiceLineEditPanel, invoiceLineTablePanel);
    invoiceLinePanel.setIncludeControlPanel(false);
    invoiceLinePanel.initializePanel();
    ((InvoiceEditPanel) invoicePanel.getEditPanel()).setInvoiceLinePanel(invoiceLinePanel);

    invoicePanel.addDetailPanel(invoiceLinePanel);
    customerPanel.addDetailPanel(invoicePanel);

    final SwingEntityModel artistModel = applicationModel.getEntityModel(Artist.TYPE);
    final EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
    final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
    final SwingEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);
    final EntityPanel trackPanel = new EntityPanel(trackModel,
            new TrackEditPanel(trackModel.getEditModel()),
            new TrackTablePanel(trackModel.getTableModel()));

    albumPanel.addDetailPanel(trackPanel);
    artistPanel.addDetailPanel(albumPanel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(Playlist.TYPE);
    final EntityPanel playlistPanel = new EntityPanel(playlistModel, new PlaylistEditPanel(playlistModel.getEditModel()));
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    final EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel,
            new PlaylistTrackEditPanel(playlistTrackModel.getEditModel()),
            new PlaylistTrackTablePanel(playlistTrackModel.getTableModel()));

    playlistPanel.addDetailPanel(playlistTrackPanel);

    return Arrays.asList(customerPanel, artistPanel, playlistPanel);
  }

  @Override
  protected ChinookApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new ChinookApplicationModel(connectionProvider);
  }

  @Override
  protected Version getClientVersion() {
    return Version.version(0, 1, 0);
  }

  @Override
  protected Controls getViewControls() {
    return super.getViewControls()
            .addSeparator()
            .add(Control.builder()
                    .command(this::selectLanguage)
                    .name(bundle.getString(SELECT_LANGUAGE))
                    .build());
  }

  @Override
  protected Controls getToolsControls() {
    return super.getToolsControls()
            .addSeparator()
            .add(Control.builder()
                    .command(this::updateInvoiceTotals)
                    .name(bundle.getString(UPDATE_TOTALS))
                    .build());
  }

  private void updateInvoiceTotals() {
    ProgressWorker.<List<Entity>>builder()
            .owner(this)
            .title(bundle.getString(UPDATING_TOTALS))
            .task(getModel()::updateInvoiceTotals)
            .onSuccess(this::handleUpdateTotalsSuccess)
            .onException(this::handleUpdateTotalsException)
            .build().execute();
  }

  private void handleUpdateTotalsSuccess(final List<Entity> updatedInvoices) {
    getModel().getEntityModel(Customer.TYPE).getDetailModel(Invoice.TYPE)
            .getTableModel().replaceEntities(updatedInvoices);
    showMessageDialog(Windows.getParentWindow(this), bundle.getString(TOTALS_UPDATED));
  }

  private void handleUpdateTotalsException(final Throwable exception) {
    Dialogs.exceptionDialogBuilder()
            .owner(Windows.getParentWindow(this))
            .title(bundle.getString(UPDATING_TOTALS_FAILED))
            .show(exception);
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
    showMessageDialog(this, buttonPanel, "Language/Tungumál", JOptionPane.QUESTION_MESSAGE);
    final String newLanguage = isButton.isSelected() ? LANGUAGE_IS : LANGUAGE_EN;
    if (!language.equals(newLanguage)) {
      UserPreferences.putUserPreference(LANGUAGE_PREFERENCES_KEY, newLanguage);
      showMessageDialog(this,
              "Language has been changed, restart the application to apply the changes.\n\n" +
                      "Tungumáli hefur verið breytt, endurræstu kerfið til að virkja breytingar");
    }
  }

  public static void main(final String[] args) throws CancelException {
    final String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    Locale.setDefault(LANGUAGE_IS.equals(language) ? LOCALE_IS : LOCALE_EN);
    addLookAndFeelProvider(lookAndFeelProvider(FlatLightLaf.class.getName(), () -> {
      FlatLightLaf.install();
      final Color background = (Color) UIManager.get("Table.background");
      UIManager.put("Table.alternateRowColor", background.darker());
    }));
    addLookAndFeelProvider(lookAndFeelProvider(FlatIntelliJLaf.class.getName(), () -> {
      FlatIntelliJLaf.install();
      final Color background = (Color) UIManager.get("Table.background");
      UIManager.put("Table.alternateRowColor", background.darker());
    }));
    addLookAndFeelProvider(lookAndFeelProvider(FlatDarkLaf.class.getName(), () -> {
      FlatDarkLaf.install();
      final Color background = (Color) UIManager.get("Table.background");
      UIManager.put("Table.alternateRowColor", background.brighter());
    }));
    addLookAndFeelProvider(lookAndFeelProvider(FlatDarculaLaf.class.getName(), () -> {
      FlatDarculaLaf.install();
      final Color background = (Color) UIManager.get("Table.background");
      UIManager.put("Table.alternateRowColor", background.brighter());
    }));
    Icons.ICONS_CLASSNAME.set(IkonliFoundationIcons.class.getName());
    FrameworkIcons.FRAMEWORK_ICONS_CLASSNAME.set(IkonliFoundationFrameworkIcons.class.getName());
    EntityInputComponents.COMBO_BOX_COMPLETION_MODE.set(EntityInputComponents.COMPLETION_MODE_AUTOCOMPLETE);
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.chinook.domain.impl.ChinookImpl");
    SwingUtilities.invokeLater(() -> new ChinookAppPanel().starter()
            .frameSize(new Dimension(1280, 720))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());

  }
}
