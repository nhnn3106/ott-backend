package iuh.fit.ottbackend.controller;

import iuh.fit.ottbackend.dto.request.RegisterRequest;
import iuh.fit.ottbackend.dto.response.ApiResponse;
import iuh.fit.ottbackend.dto.response.UserResponse;
import iuh.fit.ottbackend.service.UserService;
import iuh.fit.ottbackend.utils.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final ControllerUtils controllerUtils;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        UserResponse response = userService.register(request);

        log.info("New user registered with phone: {}", request.getPhone());

        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Account created successfully. Please login to continue.")
                .result(response)
                .build();
    }
}