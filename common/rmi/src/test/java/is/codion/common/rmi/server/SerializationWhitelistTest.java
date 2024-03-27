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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.Serializer;
import is.codion.common.rmi.server.SerializationWhitelist.WhitelistFilter;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SerializationWhitelistTest {

	@Test
	void dryRun() throws IOException, ClassNotFoundException {
		assertThrows(IllegalArgumentException.class, () -> SerializationWhitelist.whitelistDryRun().writeToFile("classpath:dryrun"));
		File tempFile = File.createTempFile("serialization_dry_run_test", "txt");

		SerializationWhitelist.DryRun serialFilter = SerializationWhitelist.whitelistDryRun();
		ObjectInputFilter.Config.setSerialFilter(serialFilter);

		Serializer.deserialize(Serializer.serialize(Integer.valueOf(42)));
		Serializer.deserialize(Serializer.serialize(Long.valueOf(42)));
		serialFilter.writeToFile(tempFile.getAbsolutePath());

		List<String> classNames = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);

		assertEquals(3, classNames.size());
		assertEquals(Integer.class.getName(), classNames.get(0));
		assertEquals(Long.class.getName(), classNames.get(1));
		assertEquals(Number.class.getName(), classNames.get(2));

		Serializer.deserialize(Serializer.serialize(Double.valueOf(42)));
		Serializer.deserialize(Serializer.serialize(new Double[] {42d}));
		serialFilter.writeToFile(tempFile.getAbsolutePath());

		classNames = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);

		assertEquals(4, classNames.size());
		assertEquals(Double.class.getName(), classNames.get(0));
		assertEquals(Integer.class.getName(), classNames.get(1));
		assertEquals(Long.class.getName(), classNames.get(2));
		assertEquals(Number.class.getName(), classNames.get(3));

		tempFile.delete();
	}

	@Test
	void testNoWildcards() {
		List<String> whitelistItems = asList(
						"#comment",
						"is.codion.common.value.Value",
						"is.codion.common.state.State",
						"is.codion.common.state.StateObserver"
		);
		WhitelistFilter filter = SerializationWhitelist.whitelistFilter(whitelistItems);
		assertEquals(filter.checkInput("is.codion.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.state.State"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.state.States"), ObjectInputFilter.Status.REJECTED);
		assertEquals(filter.checkInput("is.codion.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
		assertEquals(filter.checkInput("is.codion.common.i18n.Messages"), ObjectInputFilter.Status.REJECTED);
	}

	@Test
	void file() {
		assertThrows(RuntimeException.class, () -> SerializationWhitelist.whitelistFilter("src/test/resources/whitelist_test_non_existing.txt"));
		testFilter(SerializationWhitelist.whitelistFilter("src/test/resources/whitelist_test.txt"));
	}

	@Test
	void classpath() {
		assertThrows(IllegalArgumentException.class, () -> SerializationWhitelist.whitelistFilter("classpath:src/test/resources/whitelist_test.txt"));
		testFilter(SerializationWhitelist.whitelistFilter("classpath:whitelist_test.txt"));
		testFilter(SerializationWhitelist.whitelistFilter("classpath:/whitelist_test.txt"));
	}

	@Test
	void array() throws IOException, ClassNotFoundException {
		List<String> whitelistItems = asList(
						"java.lang.Number",
						"java.lang.Byte"
		);
		WhitelistFilter filter = SerializationWhitelist.whitelistFilter(whitelistItems);
		assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkArrayInput(new byte[0].getClass()));
		assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkArrayInput(new byte[0][].getClass()));
		assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkArrayInput(new Byte[0].getClass()));
		assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkArrayInput(new Byte[0][].getClass()));
		assertEquals(ObjectInputFilter.Status.REJECTED, filter.checkArrayInput(new Double[0].getClass()));
	}

	private static void testFilter(WhitelistFilter filter) {
		assertEquals(filter.checkInput("is.codion.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.state.State"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.state.States"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
		assertEquals(filter.checkInput("is.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
		assertEquals(filter.checkInput("is.codion.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
	}
}
