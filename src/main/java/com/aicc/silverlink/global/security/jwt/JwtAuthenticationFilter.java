package com.aicc.silverlink.global.security.jwt;

import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveBearer(request);

        if (token != null) {
            try {
                Jws<Claims> jws = jwt.parseAndValidate(token); // 토큰 서명 검증 + 만료 체크

                Claims claims = jws.getPayload();

                String userIdStr = claims.getSubject();
                String roleStr = claims.get("role", String.class);
                String sid = claims.get("sid", String.class);

                Long userId = Long.valueOf(userIdStr);
                Role role = Role.valueOf(roleStr);

                if (!sessionService.isActive(sid, userId)) {
                    logger.warn("Session expired or invalid for sid: {}");

                    filterChain.doFilter(request, response);
                    return;
                }

                // idle 연장
                sessionService.touch(sid);

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

            } catch (JwtException | IllegalArgumentException e) {
                logger.error("Invalid JWT Token: {}");
            } catch (Exception e) {
                logger.error("Authentication Error: {}");
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveBearer(HttpServletRequest req){
        String h = req.getHeader("Authorization");
        if (h == null || h.isBlank()) return null;

        // "Bearer " 대소문자/공백 약간 방어하고 싶으면 더 탄탄하게 처리
        if (!h.startsWith("Bearer ")) return null;

        String token = h.substring(7).trim();
        return token.isEmpty() ? null : token;
    }



}