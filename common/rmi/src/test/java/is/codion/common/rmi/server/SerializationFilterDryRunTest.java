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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SerializationFilterDryRunTest {

	@Test
	void dryRun() throws IOException, ClassNotFoundException {
		assertThrows(IllegalArgumentException.class, () -> SerializationFilterDryRun.whitelistDryRun("classpath:dryrun").writeToFile());
		File tempFile = File.createTempFile("serialization_dry_run_test", "txt");

		SerializationFilterDryRun.DryRun serialFilter = SerializationFilterDryRun.whitelistDryRun(tempFile.getAbsolutePath());
		ObjectInputFilter.Config.setSerialFilter(serialFilter);

		Serializer.deserialize(Serializer.serialize(Integer.valueOf(42)));
		Serializer.deserialize(Serializer.serialize(Long.valueOf(42)));
		serialFilter.writeToFile();

		List<String> classNames = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);

		assertEquals(3, classNames.size());
		assertEquals(Integer.class.getName(), classNames.get(0));
		assertEquals(Long.class.getName(), classNames.get(1));
		assertEquals(Number.class.getName(), classNames.get(2));

		Serializer.deserialize(Serializer.serialize(Double.valueOf(42)));
		Serializer.deserialize(Serializer.serialize(new Double[] {42d}));
		serialFilter.writeToFile();

		classNames = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);

		assertEquals(4, classNames.size());
		assertEquals(Double.class.getName(), classNames.get(0));
		assertEquals(Integer.class.getName(), classNames.get(1));
		assertEquals(Long.class.getName(), classNames.get(2));
		assertEquals(Number.class.getName(), classNames.get(3));

		tempFile.delete();
	}
}
