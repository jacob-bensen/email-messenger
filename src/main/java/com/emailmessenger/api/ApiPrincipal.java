package com.emailmessenger.api;

/** The authenticated caller of an {@code /api/v1} request. */
record ApiPrincipal(long userId, String email, long tokenId) {}
