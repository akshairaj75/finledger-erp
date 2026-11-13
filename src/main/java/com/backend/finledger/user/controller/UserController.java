package com.backend.finledger.user.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.user.dto.CreateUserRequest;
import com.backend.finledger.user.dto.UpdateUserRequest;
import com.backend.finledger.user.dto.UserDto;
import com.backend.finledger.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_VIEW','SETTINGS_EDIT','EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','LEAD_VIEW','OPPORTUNITY_VIEW')")
    public ResponseEntity<PageResponse<UserDto>> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
}

