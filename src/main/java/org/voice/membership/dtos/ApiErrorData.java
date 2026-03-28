package org.voice.membership.dtos;

import lombok.Builder;

import java.util.Map;

/**
 * Standard error payload for API responses.
 */
@Builder
public record ApiErrorData(String errorCode, Map<String, String> fieldErrors) {
}
