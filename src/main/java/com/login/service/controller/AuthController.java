package com.login.service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.login.service.constants.Constants;
import com.login.service.dto.JwtResponse;
import com.login.service.dto.LoginRequest;
import com.login.service.dto.RegisterRequest;
import com.login.service.dto.ResponseMessage;
import com.login.service.model.UserDocument;
import com.login.service.repository.UserRepository;
import com.login.service.util.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("${api.base-path}")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new ResponseMessage(Constants.USERNAME_ALREADY_EXISTS));
        }

        UserDocument user = new UserDocument();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(List.of("ROLE_USER"));

        userRepository.save(user);
        return ResponseEntity.ok(new ResponseMessage(Constants.USER_REGISTERED_SUCCESSFULLY));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDocument userDetails = (UserDocument) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(jwt));
    }
}
