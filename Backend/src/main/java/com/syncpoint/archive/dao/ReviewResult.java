package com.syncpoint.archive.dao;

/** Outcome of a "review if still pending" operation. */
public enum ReviewResult {
    REVIEWED,
    NOT_FOUND,
    NOT_PENDING
}
