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
package is.codion.common.model.preferences;

import is.codion.common.utilities.Text;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UserPreferences.
 * Tests CRUD operations, null handling, thread safety, and Java Preferences integration.
 */
public class UserPreferencesTest {

	private static final String TEST_KEY = "test.key";
	private static final String TEST_KEY_2 = "test.key.2";
	private static final String TEST_VALUE = "test.value";
	private static final String TEST_VALUE_2 = "test.value.2";
	private static final String DEFAULT_VALUE = "default.value";

	@BeforeEach
	void setUp() {
		// Clean up any existing test keys before each test
		cleanupTestKeys();
	}

	@AfterEach
	void tearDown() {
		// Clean up test keys after each test
		cleanupTestKeys();
	}

	private void cleanupTestKeys() {
		try {
			UserPreferences.remove(TEST_KEY);
			UserPreferences.remove(TEST_KEY_2);
			UserPreferences.flush();
		}
		catch (Exception e) {
			// Ignore cleanup errors
		}
	}

	@Test
	void file() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> UserPreferences.file("  "));
		assertThrows(IllegalArgumentException.class, () -> UserPreferences.delete("  "));
		try {
			Preferences preferences = UserPreferences.file("UserPreferencesTest");
			preferences.put(TEST_KEY, TEST_VALUE);
		}
		finally {
			UserPreferences.delete("UserPreferencesTest");
		}
	}

	@Nested
	@DisplayName("Basic operations")
	class BasicOperationsTest {

		@Test
		@DisplayName("Get non-existent key returns null")
		void get_nonExistentKey_returnsNull() {
			String result = UserPreferences.get(TEST_KEY);

			assertNull(result);
		}

		@Test
		@DisplayName("Get non-existent key with default returns default")
		void get_nonExistentKeyWithDefault_returnsDefault() {
			String result = UserPreferences.get(TEST_KEY, DEFAULT_VALUE);

			assertEquals(DEFAULT_VALUE, result);
		}

		@Test
		@DisplayName("Set and get value works correctly")
		void set_andGet_worksCorrectly() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);

			String result = UserPreferences.get(TEST_KEY);

			assertEquals(TEST_VALUE, result);
		}

		@Test
		@DisplayName("Set and get with default returns stored value")
		void set_andGetWithDefault_returnsStoredValue() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);

			String result = UserPreferences.get(TEST_KEY, DEFAULT_VALUE);

			assertEquals(TEST_VALUE, result);
		}

		@Test
		@DisplayName("Remove key removes stored value")
		void remove_removesStoredValue() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			assertEquals(TEST_VALUE, UserPreferences.get(TEST_KEY));

			UserPreferences.remove(TEST_KEY);

			assertNull(UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Remove non-existent key has no effect")
		void remove_nonExistentKey_hasNoEffect() {
			assertNull(UserPreferences.get(TEST_KEY));

			assertDoesNotThrow(() -> UserPreferences.remove(TEST_KEY));

			assertNull(UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Update existing value works")
		void update_existingValue_works() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			assertEquals(TEST_VALUE, UserPreferences.get(TEST_KEY));

			UserPreferences.set(TEST_KEY, TEST_VALUE_2);

			assertEquals(TEST_VALUE_2, UserPreferences.get(TEST_KEY));
		}
	}

	@Nested
	@DisplayName("Multiple keys handling")
	class MultipleKeysTest {

		@Test
		@DisplayName("Multiple keys can be stored independently")
		void multipleKeys_storedIndependently() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			UserPreferences.set(TEST_KEY_2, TEST_VALUE_2);

			assertEquals(TEST_VALUE, UserPreferences.get(TEST_KEY));
			assertEquals(TEST_VALUE_2, UserPreferences.get(TEST_KEY_2));
		}

		@Test
		@DisplayName("Removing one key does not affect others")
		void remove_oneKey_doesNotAffectOthers() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			UserPreferences.set(TEST_KEY_2, TEST_VALUE_2);

			UserPreferences.remove(TEST_KEY);

			assertNull(UserPreferences.get(TEST_KEY));
			assertEquals(TEST_VALUE_2, UserPreferences.get(TEST_KEY_2));
		}
	}

	@Nested
	@DisplayName("Null parameter validation")
	class NullParameterValidationTest {

		@Test
		@DisplayName("Get with null key throws NPE")
		void get_nullKey_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.get(null));
		}

		@Test
		@DisplayName("Get with null key and default throws NPE")
		void get_nullKeyWithDefault_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.get(null, DEFAULT_VALUE));
		}

		@Test
		@DisplayName("Get with null default value throws NPE")
		void get_nullDefaultValue_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.get(TEST_KEY, null));
		}

		@Test
		@DisplayName("Set with null key throws NPE")
		void set_nullKey_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.set(null, TEST_VALUE));
		}

		@Test
		@DisplayName("Set with null value throws NPE")
		void set_nullValue_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.set(TEST_KEY, null));
		}

		@Test
		@DisplayName("Remove with null key throws NPE")
		void remove_nullKey_throwsNPE() {
			assertThrows(NullPointerException.class, () -> UserPreferences.remove(null));
		}
	}

	@Nested
	@DisplayName("Edge cases")
	class EdgeCasesTest {

		@Test
		@DisplayName("Empty string key works")
		void emptyStringKey_works() {
			String emptyKey = "";
			UserPreferences.set(emptyKey, TEST_VALUE);

			assertEquals(TEST_VALUE, UserPreferences.get(emptyKey));
		}

		@Test
		@DisplayName("Empty string value works")
		void emptyStringValue_works() {
			String emptyValue = "";
			UserPreferences.set(TEST_KEY, emptyValue);

			assertEquals(emptyValue, UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Long key within limits works")
		void longKeyWithinLimits_works() {
			// Java Preferences typically has 80 character limit for keys
			String longKey = "very.long.key." + Text.leftPad("", 60, 'a');
			UserPreferences.set(longKey, TEST_VALUE);

			assertEquals(TEST_VALUE, UserPreferences.get(longKey));

			// Cleanup
			UserPreferences.remove(longKey);
		}

		@Test
		@DisplayName("Very long value works")
		void veryLongValue_works() {
			String longValue = "very.long.value." + Text.leftPad("", 1000, 'b');
			UserPreferences.set(TEST_KEY, longValue);

			assertEquals(longValue, UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Special characters in key work")
		void specialCharactersInKey_work() {
			String specialKey = "test.key.with-special_chars@#$%";
			UserPreferences.set(specialKey, TEST_VALUE);

			assertEquals(TEST_VALUE, UserPreferences.get(specialKey));

			// Cleanup
			UserPreferences.remove(specialKey);
		}

		@Test
		@DisplayName("Special characters in value work")
		void specialCharactersInValue_work() {
			String specialValue = "test.value.with-special_chars@#$%^&*()[]{}";
			UserPreferences.set(TEST_KEY, specialValue);

			assertEquals(specialValue, UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Unicode characters work")
		void unicodeCharacters_work() {
			String unicodeKey = "test.unicode.key.こんにちは.ñ.ü";
			String unicodeValue = "test.unicode.value.世界.ñoñó.ümlaut";

			UserPreferences.set(unicodeKey, unicodeValue);

			assertEquals(unicodeValue, UserPreferences.get(unicodeKey));

			// Cleanup
			UserPreferences.remove(unicodeKey);
		}
	}

	@Nested
	@DisplayName("Flush operation")
	class FlushOperationTest {

		@Test
		@DisplayName("Flush does not throw exception")
		void flush_doesNotThrowException() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);

			assertDoesNotThrow(() -> UserPreferences.flush());
		}

		@Test
		@DisplayName("Flush after set persists data")
		void flush_afterSet_persistsData() throws BackingStoreException {
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			UserPreferences.flush();

			// Data should still be there after flush
			assertEquals(TEST_VALUE, UserPreferences.get(TEST_KEY));
		}

		@Test
		@DisplayName("Multiple flush calls work")
		void multipleFlusCalls_work() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);

			assertDoesNotThrow(() -> {
				UserPreferences.flush();
				UserPreferences.flush();
				UserPreferences.flush();
			});
		}

		@Test
		@DisplayName("Flush on empty preferences works")
		void flush_onEmptyPreferences_works() {
			assertDoesNotThrow(() -> UserPreferences.flush());
		}
	}

	@Nested
	@DisplayName("Thread safety")
	class ThreadSafetyTest {

		private static final int THREAD_COUNT = 10;
		private static final int OPERATIONS_PER_THREAD = 100;

		@Test
		@DisplayName("Concurrent set operations are thread-safe")
		void concurrentSet_isThreadSafe() throws InterruptedException {
			ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

			for (int i = 0; i < THREAD_COUNT; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();
						for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
							String key = "thread." + threadId + ".key." + j;
							String value = "thread." + threadId + ".value." + j;
							UserPreferences.set(key, value);
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

			// Verify some values were set correctly
			for (int i = 0; i < THREAD_COUNT; i++) {
				String key = "thread." + i + ".key.0";
				String expectedValue = "thread." + i + ".value.0";
				assertEquals(expectedValue, UserPreferences.get(key));
			}

			// Cleanup
			for (int i = 0; i < THREAD_COUNT; i++) {
				for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
					try {
						UserPreferences.remove("thread." + i + ".key." + j);
					}
					catch (Exception e) {
						// Ignore cleanup errors
					}
				}
			}

			executor.shutdown();
		}

		@Test
		@DisplayName("Concurrent get operations are thread-safe")
		void concurrentGet_isThreadSafe() throws InterruptedException {
			// Set up some test data
			for (int i = 0; i < THREAD_COUNT; i++) {
				UserPreferences.set("concurrent.key." + i, "concurrent.value." + i);
			}

			ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
			AtomicInteger successCount = new AtomicInteger(0);

			for (int i = 0; i < THREAD_COUNT; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();
						for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
							String key = "concurrent.key." + (threadId % THREAD_COUNT);
							String value = UserPreferences.get(key);
							if (value != null && value.startsWith("concurrent.value.")) {
								successCount.incrementAndGet();
							}
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

			// Should have read many values successfully
			assertTrue(successCount.get() > 0);

			// Cleanup
			for (int i = 0; i < THREAD_COUNT; i++) {
				try {
					UserPreferences.remove("concurrent.key." + i);
				}
				catch (Exception e) {
					// Ignore cleanup errors
				}
			}

			executor.shutdown();
		}

		@Test
		@DisplayName("Mixed concurrent operations are thread-safe")
		void mixedConcurrentOperations_areThreadSafe() throws InterruptedException {
			ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

			for (int i = 0; i < THREAD_COUNT; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();
						for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
							String key = "mixed.key." + threadId;
							String value = "mixed.value." + threadId + "." + j;

							UserPreferences.set(key, value);
							String retrieved = UserPreferences.get(key);

							if (j % 2 == 0) {
								UserPreferences.remove(key);
								retrieved = UserPreferences.get(key);
								assertNull(retrieved);
							}
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

			// Cleanup
			for (int i = 0; i < THREAD_COUNT; i++) {
				try {
					UserPreferences.remove("mixed.key." + i);
				}
				catch (Exception e) {
					// Ignore cleanup errors
				}
			}

			executor.shutdown();
		}
	}

	@Nested
	@DisplayName("Java Preferences integration")
	class JavaPreferencesIntegrationTest {

		@Test
		@DisplayName("UserPreferences integrates with Java Preferences")
		void integrationWithJavaPreferences() {
			UserPreferences.set(TEST_KEY, TEST_VALUE);

			// Verify through direct Java Preferences API
			Preferences prefs = Preferences.userRoot();
			String directValue = prefs.get(TEST_KEY, null);

			assertEquals(TEST_VALUE, directValue);
		}

		@Test
		@DisplayName("Direct Java Preferences changes are visible through UserPreferences")
		void directJavaPreferencesChanges_visibleThroughUserPreferences() {
			// Set value through direct Java Preferences API
			Preferences prefs = Preferences.userRoot();
			prefs.put(TEST_KEY, TEST_VALUE);

			// Verify through UserPreferences
			String userPrefValue = UserPreferences.get(TEST_KEY);

			assertEquals(TEST_VALUE, userPrefValue);
		}

		@Test
		@DisplayName("Singleton behavior maintains same preferences instance")
		void singletonBehavior_maintainsSameInstance() {
			// This test verifies that multiple accesses use the same underlying preferences
			UserPreferences.set(TEST_KEY, TEST_VALUE);
			String value1 = UserPreferences.get(TEST_KEY);

			UserPreferences.set(TEST_KEY_2, TEST_VALUE_2);
			String value2 = UserPreferences.get(TEST_KEY);
			String value3 = UserPreferences.get(TEST_KEY_2);

			assertEquals(TEST_VALUE, value1);
			assertEquals(TEST_VALUE, value2);
			assertEquals(TEST_VALUE_2, value3);
		}
	}
}