/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.swing.common.ui.control.Control;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;

/**
 * A panel that shows a summary value for a numerical column property in a EntityTableModel.
 */
public final class ColumnSummaryPanel extends JPanel {

  private final ColumnSummaryModel model;
  private final JTextField summaryField = new JTextField();

  /**
   * @param model the PropertySummaryModel instance
   */
  public ColumnSummaryPanel(final ColumnSummaryModel model) {
    this.model = requireNonNull(model, "model");
    model.addSummaryValueListener(() -> {
      final String summaryText = model.getSummaryText();
      summaryField.setText(summaryText);
      summaryField.setToolTipText(summaryText.length() != 0 ? (model.getSummary() + ": " + summaryText) : summaryText);
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
    summaryField.setHorizontalAlignment(SwingConstants.RIGHT);
    summaryField.setEditable(false);
    summaryField.setFocusable(false);
    add(summaryField, BorderLayout.CENTER);
    final JPopupMenu menu = createPopupMenu();
    summaryField.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (!model.isLocked()) {
          menu.show(summaryField, e.getX(), e.getY() - menu.getPreferredSize().height);
        }
      }
    });
  }

  private JPopupMenu createPopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    final ButtonGroup group = new ButtonGroup();
    for (final ColumnSummaryModel.Summary summary : model.getAvailableSummaries()) {
      final JRadioButtonMenuItem item = new JRadioButtonMenuItem(Control.builder(() -> model.setSummary(summary))
              .caption(summary.toString())
              .build());
      model.addSummaryListener(newSummary -> item.setSelected(newSummary.equals(summary)));
      item.setSelected(model.getSummary().equals(summary));
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
