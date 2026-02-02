package org.voice.membership.entities;

/**
 * Enumerates the security roles available in the application.
 * Used by Spring Security to distinguish regular users and admins.
 */
public enum Role {
    USER,
    ADMIN
}

