/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.imagepanel;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static is.codion.plugin.imagepanel.NavigableImagePanel.readImage;

public class NavigableImagePanelTest {

  @Test
  public void test() throws IOException {
    final NavigableImagePanel panel = new NavigableImagePanel(readImage("../../documentation/src/docs/asciidoc/images/chinook-client.png"));
    panel.setZoom(2.0);
  }
}
