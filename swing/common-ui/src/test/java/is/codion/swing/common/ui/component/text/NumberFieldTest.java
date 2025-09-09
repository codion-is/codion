/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import static java.awt.event.KeyEvent.*;
import static org.junit.jupiter.api.Assertions.*;

public final class NumberFieldTest {

	@Test
	void integerFieldTest() {
		NumberField<Integer> integerField = NumberField.builder()
						.numberClass(Integer.class)
						.groupingUsed(false)
						.build();
		integerField.set(42);
		assertEquals("42", integerField.getText());
		integerField.setText("22");
		assertEquals(Integer.valueOf(22), integerField.get());

		integerField.set(10000000);
		assertEquals("10000000", integerField.getText());
		integerField.set(100000000);
		assertEquals("100000000", integerField.getText());

		NumberField<Integer> zeroToTen = NumberField.builder()
						.numberClass(Integer.class)
						.range(0, 10)
						.build();

		assertThrows(IllegalArgumentException.class, () -> zeroToTen.set(100));
		assertEquals("", zeroToTen.getText());
		zeroToTen.set(9);
		assertEquals("9", zeroToTen.getText());
		assertThrows(IllegalArgumentException.class, () -> zeroToTen.set(-1));
		assertEquals("", zeroToTen.getText());
		assertThrows(IllegalArgumentException.class, () -> zeroToTen.set(-10));
		assertEquals("", zeroToTen.getText());

		NumberField<Integer> zeroToMax = NumberField.builder()
						.numberClass(Integer.class)
						.range(0, Integer.MAX_VALUE)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.build();

		DecimalFormat decimalFormat = (DecimalFormat) ((NumberDocument<Integer>) zeroToMax.getDocument()).format();
		decimalFormat.setGroupingSize(3);
		decimalFormat.setGroupingUsed(true);
		zeroToMax.setText("100.000.000");
		assertEquals(100000000, (int) zeroToMax.get());
		zeroToMax.setText("10.00.000");
		assertEquals("1.000.000", zeroToMax.getText());
		assertEquals(1000000, (int) zeroToMax.get());
		zeroToMax.set(123456789);
		assertEquals("123.456.789", zeroToMax.getText());
		zeroToMax.setText("987654321");
		assertEquals(987654321, (int) zeroToMax.get());

		zeroToMax.set(null);
		zeroToMax.observable().addConsumer(value -> assertEquals(42, value));
		zeroToMax.set(42);
	}

	@Test
	void skipGroupingSeparator() {
		NumberField<Integer> integerField = NumberField.builder()
						.numberClass(Integer.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.groupingUsed(true)
						.build();
		KeyListener keyListener = integerField.getKeyListeners()[0];
		integerField.set(123456);
		assertEquals("123.456", integerField.getText());
		integerField.setCaretPosition(3);
		keyListener.keyReleased(new KeyEvent(integerField, KEY_RELEASED, System.currentTimeMillis(), 0,
						VK_DELETE, CHAR_UNDEFINED));
		assertEquals(4, integerField.getCaretPosition());
		keyListener.keyReleased(new KeyEvent(integerField, KEY_RELEASED, System.currentTimeMillis(), 0,
						VK_BACK_SPACE, CHAR_UNDEFINED));
		assertEquals(3, integerField.getCaretPosition());
	}

	@Test
	void longFieldTest() {
		NumberField<Long> longField = NumberField.builder()
						.numberClass(Long.class)
						.groupingUsed(false)
						.build();
		longField.set(42L);
		assertEquals("42", longField.getText());
		longField.setText("22");
		assertEquals(Long.valueOf(22), longField.get());

		longField.set(10000000000000L);
		assertEquals("10000000000000", longField.getText());
		longField.set(1000000000000L);
		assertEquals("1000000000000", longField.getText());

		longField = NumberField.builder()
						.numberClass(Long.class)
						.groupingUsed(true)
						.groupingSeparator(',')
						.build();
		longField.set(1_000_000_000L);
		assertEquals("1,000,000,000", longField.getText());

		longField = NumberField.builder()
						.numberClass(Long.class)
						.groupingUsed(false)
						.groupingSeparator(',')
						.build();

		NumberField<Long> rangedField = NumberField.builder()
						.numberClass(Long.class)
						.range(0, 10)
						.build();

		rangedField.setText("");
		assertThrows(IllegalArgumentException.class, () -> rangedField.set(100L));
		assertEquals("", rangedField.getText());
		rangedField.set(9L);
		assertEquals("9", rangedField.getText());
		rangedField.setText("");
		assertThrows(IllegalArgumentException.class, () -> rangedField.set(-1L));
		assertEquals("", rangedField.getText());
	}

	@Test
	void testNoGrouping() {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.groupingUsed(false)
						.build();
		doubleField.setText(",");
		assertEquals("0,", doubleField.getText());
		doubleField.set(42.2);
		assertEquals("42,2", doubleField.getText());
		doubleField.setText("22,3");
		assertEquals(Double.valueOf(22.3), doubleField.get());
		doubleField.setText("22.5");//note this is a thousand separator
		assertEquals(Double.valueOf(22.3), doubleField.get());
		assertEquals("22,3", doubleField.getText());
		doubleField.setText("22.123.123,123");
		assertEquals("22,3", doubleField.getText());
		assertEquals(Double.valueOf(22.3), doubleField.get());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.groupingUsed(false)
						.build();

		doubleField.set(42.2);
		assertEquals("42.2", doubleField.getText());
		doubleField.setText("2,123,123.123");
		assertEquals("42.2", doubleField.getText());
		assertEquals(Double.valueOf(42.2), doubleField.get());

		doubleField.set(10000000d);
		assertEquals("10000000", doubleField.getText());
		doubleField.set(100000000.4d);
		assertEquals("100000000.4", doubleField.getText());
	}

	@Test
	void testGrouping() {
		DecimalFormat decimalFormat = new DecimalFormat();
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.format(decimalFormat)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.groupingUsed(true)
						.build();
		assertEquals(0, doubleField.getCaretPosition());
		doubleField.setText(",");
		assertEquals("0,", doubleField.getText());
		assertEquals(2, doubleField.getCaretPosition());
		doubleField.set(42.2);
		assertEquals("42,2", doubleField.getText());
		assertEquals(4, doubleField.getCaretPosition());
		doubleField.setText("22,3");
		assertEquals(Double.valueOf(22.3), doubleField.get());
		doubleField.setText("22.3");//note this is a thousand separator
		assertEquals(Double.valueOf(223), doubleField.get());
		assertEquals("223", doubleField.getText());
		doubleField.setText("22.123.123,123");
		assertEquals("22.123.123,123", doubleField.getText());
		assertEquals(Double.valueOf(22123123.123), doubleField.get());
		doubleField.setText("22123123,123");
		assertEquals("22.123.123,123", doubleField.getText());
		assertEquals(Double.valueOf(22123123.123), doubleField.get());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.format(decimalFormat)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.groupingUsed(true)
						.build();

		doubleField.set(42.2);
		assertEquals("42.2", doubleField.getText());
		doubleField.setText("22,123,123.123");
		assertEquals("22,123,123.123", doubleField.getText());
		assertEquals(Double.valueOf(22123123.123), doubleField.get());
		doubleField.setText("22123123.123");
		assertEquals("22,123,123.123", doubleField.getText());
		assertEquals(Double.valueOf(22123123.123), doubleField.get());

		doubleField.set(10000000d);
		assertEquals("10,000,000", doubleField.getText());
		doubleField.set(100000000.4d);
		assertEquals("100,000,000.4", doubleField.getText());

		doubleField.setText("2.2.2");
		assertEquals("100,000,000.4", doubleField.getText());
		doubleField.setText("..22.2.2.2");
		assertEquals("100,000,000.4", doubleField.getText());
		doubleField.setText("22.2.2.2");
		assertEquals("100,000,000.4", doubleField.getText());
		doubleField.setText("2222.2.2.2");
		assertEquals("100,000,000.4", doubleField.getText());
	}

	@Test
	void caretPosition() throws BadLocationException {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.groupingUsed(true)
						.build();
		NumberDocument<Double> document = doubleField.document();

		doubleField.setText("123456789");
		assertEquals("123.456.789", doubleField.getText());

		doubleField.setCaretPosition(3);
		doubleField.moveCaretPosition(8);
		assertEquals(".456.", doubleField.getSelectedText());

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);

		doubleField.paste();
		assertEquals("123.789", doubleField.getText());
		assertEquals(4, doubleField.getCaretPosition());

		document.insertString(3, "456", null);
		assertEquals("123.456.789", doubleField.getText());
		assertEquals(7, doubleField.getCaretPosition());

		doubleField.setCaretPosition(3);
		doubleField.moveCaretPosition(11);
		assertEquals(".456.789", doubleField.getSelectedText());

		doubleField.paste();
		assertEquals("123", doubleField.getText());
		assertEquals(3, doubleField.getCaretPosition());

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(".456.789"), null);

		doubleField.paste();
		assertEquals("123.456.789", doubleField.getText());
		assertEquals(11, doubleField.getCaretPosition());

		doubleField.setText("");

		document.insertString(0, ",", null);
		assertEquals("0,", doubleField.getText());
		assertEquals(2, doubleField.getCaretPosition());

		doubleField.setText("");
		document.insertString(0, "1", null);
		assertEquals("1", doubleField.getText());
		assertEquals(1, doubleField.getCaretPosition());

		document.insertString(1, "2", null);
		assertEquals("12", doubleField.getText());
		assertEquals(2, doubleField.getCaretPosition());

		document.insertString(2, "3", null);
		assertEquals("123", doubleField.getText());
		assertEquals(3, doubleField.getCaretPosition());

		document.insertString(3, "4", null);
		assertEquals("1.234", doubleField.getText());
		assertEquals(5, doubleField.getCaretPosition());

		document.insertString(5, "5", null);
		assertEquals("12.345", doubleField.getText());
		assertEquals(6, doubleField.getCaretPosition());

		document.insertString(6, "6", null);
		assertEquals("123.456", doubleField.getText());
		assertEquals(7, doubleField.getCaretPosition());

		document.insertString(7, "7", null);
		assertEquals("1.234.567", doubleField.getText());
		assertEquals(9, doubleField.getCaretPosition());
	}

	@Test
	void setSeparatorsSameCharacter() {
		assertThrows(IllegalArgumentException.class, () -> NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator('.')
						.groupingSeparator('.'));
		assertThrows(IllegalArgumentException.class, () -> NumberField.builder()
						.numberClass(Double.class)
						.groupingSeparator('.')
						.decimalSeparator('.'));
	}

	@Test
	void maximumFractionDigits() throws BadLocationException {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.maximumFractionDigits(2)
						.build();
		doubleField.set(5.1254);
		assertEquals("5,12", doubleField.getText());
		doubleField.setText("5,123");
		assertEquals("5,12", doubleField.getText());
		doubleField.getDocument().insertString(3, "4", null);
		assertEquals("5,14", doubleField.getText());
		doubleField.getDocument().remove(3, 1);
		assertEquals("5,1", doubleField.getText());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.maximumFractionDigits(3)
						.build();
		doubleField.setText("5,12378");
		assertEquals("5,123", doubleField.getText());//no rounding should occur

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.build();
		doubleField.setText("5,12378");
		assertEquals("5,12378", doubleField.getText());
	}

	@Test
	void decimalSeparators() {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.groupingUsed(false)
						.decimalSeparator('.')
						.build();
		doubleField.setText("1.5");
		assertEquals(Double.valueOf(1.5), doubleField.get());
		doubleField.setText("123.34.56");
		assertEquals(Double.valueOf(1.5), doubleField.get());

		doubleField.setText("1,5");
		assertEquals(Double.valueOf(1.5), doubleField.get());
		doubleField.setText("1,4.5");
		assertEquals(Double.valueOf(1.5), doubleField.get());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.groupingUsed(false)
						.decimalSeparator(',')
						.build();
		doubleField.setText("1,6");
		assertEquals(Double.valueOf(1.6), doubleField.get());
		doubleField.setText("123,34,56");
		assertEquals(Double.valueOf(1.6), doubleField.get());
	}

	@Test
	void trailingDecimalSeparator() throws BadLocationException {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.build();
		NumberDocument<Double> document = doubleField.document();
		document.insertString(0, "1", null);
		assertEquals(Double.valueOf(1), doubleField.get());
		document.insertString(1, ".", null);
		assertEquals("1.", doubleField.getText());
		assertEquals(Double.valueOf(1), doubleField.get());
		document.insertString(2, "1", null);
		assertEquals("1.1", doubleField.getText());
		assertEquals(Double.valueOf(1.1), doubleField.get());
	}

	@Test
	void setSeparators() {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.groupingUsed(true)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.build();
		doubleField.set(12345678.9);
		assertEquals("12,345,678.9", doubleField.getText());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.value(12345678.9)
						.groupingUsed(true)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.build();
		assertEquals("12.345.678,9", doubleField.getText());

		doubleField = NumberField.builder()
						.numberClass(Double.class)
						.groupingUsed(true)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.build();
		doubleField.set(12345678.9);

		assertEquals("12,345,678.9", doubleField.getText());
	}

	@Test
	void trailingDecimalZeros() throws BadLocationException {
		NumberField<Double> doubleField = NumberField.builder()
						.numberClass(Double.class)
						.decimalSeparator('.')
						.groupingSeparator(',')
						.build();
		NumberDocument<Double> document = doubleField.document();

		document.insertString(0, "1", null);
		document.insertString(1, ".", null);
		assertEquals("1.", doubleField.getText());

		document.insertString(2, "0", null);
		assertEquals("1.0", doubleField.getText());

		document.insertString(3, "0", null);
		assertEquals("1.00", doubleField.getText());

		document.insertString(4, "1", null);
		assertEquals("1.001", doubleField.getText());

		document.insertString(5, "0", null);
		assertEquals("1.0010", doubleField.getText());

		document.insertString(6, "0", null);
		assertEquals("1.00100", doubleField.getText());

		document.insertString(7, "2", null);
		assertEquals("1.001002", doubleField.getText());

		document.insertString(8, "0", null);
		assertEquals("1.0010020", doubleField.getText());

		doubleField.setText("");
		document.insertString(0, ".", null);
		assertEquals("0.", doubleField.getText());

		document.insertString(2, "0", null);
		assertEquals("0.0", doubleField.getText());

		document.insertString(3, "1", null);
		assertEquals("0.01", doubleField.getText());
	}

	@Test
	void silentValidation() {
		NumberField<Integer> integerField = NumberField.builder()
						.numberClass(Integer.class)
						.value(10)
						.range(0, 100)
						.build();
		assertEquals(10, integerField.get());
		assertThrows(IllegalArgumentException.class, () -> integerField.set(101));

		NumberField<Integer> silentField = NumberField.builder()
						.numberClass(Integer.class)
						.value(10)
						.range(0, 100)
						.silentValidation(true)
						.build();
		silentField.set(50);
		assertNotNull(silentField.get());
		silentField.set(110);
		assertNull(silentField.get());
	}
}
