package com.tesshu.jpsonic.domain.system;

/**
 * Represents the “application-level generation” of the Lucene index managed by
 * Jpsonic.
 *
 * <p>
 * This generation number is distinct from Lucene’s internal codec version, but
 * it is incremented whenever Lucene introduces a backward-incompatible codec
 * change. In such cases, the generation acts as a trigger to ensure that
 * Jpsonic maintains a safe and consistent index structure. Therefore, while not
 * identical to Lucene’s internal versioning, it is not unrelated to Lucene’s
 * compatibility model and is used as part of the application’s generation
 * management.
 * </p>
 *
 * <p>
 * The generation number is a fixed value updated manually at Jpsonic release
 * time, and represents the “application-defined index generation,” including
 * changes to the index schema or directory layout.
 * </p>
 *
 * <p>
 * The EnvironmentProvider uses this generation number to determine the index
 * directory and to clean up obsolete generations at startup. The IndexManager
 * simply performs CRUD operations against the directory associated with the
 * current generation and does not hold any knowledge about generation
 * management or directory structure.
 * </p>
 *
 * <p>
 * This enum serves as the single source of truth for index generation
 * management within Jpsonic, safely absorbing both Lucene compatibility issues
 * and application-level changes.
 * </p>
 */
public record IndexGeneration(int value) {
    public static final IndexGeneration CURRENT = new IndexGeneration(31);
}
