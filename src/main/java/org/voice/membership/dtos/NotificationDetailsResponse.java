package org.voice.membership.dtos;

import lombok.Builder;

import java.util.List;

@Builder
public record NotificationDetailsResponse(NotificationDTO notification, List<SimpleUserResponse> members) {
}
