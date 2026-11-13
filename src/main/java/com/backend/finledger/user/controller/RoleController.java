package com.backend.finledger.user.controller;

import com.backend.finledger.user.dto.PermissionDto;
import com.backend.finledger.user.dto.RoleDto;
import com.backend.finledger.user.dto.RoleRequest;
import com.backend.finledger.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<List<RoleDto>> listRoles() {
        return ResponseEntity.ok(userService.getRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<RoleDto> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getRoleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createRole(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<List<PermissionDto>> listPermissions() {
        return ResponseEntity.ok(userService.getAllPermissions());
    }
}

