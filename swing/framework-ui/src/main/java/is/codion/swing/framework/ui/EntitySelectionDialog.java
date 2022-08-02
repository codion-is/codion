/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.value.ValueObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.AbstractDialogBuilder;
import is.codion.swing.common.ui.dialog.DialogBuilder;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.Utilities.getParentWindow;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * A dialog for searching for and selecting one or more entities from a table model.
 * @see #builder(SwingEntityTableModel)
 */
public final class EntitySelectionDialog extends JDialog {

  private final List<Entity> selectedEntities = new ArrayList<>();
  private final SwingEntityTableModel tableModel;
  private final EntityTablePanel entityTablePanel;

  private final Control okControl = Control.builder(this::ok)
          .caption(Messages.ok())
          .mnemonic(Messages.okMnemonic())
          .build();
  private final Control cancelControl = Control.builder(this::dispose)
          .caption(Messages.cancel())
          .mnemonic(Messages.cancelMnemonic())
          .build();
  private final Control searchControl = Control.builder(this::search)
          .caption(FrameworkMessages.search())
          .mnemonic(FrameworkMessages.searchMnemonic())
          .build();

  private EntitySelectionDialog(SwingEntityTableModel tableModel, Window owner, ValueObserver<String> titleObserver,
                                ImageIcon icon, Dimension preferredSize, boolean singleSelection) {
    super(owner, titleObserver == null ? null : titleObserver.get());
    if (titleObserver != null) {
      titleObserver.addDataListener(this::setTitle);
    }
    if (icon != null) {
      setIconImage(icon.getImage());
    }
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.tableModel.getEditModel().setReadOnly(true);
    this.entityTablePanel = initializeTablePanel(tableModel, preferredSize, singleSelection);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(cancelControl)
            .enable(getRootPane());
    JPanel buttonPanel = new JPanel(flowLayout(FlowLayout.RIGHT));
    JButton okButton = okControl.createButton();
    buttonPanel.add(okButton);
    buttonPanel.add(cancelControl.createButton());
    buttonPanel.add(searchControl.createButton());
    getRootPane().setDefaultButton(okButton);
    setLayout(new BorderLayout());
    add(entityTablePanel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    pack();
    setLocationRelativeTo(owner);
    setModal(true);
    setResizable(true);
  }

  /**
   * Creates a new {@link Builder} instance.
   * @param tableModel the table model on which to base the table panel
   * @return a new builder instance
   */
  public static Builder builder(SwingEntityTableModel tableModel) {
    return new DefaultBuilder(requireNonNull(tableModel));
  }

  /**
   * A builder for {@link EntitySelectionDialog}.
   */
  public interface Builder extends DialogBuilder<Builder> {

    /**
     * @param preferredSize the preferred dialog size
     * @return this builder instance
     */
    Builder preferredSize(Dimension preferredSize);

    /**
     * @return a List containing the selected entities
     * @throws CancelException in case the user cancels the operation
     */
    List<Entity> select();

    /**
     * Displays an entity table in a dialog for selecting a single entity
     * @return the selected entity or {@link Optional#empty()} if none was selected
     */
    Optional<Entity> selectSingle();
  }

  private EntityTablePanel initializeTablePanel(SwingEntityTableModel tableModel, Dimension preferredSize,
                                                boolean singleSelection) {
    EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
    tablePanel.initializePanel();
    tablePanel.getTable().addDoubleClickListener(mouseEvent -> {
      if (!tableModel.getSelectionModel().isSelectionEmpty()) {
        okControl.actionPerformed(null);
      }
    });
    tablePanel.setConditionPanelVisible(true);
    tablePanel.getTable().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    tablePanel.getTable().setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION);
    if (preferredSize != null) {
      tablePanel.setPreferredSize(preferredSize);
    }


    return tablePanel;
  }

  private void ok() {
    selectedEntities.addAll(tableModel.getSelectionModel().getSelectedItems());
    dispose();
  }

  private void search() {
    tableModel.refresh();
    if (tableModel.getRowCount() > 0) {
      tableModel.getSelectionModel().setSelectedIndexes(singletonList(0));
      entityTablePanel.getTable().requestFocusInWindow();
    }
    else {
      JOptionPane.showMessageDialog(getParentWindow(entityTablePanel).orElse(null),
              FrameworkMessages.noResultsFromCondition());
    }
  }

  private List<Entity> selectEntities() {
    setVisible(true);

    return selectedEntities;
  }

  private static final class DefaultBuilder extends AbstractDialogBuilder<Builder> implements Builder {

    private final SwingEntityTableModel tableModel;

    private Dimension preferredSize;

    private DefaultBuilder(SwingEntityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public EntitySelectionDialog.Builder preferredSize(Dimension preferredSize) {
      this.preferredSize = requireNonNull(preferredSize);
      return this;
    }

    @Override
    public List<Entity> select() {
      return new EntitySelectionDialog(tableModel, owner, titleProvider, icon, preferredSize, false).selectEntities();
    }

    @Override
    public Optional<Entity> selectSingle() {
      List<Entity> entities = new EntitySelectionDialog(tableModel, owner, titleProvider, icon, preferredSize, true).selectEntities();

      return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }
  }
}
