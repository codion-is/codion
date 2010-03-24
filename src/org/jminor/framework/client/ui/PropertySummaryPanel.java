/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.client.model.PropertySummaryModel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A panel that shows a summary value for a numerical column property in a EntityTableModel.
 */
public class PropertySummaryPanel extends JPanel {

  private final PropertySummaryModel model;
  private final JLabel lblSummary = new JLabel("", JLabel.RIGHT);

  /**
   * @param model the PropertySummaryModel instance
   */
  public PropertySummaryPanel(final PropertySummaryModel model) {
    this.model = model;
    model.eventSummaryChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String summaryText = model.getSummaryText();
        lblSummary.setText(summaryText);
        lblSummary.setToolTipText(summaryText.length() > 0 ? (model.getSummaryType() + ": " + summaryText) : summaryText);
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
    lblSummary.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(lblSummary, BorderLayout.CENTER);
    final JPopupMenu menu = createPopupMenu();
    lblSummary.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        menu.show(lblSummary, e.getX(), e.getY()-menu.getPreferredSize().height);
      }
    });
    lblSummary.setBorder(BorderFactory.createEtchedBorder());
  }

  private JPopupMenu createPopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    final ButtonGroup group = new ButtonGroup();
    for (final PropertySummaryModel.Summary summaryType : model.getSummaryTypes()) {
      final JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(summaryType.toString()) {
        public void actionPerformed(ActionEvent e) {
          model.setSummaryType(summaryType);
        }
      });
      model.eventSummaryTypeChanged().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          item.setSelected(PropertySummaryPanel.this.getModel().getSummaryType() == summaryType);
        }
      });
      item.setSelected(PropertySummaryPanel.this.getModel().getSummaryType() == summaryType);
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
