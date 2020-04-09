/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.reports.ReportUiWrapper;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.ResourceBundle;

/**
 * A static utility class for displaying reports.
 */
public final class EntityReports {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityReports.class.getName());

  private static final Dimension MINIMUM_REPORT_WINDOW_SIZE = new Dimension(800, 600);
  private static final double SCREEN_SIZE_RATIO = 0.8;

  private EntityReports() {}

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param reportTitle the title to display on the frame
   * @param connectionProvider the db provider
   * @param <R> the report result type
   */
  public static <R> void viewJdbcReport(final JComponent component, final ReportWrapper<R, ?> reportWrapper,
                                        final ReportUiWrapper<R> uiWrapper, final String reportTitle,
                                        final EntityConnectionProvider connectionProvider) {
    try {
      Components.showWaitCursor(component);
      viewReport(connectionProvider.getConnection().fillReport(reportWrapper), uiWrapper, reportTitle);
    }
    catch (final ReportException | DatabaseException e) {
      throw new RuntimeException(e);
    }
    finally {
      Components.hideWaitCursor(component);
    }
  }

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param dataSource the datasource used to provide the report data
   * @param reportTitle the title to display on the frame
   * @param <R> the report result type
   * @param <D> the type of the data source used to fill the report
   */
  public static <R, D> void viewReport(final JComponent component, final ReportWrapper<R, D> reportWrapper,
                                       final ReportUiWrapper<R> uiWrapper, final D dataSource,
                                       final String reportTitle) {
    try {
      Components.showWaitCursor(component);
      viewReport(reportWrapper.fillReport(dataSource), uiWrapper, reportTitle);
    }
    catch (final ReportException e) {
      throw new RuntimeException(e);
    }
    finally {
      Components.hideWaitCursor(component);
    }
  }

  /**
   * Shows a view for report printing
   * @param reportResult the report result
   * @param uiWrapper the UI wrapper
   * @param frameTitle the title to display on the frame
   * @param <R> the report result type
   */
  public static <R> void viewReport(final R reportResult, final ReportUiWrapper<R> uiWrapper, final String frameTitle) {
    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new JFrame(frameTitle == null ? MESSAGES.getString("report_printer") : frameTitle);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(uiWrapper.createReportComponent(reportResult));
      Windows.resizeWindow(frame, SCREEN_SIZE_RATIO, MINIMUM_REPORT_WINDOW_SIZE);
      Windows.centerWindow(frame);
      frame.setVisible(true);
    });
  }
}
