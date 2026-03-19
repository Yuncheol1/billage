package com.ssafy.a305.be.domain.auth.security;

import com.ssafy.a305.be.domain.auth.util.JwtTokenProvider;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String accessToken = resolveAccessToken(request);

            if (accessToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                jwtTokenProvider.validateAccessToken(accessToken);
                Integer userSeq = jwtTokenProvider.extractUserSeq(accessToken);

                User user = userRepository.findById(userSeq)
                        .orElseThrow(() -> new BadCredentialsException("인증 사용자를 찾을 수 없습니다."));

                if ("DELETED".equals(user.getState()) || "BLOCKED".equals(user.getState())) {
                    throw new DisabledException("로그인할 수 없는 사용자 상태입니다.");
                }

                AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                if (user.getState() != null && !user.getState().isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getState()));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authenticatedUser, accessToken, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            jwtAuthenticationEntryPoint.commence(request, response, e);
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InsufficientAuthenticationException("Bearer 토큰이 필요합니다.");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
