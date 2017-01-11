/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.model.table.ColumnSummaryModel;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A panel that shows a summary value for a numerical column property in a EntityTableModel.
 */
public final class ColumnSummaryPanel extends JPanel {

  private final ColumnSummaryModel model;
  private final JTextField txtSummary = new JTextField();

  /**
   * @param model the PropertySummaryModel instance
   */
  public ColumnSummaryPanel(final ColumnSummaryModel model) {
    this.model = model;
    model.addSummaryValueListener(() -> {
      final String summaryText = model.getSummaryText();
      txtSummary.setText(summaryText);
      txtSummary.setToolTipText(summaryText.length() != 0 ? (model.getSummary() + ": " + summaryText) : summaryText);
    });
    initialize();
  }

  /**
   * @return the summary type
   */
  public ColumnSummaryModel getModel() {
    return model;
  }

  private void initialize() {
    setLayout(new BorderLayout());
    txtSummary.setHorizontalAlignment(JTextField.RIGHT);
    txtSummary.setEditable(false);
    txtSummary.setFocusable(false);
    add(txtSummary, BorderLayout.CENTER);
    final JPopupMenu menu = createPopupMenu();
    txtSummary.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (!model.isLocked()) {
          menu.show(txtSummary, e.getX(), e.getY() - menu.getPreferredSize().height);
        }
      }
    });
  }

  private JPopupMenu createPopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    final ButtonGroup group = new ButtonGroup();
    for (final ColumnSummaryModel.Summary summary : model.getAvailableSummaries()) {
      final JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(summary.toString()) {
        @Override
        public void actionPerformed(final ActionEvent e) {
          model.setSummary(summary);
        }
      });
      model.addSummaryListener(newSummary -> item.setSelected(newSummary.equals(summary)));
      item.setSelected(model.getSummary().equals(summary));
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
