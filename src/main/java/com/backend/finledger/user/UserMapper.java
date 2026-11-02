package com.backend.finledger.user;

import com.backend.finledger.user.dto.PermissionDto;
import com.backend.finledger.user.dto.RoleDto;
import com.backend.finledger.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserDto toDto(User user);

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permissionsToCodes")
    RoleDto toDto(Role role);

    PermissionDto toDto(Permission permission);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    @Named("permissionsToCodes")
    default Set<String> permissionsToCodes(Set<Permission> permissions) {
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
    }
}

