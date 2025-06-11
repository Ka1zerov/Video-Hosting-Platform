package com.tskrypko.authentication.dto;

import com.tskrypko.authentication.model.RolePermission;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
public class UserDetailsDto implements UserDetails {
    @Getter
    private UUID id;
    private String username;
    private String password;
    private String email;
    private Set<String> roles;
    private Map<String, RolePermission.Level> permissions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.entrySet().stream()
                .filter(entry -> entry.getValue() == RolePermission.Level.ALLOW)
                .map(entry -> new SimpleGrantedAuthority(entry.getKey()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
