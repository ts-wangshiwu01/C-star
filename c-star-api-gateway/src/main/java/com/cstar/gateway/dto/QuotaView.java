package com.cstar.gateway.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Current period remaining-quota view. Mirrors the frontend {@code QuotaView}
 * (c-star-frontend/src/types/api.ts).
 *
 * <p>Note: the frontend's {@code QuotaView} type names the default-quota field
 * {@code default} (a Java reserved word, so the record component is
 * {@code defaultQuota}). The frontend normalizer (quota.ts) reads the field
 * defensively as {@code default ?? defaultQuota ?? default_quota}, so emitting
 * {@code defaultQuota} is accepted verbatim by the frontend.
 *
 * @param remaining   red flowers the current user can still give this period
 * @param defaultQuota period default quota (deployment config)
 * @param consumed     already consumed this period
 * @param periodDate   period start/current date (ISO date, YYYY-MM-DD)
 */
@Serdeable
public record QuotaView(
        long remaining,
        long defaultQuota,
        long consumed,
        String periodDate
) {
}