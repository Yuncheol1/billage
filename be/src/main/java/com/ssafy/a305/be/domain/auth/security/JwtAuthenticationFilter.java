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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("removal")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Bearer 검증 없이 통과할 공개 API (헬스, 로그인·토큰 교환 등).
     * {@code /api/v1/auth/consents} 등 인증 필요 엔드포인트는 포함하지 않음.
     */
    private static final List<AntPathRequestMatcher> JWT_FILTER_WHITELIST = List.of(
            new AntPathRequestMatcher("/api/v1/health"),
            new AntPathRequestMatcher("/api/v1/auth/kakao/authorize"),
            new AntPathRequestMatcher("/api/v1/auth/kakao/callback"),
            new AntPathRequestMatcher("/api/v1/auth/kakao/login"),
            new AntPathRequestMatcher("/api/v1/auth/reissue"),
            new AntPathRequestMatcher("/api/v1/auth/logout")
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return JWT_FILTER_WHITELIST.stream()
                .anyMatch(matcher -> matcher.matches(request));
    }

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
