package com.tskrypko.authentication.dto;

import com.tskrypko.authentication.model.RolePermission;
import java.util.Map;
import java.util.Set;

public record UserAuthDto(
        String token,
        String username,
        String email,
        Set<String> roles,
        Map<String, RolePermission.Level> permissions
) {}
