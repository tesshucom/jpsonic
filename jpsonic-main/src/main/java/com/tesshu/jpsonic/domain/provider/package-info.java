/**
 * Domain providers for read operations.
 *
 * <p>
 * In conventional designs, services typically combine CRUD operations. This
 * media server has a particularly high proportion of read operations. Providers
 * therefore separate read operations from create, update, and delete operations
 * and organize them according to their domain role:
 *
 * <ul>
 * <li>{@code resource}: what to retrieve or operate on.</li>
 * <li>{@code master}: what to classify or aggregate from current data.</li>
 * <li>{@code search}: what to find across resources from search criteria.</li>
 * </ul>
 */
package com.tesshu.jpsonic.domain.provider;
