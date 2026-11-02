package com.backend.finledger.user;

import com.backend.finledger.user.dto.PermissionDto;
import com.backend.finledger.user.dto.RoleDto;
import com.backend.finledger.user.dto.RoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<RoleDto>> listRoles() {
        return ResponseEntity.ok(userService.getRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getRoleById(id));
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> listPermissions() {
        return ResponseEntity.ok(userService.getAllPermissions());
    }
}

