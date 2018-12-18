/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.FileUtil;
import org.jminor.common.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a serialization whitelist for Java 8
 */
public final class SerializationWhitelist {

  protected static final Logger LOG = LoggerFactory.getLogger(SerializationWhitelist.class);

  static void configureSerializationWhitelist(final String whitelist, final Boolean dryRun) {
    if (!Util.nullOrEmpty(whitelist)) {
      java.io.ObjectInputFilter.Config.setSerialFilter(dryRun ? new SerializationFilterDryRun() : new SerializationFilter(whitelist));
      LOG.debug("Serialization filter whitelist set: " + whitelist + " (dry run: " + dryRun + ")");
    }
  }

  static void writeSerializationWhitelist(final String whitelist) {
    final java.io.ObjectInputFilter serialFilter = java.io.ObjectInputFilter.Config.getSerialFilter();
    if (serialFilter instanceof SerializationFilterDryRun) {
      writeDryRunWhitelist(whitelist, (SerializationFilterDryRun) serialFilter);
    }
  }

  private static void writeDryRunWhitelist(final String whitelist, final SerializationFilterDryRun serialFilter) {
    if (!Util.nullOrEmpty(whitelist)) {
      try {
        final File file = new File(whitelist);
        file.createNewFile();
        FileUtil.writeFile(serialFilter.deserializedClasses.stream().map(Class::getName).sorted()
                .collect(Collectors.joining(Util.LINE_SEPARATOR)), file);
        LOG.debug("Serialization whitelist written: " + whitelist);
      }
      catch (final Exception e) {
        LOG.error("Error while writing serialization filter dry run results", e);
      }
    }
  }

  private static final class SerializationFilterDryRun implements java.io.ObjectInputFilter {

    private final Set<Class> deserializedClasses = new HashSet<>();

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
      final Class clazz = filterInfo.serialClass();
      if (clazz != null) {
        deserializedClasses.add(clazz);
      }

      return Status.ALLOWED;
    }
  }

  private static final class SerializationFilter implements java.io.ObjectInputFilter {

    private final List<String> allowedClassnames;

    private SerializationFilter(final String whitelist) {
      try (final Stream<String> stream = Files.lines(Paths.get(whitelist))) {
        this.allowedClassnames = stream.collect(Collectors.toList());
      }
      catch (final IOException e) {
        LOG.error("Unable to read serialization whitelist: " + whitelist);
        throw new RuntimeException(e);
      }
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
      final Class clazz = filterInfo.serialClass();
      if (clazz != null && !allowedClassnames.contains(clazz.getName())) {
        LOG.debug("Serialization rejected: " + clazz.getName());
        return Status.REJECTED;
      }

      return Status.ALLOWED;
    }
  }
}
