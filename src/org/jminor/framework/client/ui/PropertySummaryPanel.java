/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

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

/**
 * A panel that shows a summary value for a numerical column property in a EntityTableModel.
 * The following summary types are implemented: Sum, average, minimum, maximum and minimum & maximum
 */
public class PropertySummaryPanel extends JPanel {

  public enum SummaryType {
    NONE, SUM, AVERAGE, MINIMUM, MAXIMUM, MINIMUM_AND_MAXIMUM;

    @Override
    public String toString() {
      switch (this) {
        case NONE: return FrameworkMessages.get(FrameworkMessages.NONE);
        case SUM: return FrameworkMessages.get(FrameworkMessages.SUM);
        case AVERAGE: return FrameworkMessages.get(FrameworkMessages.AVERAGE);
        case MINIMUM: return FrameworkMessages.get(FrameworkMessages.MINIMUM);
        case MAXIMUM: return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
        case MINIMUM_AND_MAXIMUM: return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
      }

      return super.toString();
    }
  }

  public final Event evtSummaryTypeChanged = new Event();

  private final Property property;
  private final EntityTableModel tableModel;
  private final NumberFormat format = NumberFormat.getInstance();
  private final JLabel lblSummary = new JLabel("", JLabel.RIGHT);

  private SummaryType summaryType = SummaryType.NONE;

  /**
   * Initializes a new PropertySummaryPanel with a default <code>maximumFractionDigits</code> as 4
   * @param property the property on which to base this summary panel
   * @param tableModel the tableModel
   */
  public PropertySummaryPanel(final Property property, final EntityTableModel tableModel) {
    this(property, tableModel, 4);
  }

  /**
   * Initializes a new PropertySummaryPanel.
   * @param property the property on which to base this summary panel
   * @param tableModel the tableModel
   * @param maximumFractionDigits the maximum number of fraction digits to show
   */
  public PropertySummaryPanel(final Property property, final EntityTableModel tableModel,
                              final int maximumFractionDigits) {
    this.property = property;
    this.tableModel = tableModel;
    this.format.setMaximumFractionDigits(maximumFractionDigits);
    initialize();
  }

  /**
   * @return the summary type
   */
  public SummaryType getSummaryType() {
    return summaryType;
  }

  /**
   * @param summaryType the type of summary to show
   */
  public void setSummaryType(final SummaryType summaryType) {
    if (this.summaryType != summaryType) {
      this.summaryType = summaryType;
      evtSummaryTypeChanged.fire();
    }
  }

  private void initialize() {
    setLayout(new BorderLayout());
    lblSummary.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(lblSummary, BorderLayout.CENTER);

    final List<SummaryType> summaryTypes = getSummaryStates(property.getPropertyType());
    if (summaryTypes.size() > 0) {
      final ActionListener updater = getUpdater(tableModel);//todo should update on insert
      tableModel.evtFilteringDone.addListener(updater);//todo summary is updated twice per refresh
      tableModel.evtRefreshDone.addListener(updater);
      tableModel.evtSelectionChangedAdjusting.addListener(updater);
      tableModel.evtSelectionChanged.addListener(updater);
      evtSummaryTypeChanged.addListener(updater);

      final JPopupMenu menu = createPopupMenu(summaryTypes);
      lblSummary.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
          menu.show(lblSummary, e.getX(), e.getY()-menu.getPreferredSize().height);
        }
      });
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
      evtSummaryTypeChanged.addListener(new ActionListener() {
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
    final Collection<Object> values = tableModel.getValues(property, !tableModel.stSelectionEmpty.isActive());
    switch (summaryType) {
      case SUM:
        return sum(values);
      case AVERAGE:
        return average(values);
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

  private String average(final Collection<Object> values) {
    String txt = "";
    if (property.getPropertyType() == Type.INT) {
      double sum = 0;
      int count = 0;
      for (final Object obj : values) {
        sum += (Integer)obj;
        count++;
      }
      if (count > 0)
        txt = format.format(sum/count);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double sum = 0;
      int count = 0;
      for (final Object obj : values) {
        sum += (Double)obj;
        count++;
      }
      if (count > 0)
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
      txt = format.format(sum);
    }
    else if (property.getPropertyType() == Type.DOUBLE) {
      double sum = 0;
      for (final Object obj : values)
        sum += (Double)obj;
      txt = format.format(sum);
    }

    return txt;
  }
}
