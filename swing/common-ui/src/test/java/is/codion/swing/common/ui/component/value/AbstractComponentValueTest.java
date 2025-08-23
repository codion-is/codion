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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.value;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractComponentValueTest {

	@Test
	void threadSafetySetValue() throws Exception {
		JTextField textField = new JTextField();
		TestComponentValue componentValue = new TestComponentValue(textField);

		// Test setting value from non-EDT thread
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean setOnEDT = new AtomicBoolean(false);

		Thread nonEDTThread = new Thread(() -> {
			componentValue.set("test value");
			latch.countDown();
		});

		// Override setComponentValue to check if we're on EDT
		TestComponentValue spyValue = new TestComponentValue(textField) {
			@Override
			protected void setComponentValue(String value) {
				setOnEDT.set(SwingUtilities.isEventDispatchThread());
				super.setComponentValue(value);
			}
		};

		nonEDTThread.start();
		nonEDTThread.join();

		// Now test with our spy
		Thread testThread = new Thread(() -> spyValue.set("another value"));
		testThread.start();
		testThread.join();

		// Wait for EDT to process
		SwingUtilities.invokeAndWait(() -> {});

		assertTrue(setOnEDT.get(), "setComponentValue should be called on EDT");
		assertEquals("another value", textField.getText());
	}

	@Test
	void threadSafetyGetValue() throws Exception {
		JTextField textField = new JTextField("initial");
		TestComponentValue componentValue = new TestComponentValue(textField);

		// Test getting value from non-EDT thread
		AtomicReference<String> valueFromThread = new AtomicReference<>();

		Thread nonEDTThread = new Thread(() -> valueFromThread.set(componentValue.get()));

		nonEDTThread.start();
		nonEDTThread.join();

		assertEquals("initial", valueFromThread.get());
	}

	@Test
	void nullHandling() {
		JTextField textField = new JTextField();

		// Test nullable component value
		TestComponentValue nullable = new TestComponentValue(textField);
		nullable.set(null);
		assertEquals("", textField.getText());
		assertNull(nullable.get());

		// Test non-nullable with null value replacement
		TestComponentValue nonNullable = new TestComponentValue(textField, "DEFAULT");
		nonNullable.set(null);
		assertEquals("DEFAULT", textField.getText());
		assertEquals("DEFAULT", nonNullable.get());
	}

	@Test
	void interruptedExceptionHandling() throws Exception {
		JTextField textField = new JTextField();
		TestComponentValue componentValue = new TestComponentValue(textField) {
			@Override
			protected void setComponentValue(String value) {
				// Simulate a long operation that gets interrupted
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Interrupted", e);
				}
				super.setComponentValue(value);
			}
		};

		CountDownLatch startedLatch = new CountDownLatch(1);
		AtomicBoolean wasInterrupted = new AtomicBoolean(false);

		Thread thread = new Thread(() -> {
			startedLatch.countDown();
			try {
				componentValue.set("value");
			}
			catch (RuntimeException e) {
				wasInterrupted.set(Thread.currentThread().isInterrupted());
			}
		});

		thread.start();
		assertTrue(startedLatch.await(1, TimeUnit.SECONDS));
		Thread.sleep(100); // Let it start the invokeAndWait
		thread.interrupt();
		thread.join(2000);

		assertTrue(wasInterrupted.get(), "Thread should maintain interrupted status");
	}

	@Test
	void componentRequired() {
		assertThrows(NullPointerException.class, () -> new TestComponentValue(null));
		assertThrows(NullPointerException.class, () -> new TestComponentValue(null, "default"));
	}

	@Test
	void edtValueSetting() throws Exception {
		JTextField textField = new JTextField();
		TestComponentValue componentValue = new TestComponentValue(textField);

		SwingUtilities.invokeAndWait(() -> {
			componentValue.set("EDT value");
			assertEquals("EDT value", textField.getText());
		});
	}

	// Test implementation of AbstractComponentValue
	private static class TestComponentValue extends AbstractComponentValue<JTextField, String> {

		TestComponentValue(JTextField textField) {
			super(textField);
		}

		TestComponentValue(JTextField textField, String nullValue) {
			super(textField, nullValue);
		}

		@Override
		protected String getComponentValue() {
			String text = component().getText();
			return text.isEmpty() ? null : text;
		}

		@Override
		protected void setComponentValue(String value) {
			component().setText(value == null ? "" : value);
		}
	}
}