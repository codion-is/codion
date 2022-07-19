/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.imagepanel;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static is.codion.plugin.imagepanel.NavigableImagePanel.readImage;

public class NavigableImagePanelTest {

  @Test
  void test() throws IOException {
    NavigableImagePanel panel = new NavigableImagePanel(readImage("../../documentation/src/docs/asciidoc/images/chinook-client.png"));
    panel.setZoom(2.0);
  }
}
