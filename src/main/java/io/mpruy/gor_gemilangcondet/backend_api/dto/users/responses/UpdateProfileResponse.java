package io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import lombok.Builder;
import lombok.Data;

/**
 * Response for PUT /users/profile.
 * Carries the updated user profile together with fresh auth tokens
 * so the frontend can seamlessly continue without re-login when the
 * username (JWT subject) changes.
 */
@Data
@Builder
public class UpdateProfileResponse {

    private UserDto user;

    /** Fresh JWT access token signed with the new username. */
    private String accessToken;

    /** Fresh refresh token mapped to the new username. */
    private String refreshToken;
}
