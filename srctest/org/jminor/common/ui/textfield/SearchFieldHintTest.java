package org.jminor.common.ui.textfield;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import javax.swing.JTextField;

public class SearchFieldHintTest {

  @Test
  public void test() {
    final JTextField txtOne = new JTextField();
    SearchFieldHint.enable(txtOne);

    final JTextField txt = new JTextField();
    final SearchFieldHint hint = SearchFieldHint.enable(txt, "search");
    assertEquals("search", hint.getSearchHint());
    assertEquals("search", txt.getText());
    txt.setText("he");
    assertEquals("he", txt.getText());
  }
}
