/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collection;
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

  private SerializationWhitelist() {}

  static void configureSerializationWhitelist(final String whitelist, final Boolean dryRun) {
    if (!Util.nullOrEmpty(whitelist)) {
      try (final Stream<String> stream = Files.lines(Paths.get(whitelist))) {
        sun.misc.ObjectInputFilter.Config.setSerialFilter(dryRun ? new SerializationFilterDryRun() : new SerializationFilter(stream.collect(Collectors.toSet())));
        LOG.debug("Serialization filter whitelist set: " + whitelist + " (dry run: " + dryRun + ")");
      }
      catch (final IOException e) {
        LOG.error("Unable to read serialization whitelist: " + whitelist);
        throw new RuntimeException(e);
      }
    }
  }

  static void writeSerializationWhitelist(final String whitelist) {
    final sun.misc.ObjectInputFilter serialFilter = sun.misc.ObjectInputFilter.Config.getSerialFilter();
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

  private static final class SerializationFilterDryRun implements sun.misc.ObjectInputFilter {

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

  static final class SerializationFilter implements sun.misc.ObjectInputFilter {

    private static final String WILDCARD = "*";

    private final Set<String> allowedClassnames = new HashSet<>();
    private final List<String> allowedWildcardClassnames = new ArrayList<>();

    SerializationFilter(final Collection<String>whitelistItems) {
      whitelistItems.forEach(whitelistItem -> {
        if (whitelistItem.endsWith(WILDCARD)) {
          allowedWildcardClassnames.add(whitelistItem.substring(0, whitelistItem.length() - 1));
        }
        else {
          allowedClassnames.add(whitelistItem);
        }
      });
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
      final Class clazz = filterInfo.serialClass();
      if (clazz == null) {
        return Status.ALLOWED;
      }

      return checkInput(clazz.getName());
    }

    Status checkInput(final String classname) {
      if (allowedClassnames.contains(classname) || allowWildcard(classname)) {
        return Status.ALLOWED;
      }
      LOG.debug("Serialization rejected: " + classname);

      return Status.REJECTED;
    }

    private boolean allowWildcard(final String classname) {
      if (allowedWildcardClassnames.isEmpty()) {
        return true;
      }

      for (int i = 0; i < allowedWildcardClassnames.size(); i++) {
        if (classname.startsWith(allowedWildcardClassnames.get(i))) {
          return true;
        }
      }

      return false;
    }
  }
}
