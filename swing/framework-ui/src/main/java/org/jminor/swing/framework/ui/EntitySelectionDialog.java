/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.jminor.swing.common.ui.UiUtil.*;
import static org.jminor.swing.common.ui.control.Controls.control;

/**
 * A dialog for searching for and selecting one or more entities from a table model.
 */
public final class EntitySelectionDialog extends JDialog {

  private final List<Entity> selectedEntities = new ArrayList<>();
  private final SwingEntityTableModel tableModel;
  private final EntityTablePanel entityTablePanel;

  private final Control okControl = control(this::ok,
          Messages.get(Messages.OK), null, null,
          Messages.get(Messages.OK_MNEMONIC).charAt(0));
  private final Control cancelControl = control(this::dispose,
          Messages.get(Messages.CANCEL), null, null,
          Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
  private final Control searchControl = control(this::search,
          FrameworkMessages.get(FrameworkMessages.SEARCH), null, null,
          FrameworkMessages.get(FrameworkMessages.SEARCH_MNEMONIC).charAt(0));

  /**
   * Instantiates a JDialog for searching for and selecting one or more entities.
   * @param tableModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param dialogTitle the dialog title
   * @param preferredSize the preferred size of the dialog, may be null
   */
  public EntitySelectionDialog(final SwingEntityTableModel tableModel, final Container dialogOwner,
                               final String dialogTitle, final Dimension preferredSize) {
    super(dialogOwner instanceof Window ? (Window) dialogOwner : getParentWindow(dialogOwner), dialogTitle);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    this.tableModel = requireNonNull(tableModel, "tableModel");
    if (tableModel.getEditModel() != null) {
      tableModel.getEditModel().setReadOnly(true);
    }
    this.entityTablePanel = initializeTablePanel(tableModel, preferredSize);
    addKeyEvent(getRootPane(), KeyEvent.VK_ESCAPE, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelControl);
    setLayout(new BorderLayout());
    final JPanel buttonPanel = new JPanel(createFlowLayout(FlowLayout.RIGHT));
    final JButton okButton = new JButton(okControl);
    buttonPanel.add(okButton);
    buttonPanel.add(new JButton(cancelControl));
    buttonPanel.add(new JButton(searchControl));
    getRootPane().setDefaultButton(okButton);
    add(entityTablePanel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    pack();
    setLocationRelativeTo(dialogOwner);
    setModal(true);
    setResizable(true);
  }

  public void setSingleSelection(final boolean singleSelection) {
    entityTablePanel.getTable().setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION);
  }

  /**
   * Displays this dialog.
   * @return the selected entities
   * @throws CancelException in case no entities were selected
   */
  public List<Entity> selectEntities() {
    setVisible(true);
    if (selectedEntities.isEmpty()) {
      throw new CancelException();
    }

    return selectedEntities;
  }

  private EntityTablePanel initializeTablePanel(final SwingEntityTableModel tableModel, final Dimension preferredSize) {
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
    tablePanel.initializePanel();
    tablePanel.getTable().addDoubleClickListener(mouseEvent -> {
      if (!tableModel.getSelectionModel().isSelectionEmpty()) {
        okControl.actionPerformed(null);
      }
    });
    tablePanel.setConditionPanelVisible(true);
    tablePanel.getTable().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
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
      JOptionPane.showMessageDialog(getParentWindow(entityTablePanel),
              FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
    }
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param tableModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @return a List containing the selected entities
   * @throws CancelException in case the user cancels the operation
   */
  public static List<Entity> selectEntities(final SwingEntityTableModel tableModel, final Container dialogOwner,
                                            final boolean singleSelection, final String dialogTitle) {
    return selectEntities(tableModel, dialogOwner, singleSelection, dialogTitle, null);
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param tableModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @param preferredSize the preferred size of the dialog
   * @return a List containing the selected entities
   * @throws CancelException in case the user cancels the operation or selects no entities
   */
  public static List<Entity> selectEntities(final SwingEntityTableModel tableModel, final Container dialogOwner,
                                            final boolean singleSelection, final String dialogTitle,
                                            final Dimension preferredSize) {
    final EntitySelectionDialog dialog = new EntitySelectionDialog(tableModel, dialogOwner, dialogTitle, preferredSize);
    dialog.setSingleSelection(singleSelection);

    return dialog.selectEntities();
  }
}
