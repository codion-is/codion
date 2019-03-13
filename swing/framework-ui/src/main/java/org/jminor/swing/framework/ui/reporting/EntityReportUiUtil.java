/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui.reporting;

import org.jminor.common.db.reports.ReportDataWrapper;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.reports.ReportUIWrapper;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A static utility class for displaying reports.
 */
public final class EntityReportUiUtil {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityReportUiUtil.class.getName(), Locale.getDefault());

  private static final Dimension MINIMUM_REPORT_WINDOW_SIZE = new Dimension(800, 600);
  private static final double SCREEN_SIZE_RATIO = 0.8;

  private EntityReportUiUtil() {}

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param reportTitle the title to display on the frame
   * @param connectionProvider the db provider
   */
  public static void viewJdbcReport(final JComponent component, final ReportWrapper reportWrapper,
                                    final ReportUIWrapper uiWrapper, final String reportTitle,
                                    final EntityConnectionProvider connectionProvider) {
    try {
      UiUtil.setWaitCursor(true, component);
      viewReport(EntityReportUtil.fillReport(reportWrapper, connectionProvider), uiWrapper, reportTitle);
    }
    catch (final ReportException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, component);
    }
  }

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param dataSource the datasource used to provide the report data
   * @param reportTitle the title to display on the frame
   */
  public static void viewReport(final JComponent component, final ReportWrapper reportWrapper,
                                final ReportUIWrapper uiWrapper, final ReportDataWrapper dataSource,
                                final String reportTitle) {
    try {
      UiUtil.setWaitCursor(true, component);
      viewReport(EntityReportUtil.fillReport(reportWrapper, dataSource), uiWrapper, reportTitle);
    }
    catch (final ReportException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, component);
    }
  }

  /**
   * Shows a view for report printing
   * @param reportResult the report result
   * @param uiWrapper the UI wrapper
   * @param frameTitle the title to display on the frame
   */
  public static void viewReport(final ReportResult reportResult, final ReportUIWrapper uiWrapper, final String frameTitle) {
    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new JFrame(frameTitle == null ? MESSAGES.getString("report_printer") : frameTitle);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(uiWrapper.createReportComponent(reportResult));
      UiUtil.resizeWindow(frame, SCREEN_SIZE_RATIO, MINIMUM_REPORT_WINDOW_SIZE);
      UiUtil.centerWindow(frame);
      frame.setVisible(true);
    });
  }
}
