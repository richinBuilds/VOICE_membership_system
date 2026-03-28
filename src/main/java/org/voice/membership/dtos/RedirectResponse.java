package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record RedirectResponse(boolean success, String redirectUrl) {
}
