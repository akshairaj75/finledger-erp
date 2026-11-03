package com.backend.finledger.user.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.user.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    PageResponse<UserDto> getUsers(Pageable pageable);
    UserDto getUserById(Long id);
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(Long id, UpdateUserRequest request);
    List<RoleDto> getRoles();
    RoleDto getRoleById(Long id);
    RoleDto createRole(RoleRequest request);
    RoleDto updateRole(Long id, RoleRequest request);
    List<PermissionDto> getAllPermissions();
}

