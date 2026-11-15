package org.AirTortilla;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository users
    ) {
        this.authenticationManager = authenticationManager;
        this.users = users;
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record UserInfo(String username, String role) {
    }

    @PostMapping("/login")
    public UserInfo login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSessionSecurityContextRepository contextRepository =
                    new HttpSessionSecurityContextRepository();
            contextRepository.saveContext(
                    context,
                    servletRequest,
                    servletResponse
            );

            AppUser user = users.findByUsername(request.username())
                    .orElseThrow();
            return new UserInfo(user.getUsername(), user.getRole().name());
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales incorrectas"
            );
        }
    }

    @GetMapping("/me")
    public UserInfo currentUser(Authentication authentication) {
        AppUser user = users.findByUsername(authentication.getName())
                .orElseThrow();
        return new UserInfo(user.getUsername(), user.getRole().name());
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        SecurityContextHolder.clearContext();

        if (session != null) {
            session.invalidate();
        }
    }
}
