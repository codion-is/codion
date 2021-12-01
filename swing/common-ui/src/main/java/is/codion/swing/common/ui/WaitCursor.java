/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for showing a wait cursor.
 */
public final class WaitCursor {

  private static final Map<Window, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  private WaitCursor() {}

  /**
   * Adds a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used with a try/finally block.
   * <pre>
   WaitCursor.show(component);
   try {
     doSomething();
   }
   finally {
     WaitCursor.hide(component);
   }
   * </pre>
   * @param component the component
   * @see #hide(JComponent)
   */
  public static void show(final JComponent component) {
    show(Windows.getParentWindow(component));
  }

  /**
   * Removes a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used with a try/finally block.
   * <pre>
   WaitCursor.show(component);
   try {
     doSomething();
   }
   finally {
     WaitCursor.hide(component);
   }
   * </pre>
   * @param component the component
   * @see #show(JComponent)
   */
  public static void hide(final JComponent component) {
    hide(Windows.getParentWindow(component));
  }

  public static void show(final Window window) {
    setWaitCursor(true, window);
  }

  /**
   * Removes a wait cursor request for the given window
   * @param window the window
   */
  public static void hide(final Window window) {
    setWaitCursor(false, window);
  }

  /**
   * Adds a wait cursor request for the given window
   * @param window the window
   */
  private static void setWaitCursor(final boolean on, final Window window) {
    if (window == null) {
      return;
    }

    synchronized (WAIT_CURSOR_REQUESTS) {
      int requests = WAIT_CURSOR_REQUESTS.computeIfAbsent(window, win -> 0);
      if (on) {
        requests++;
      }
      else {
        requests--;
      }

      if ((requests == 1 && on) || (requests == 0 && !on)) {
        window.setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      if (requests == 0) {
        WAIT_CURSOR_REQUESTS.remove(window);
      }
      else {
        WAIT_CURSOR_REQUESTS.put(window, requests);
      }
    }
  }
}
