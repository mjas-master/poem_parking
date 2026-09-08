package com.poem.parking.config

import com.poem.parking.repository.ApartmentRepository
import com.poem.parking.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

/** 인증된 주체: 앱 사용자 또는 PC 연동 프로그램 */
sealed interface Principal {
    data class AppUser(val userId: Long, val phone: String) : Principal
    data class Connector(val apartmentCode: String) : Principal
}

@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val claims = jwtProvider.parse(header.substring(7))
            if (claims != null) {
                val userId = claims.subject.toLong()
                val user = userRepository.findById(userId).orElse(null)
                if (user != null) {
                    val principal = Principal.AppUser(userId, user.phone)
                    val auth = UsernamePasswordAuthenticationToken(
                        principal, null, listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
        chain.doFilter(req, res)
    }
}

@Component
class ConnectorApiKeyFilter(
    private val props: AppProperties,
    private val apartmentRepository: ApartmentRepository,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest) = !request.requestURI.startsWith("/api/v1/connector")

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val apiKey = req.getHeader("X-Connector-Key")
        val aptCode = req.getHeader("X-Apartment-Code")
        if (apiKey.isNullOrBlank() || aptCode.isNullOrBlank() || props.connector.apiKeys[aptCode] != apiKey) {
            res.status = HttpStatus.UNAUTHORIZED.value()
            res.contentType = "application/json"
            res.writer.write("""{"code":"CONNECTOR_UNAUTHORIZED","message":"연동 키가 올바르지 않습니다."}""")
            return
        }
        val auth = UsernamePasswordAuthenticationToken(
            Principal.Connector(aptCode), null, listOf(SimpleGrantedAuthority("ROLE_CONNECTOR"))
        )
        SecurityContextHolder.getContext().authentication = auth
        chain.doFilter(req, res)
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val connectorApiKeyFilter: ConnectorApiKeyFilter,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { it.frameOptions { f -> f.sameOrigin() } } // H2 console
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/auth/**", "/h2/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                it.requestMatchers("/api/v1/connector/**").hasRole("CONNECTOR")
                it.requestMatchers("/api/v1/**").hasAnyRole("RESIDENT", "ADMIN")
                it.anyRequest().denyAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, res, _ ->
                    res.status = 401
                    res.contentType = "application/json"
                    res.writer.write("""{"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}""")
                }
            }
            .addFilterBefore(connectorApiKeyFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun corsSource(): UrlBasedCorsConfigurationSource {
        val cfg = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", cfg) }
    }
}
