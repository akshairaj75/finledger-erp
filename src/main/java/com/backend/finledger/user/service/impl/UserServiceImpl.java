package com.backend.finledger.user.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.user.dto.*;
import com.backend.finledger.user.entity.Permission;
import com.backend.finledger.user.entity.Role;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.entity.UserStatus;
import com.backend.finledger.user.mapper.UserMapper;
import com.backend.finledger.user.repository.PermissionRepository;
import com.backend.finledger.user.repository.RoleRepository;
import com.backend.finledger.user.repository.UserRepository;
import com.backend.finledger.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public PageResponse<UserDto> getUsers(Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        return PageResponse.from(userRepository.findByCompanyId(companyId, pageable).map(userMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public UserDto getUserById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        User user = userRepository.findById(id)
                .filter(u -> u.getCompanyId().equals(companyId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('SETTINGS_EDIT')")
    public UserDto createUser(CreateUserRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "A user with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setCompanyId(companyId);
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedAttempts((byte) 0);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.roleIds());
            roles.removeIf(r -> !r.getCompanyId().equals(companyId));
            user.setRoles(new HashSet<>(roles));
        }

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        User user = userRepository.findById(id)
                .filter(u -> u.getCompanyId().equals(companyId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found with id: " + id));

        user.setFullName(request.fullName().trim());
        if (request.status() != null) {
            user.setStatus(request.status());
            if (request.status() == UserStatus.ACTIVE) {
                user.setFailedAttempts((byte) 0);
            }
        }

        if (request.roleIds() != null) {
            List<Role> roles = roleRepository.findAllById(request.roleIds());
            roles.removeIf(r -> !r.getCompanyId().equals(companyId));
            user.setRoles(new HashSet<>(roles));
        }

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public List<RoleDto> getRoles() {
        Long companyId = CurrentUser.getCompanyId();
        return roleRepository.findByCompanyId(companyId).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public RoleDto getRoleById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Role role = roleRepository.findById(id)
                .filter(r -> r.getCompanyId().equals(companyId))
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role not found with id: " + id));
        return userMapper.toDto(role);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public RoleDto createRole(RoleRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        if (roleRepository.findByCompanyIdAndName(companyId, request.name()).isPresent()) {
            throw new BusinessException("ROLE_NAME_EXISTS", "A role named " + request.name() + " already exists in this company");
        }

        Role role = new Role(companyId, request.name().trim(), request.description());
        if (request.permissionCodes() != null) {
            Set<Permission> perms = new HashSet<>();
            for (String code : request.permissionCodes()) {
                permissionRepository.findByCode(code).ifPresent(perms::add);
            }
            role.setPermissions(perms);
        }

        Role saved = roleRepository.save(role);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public RoleDto updateRole(Long id, RoleRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Role role = roleRepository.findById(id)
                .filter(r -> r.getCompanyId().equals(companyId))
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role not found with id: " + id));

        role.setName(request.name().trim());
        role.setDescription(request.description());

        if (request.permissionCodes() != null) {
            Set<Permission> perms = new HashSet<>();
            for (String code : request.permissionCodes()) {
                permissionRepository.findByCode(code).ifPresent(perms::add);
            }
            role.setPermissions(perms);
        }

        Role saved = roleRepository.save(role);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}

