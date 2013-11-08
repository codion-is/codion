/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.framework.client.model.PropertySummaryModel;

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
public final class PropertySummaryPanel extends JPanel {

  private final PropertySummaryModel model;
  private final JTextField txtSummary = new JTextField();

  /**
   * @param model the PropertySummaryModel instance
   */
  public PropertySummaryPanel(final PropertySummaryModel model) {
    this.model = model;
    model.addSummaryValueListener(new EventListener() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        final String summaryText = model.getSummaryText();
        txtSummary.setText(summaryText);
        txtSummary.setToolTipText(summaryText.length() != 0 ? (model.getCurrentSummary() + ": " + summaryText) : summaryText);
      }
    });
    initialize();
  }

  /**
   * @return the summary type
   */
  public PropertySummaryModel getModel() {
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
      /** {@inheritDoc} */
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
    for (final PropertySummaryModel.Summary summary : model.getAvailableSummaries()) {
      final JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(summary.toString()) {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
          model.setCurrentSummary(summary);
        }
      });
      model.addSummaryListener(new EventInfoListener<PropertySummaryModel.Summary>() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred(final PropertySummaryModel.Summary newSummary) {
          item.setSelected(newSummary == summary);
        }
      });
      item.setSelected(model.getCurrentSummary() == summary);
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
