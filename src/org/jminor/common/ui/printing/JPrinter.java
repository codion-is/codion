/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.printing;

import javax.swing.JTable;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.PrintGraphics;
import java.awt.PrintJob;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

/**
 * @author unknown
 */
public class JPrinter  {

  private JPrinter() {}

  public static void print(Pageable pr) throws PrinterException {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setPageable(pr);
    if (pj.printDialog()) {
      pj.print();
    }
  }

  public static void print(Printable pr) throws PrinterException {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setPrintable(pr);
    if (pj.printDialog()) {
      pj.print();
    }
  }

  public static void print(JTable table) throws PrinterException {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setPrintable(new JTablePrinter(table));
    if (pj.printDialog()) {
      pj.print();
    }
  }

  public static void printLongString(PrintJob pjob, Graphics pg, String s) throws IOException {
    int pageNum = 1;
    int linesForThisPage = 0;

    // Note: String is immutable so won't change while printing.
    if (!(pg instanceof PrintGraphics)) {
      throw new IllegalArgumentException("Graphics context not PrintGraphics");
    }
    StringReader sr = new StringReader(s);
    LineNumberReader lnr = new LineNumberReader(sr);
    int pageHeight = pjob.getPageDimension().height;
    pageHeight -= 20;
    Font helv = new Font("Arial", Font.PLAIN, 12);
    //have to set the font to get any output
    pg.setFont(helv);

    FontMetrics fm = pg.getFontMetrics(helv);
    int fontHeight = fm.getHeight();
    int fontDescent = fm.getDescent();
    int curHeight = 0;
    String nextLine;
    try {
      do {
        nextLine = lnr.readLine();
        if (nextLine != null) {
          if ((curHeight + fontHeight) > pageHeight) {
            // New Page
            System.out.println("" + linesForThisPage + " lines printed for page " + pageNum);
            pageNum++;
            linesForThisPage = 0;
            if (pg != null)
              pg.dispose();
            pg = pjob.getGraphics();
            if (pg != null) {
              pg.setFont(helv);
            }
            curHeight = 0;
          }
          curHeight += fontHeight;
          if (pg != null) {
            pg.drawString(nextLine, 0, curHeight - fontDescent);
            linesForThisPage++;
          }
          else {
            System.out.println("pg null");
          }
        }
      }
      while (nextLine != null);
    }
    catch (EOFException eof) {
      // Fine, ignore
    }
  }
}
