/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.Constants;
import org.jminor.common.model.Event;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PropertySummaryPanel extends JPanel {

  public enum SummaryType {
    NONE {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.NONE);
      }
    },
    SUM {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.SUM);
      }
    },
    AVARAGE {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.AVERAGE);
      }
    },
    MINIMUM {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM);
      }
    },
    MAXIMUM {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
      }
    },
    MINIMUM_AND_MAXIMUM {
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
      }
    }
  }

  public final Event evtStateChanged = new Event("PropertySummaryPanel.evtStateChanged");

  private final Property property;
  private final EntityTableModel tableModel;
  private final NumberFormat format = NumberFormat.getInstance();
  private final JLabel lblSummary = new JLabel("", JLabel.RIGHT);

  private SummaryType summaryType = SummaryType.NONE;

  public PropertySummaryPanel(final Property property, final EntityTableModel tableModel) {
    this.property = property;
    this.tableModel = tableModel;
    initialize();
  }

  /**
   * @return Value for property 'summaryType'.
   */
  public SummaryType getSummaryType() {
    return summaryType;
  }

  /**
   * @param summaryType Value to set for property 'summaryType'.
   */
  public void setSummaryType(final SummaryType summaryType) {
    if (this.summaryType != summaryType) {
      this.summaryType = summaryType;
      evtStateChanged.fire();
    }
  }

  private void initialize() {
    setLayout(new BorderLayout());
    lblSummary.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(lblSummary, BorderLayout.CENTER);

    final List<SummaryType> summaryTypes = getSummaryStates(property.getPropertyType());
    if (summaryTypes.size() > 0) {
      final ActionListener updater = getUpdater(tableModel);
      tableModel.evtAfterFiltering.addListener(updater);//todo happens twice after refresh
      tableModel.evtRefreshDone.addListener(updater);
      tableModel.evtSelectionChangedAdjusting.addListener(updater);
      tableModel.evtSelectionChanged.addListener(updater);
      evtStateChanged.addListener(updater);

      final JPopupMenu menu = createPopupMenu(summaryTypes);
      lblSummary.addMouseListener(new MouseAdapter() {
        public void mouseReleased(final MouseEvent e) {
          menu.show(lblSummary, e.getX(), e.getY()-menu.getPreferredSize().height);
        }
      });
      format.setMaximumFractionDigits(4);
      lblSummary.setBorder(BorderFactory.createEtchedBorder());
    }
  }

  private ActionListener getUpdater(final EntityTableModel tableModel) {
    return new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (summaryType == SummaryType.NONE) {
          lblSummary.setToolTipText("");
          lblSummary.setText("");
        }
        else {
          final String summaryTxt = getSummaryText();
          final String txt =
                  summaryTxt.length() > 0 ? summaryTxt + (tableModel.stSelectionEmpty.isActive() ? "" : "*")
                          : summaryTxt;
          lblSummary.setToolTipText(summaryType.toString() + (txt.length() > 0 ? ": " + txt : ""));
          lblSummary.setText(txt);
        }
      }
    };
  }

  private List<SummaryType> getSummaryStates(final Type propertyType) {
    final List<SummaryType> ret = new ArrayList<SummaryType>();
    switch(propertyType) {
      case INT:
      case DOUBLE:
        ret.addAll(Arrays.asList(SummaryType.values()));
      default:
        return ret;
    }
  }

  private JPopupMenu createPopupMenu(final List<SummaryType> summaryTypes) {
    final JPopupMenu ret = new JPopupMenu();
    final ButtonGroup group = new ButtonGroup();
    for (final SummaryType summaryType : summaryTypes) {
      final JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(summaryType.toString()) {
        public void actionPerformed(ActionEvent e) {
          setSummaryType(summaryType);
        }
      });
      evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          item.setSelected(PropertySummaryPanel.this.summaryType == summaryType);
        }
      });
      group.add(item);
      ret.add(item);
    }

    return ret;
  }

  private String getSummaryText() {
    final Collection<Object> values = tableModel.getValues(property, true);
    switch (summaryType) {
      case SUM:
        return sum(values);
      case AVARAGE:
        return avarage(values);
      case MINIMUM:
        return minimum(values);
      case MAXIMUM:
        return maximum(values);
      case MINIMUM_AND_MAXIMUM:
        return minimumAndMaximum(values);
    }

    return "";
  }

  private String minimumAndMaximum(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      for (final Object obj : values) {
        max = Math.max(max, (Integer) obj);
        min = Math.min(min, (Integer) obj);
      }
      if (max != Integer.MIN_VALUE)
        txt = format.format(min) + "/" + format.format(max);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double min = Double.MAX_VALUE;
      double max = Double.MIN_VALUE;
      for (final Object obj : values) {
        max = Math.max(max, (Double) obj);
        min = Math.min(min, (Double) obj);
      }
      if (max != Double.MIN_VALUE)
        txt = format.format(min) + "/" + format.format(max);
    }

    return txt;
  }

  private String maximum(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      int max = Integer.MIN_VALUE;
      for (final Object obj : values)
        max = Math.max(max, (Integer) obj);
      if (max != Integer.MIN_VALUE)
        txt = format.format(max);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double max = Double.MIN_VALUE;
      for (final Object obj : values)
        max = Math.max(max, (Double) obj);
      if (max != Double.MIN_VALUE)
        txt = format.format(max);
    }

    return txt;
  }

  private String minimum(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      int min = Integer.MAX_VALUE;
      for (final Object obj : values)
        min = Math.min(min, (Integer) obj);
      if (min != Integer.MAX_VALUE)
        txt = format.format(min);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double min = Double.MAX_VALUE;
      for (final Object obj : values)
        min = Math.min(min, (Double) obj);
      if (min != Double.MAX_VALUE)
        txt = format.format(min);
    }

    return txt;
  }

  private String avarage(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      int sum = 0;
      int count = 0;
      for (final Object obj : values) {
        sum += (Integer)obj;
        count++;
      }
      if (count > 0 && sum != 0 && sum != Constants.INT_NULL_VALUE)
        txt = format.format(sum/count);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double sum = 0;
      int count = 0;
      for (final Object obj : values) {
        sum += (Double)obj;
        count++;
      }
      if (count > 0 || sum != 0 && sum != Constants.DOUBLE_NULL_VALUE)
        txt = format.format(sum/count);
    }

    return txt;
  }

  private String sum(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      int sum = 0;
      for (final Object obj : values)
        sum += (Integer)obj;
      if (sum != 0 && sum != Constants.INT_NULL_VALUE)
        txt = format.format(sum);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double sum = 0;
      for (final Object obj : values)
        sum += (Double)obj;
      if (sum != 0 && sum != Constants.DOUBLE_NULL_VALUE)
        txt = format.format(sum);
    }

    return txt;
  }
}
