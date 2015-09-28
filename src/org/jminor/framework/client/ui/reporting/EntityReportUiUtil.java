/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.reporting;

import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.swing.ui.UiUtil;
import org.jminor.common.swing.ui.reports.ReportUIWrapper;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * A static utility class for displaying reports.
 */
public final class EntityReportUiUtil {

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
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final JFrame frame = new JFrame(frameTitle == null ? FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER) : frameTitle);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(uiWrapper.createReportComponent(reportResult));
        UiUtil.resizeWindow(frame, SCREEN_SIZE_RATIO, MINIMUM_REPORT_WINDOW_SIZE);
        UiUtil.centerWindow(frame);
        frame.setVisible(true);
      }
    });
  }
}
