package org.jminor.common.ui.control;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormattedTextBeanPropertyLinkTest {

  private Date dateValue;
  private Event evtDateValueChanged = new Event();

  @Test
  public void test() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    new FormattedTextBeanPropertyLink(txtString, this, "dateValue", Date.class, evtDateValueChanged, LinkType.READ_WRITE, format);
    assertEquals("String value should be empty on initialization", "__.__.____", txtString.getText());

    final Date date = format.parse("03.10.1975");

    setDateValue(date);
    assertEquals("String value should be '03.10.1975'", "03.10.1975", txtString.getText());
    txtString.setText("03.03.1983");
    assertEquals("String value should be 03.03.1983", format.parse("03.03.1983"), getDateValue());
    txtString.setText("");
    assertNull("String value should be empty", getDateValue());
  }

  public Date getDateValue() {
    return dateValue;
  }

  public void setDateValue(final Date dateValue) {
    this.dateValue = dateValue;
    evtDateValueChanged.fire();
  }
}
