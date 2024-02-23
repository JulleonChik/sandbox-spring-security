package pro.julleon.sandbox;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.Map;

@SpringBootApplication
public class SandboxSpringSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxSpringSecurityApplication.class, args);
    }


    /*
AuthenticationEntryPoint - это интерфейс в Spring Security, предназначенный для обработки ситуаций,
когда пользователь не прошел аутентификацию, но доступ к защищенному ресурсу требуется.
Этот интерфейс определяет метод commence, который вызывается, когда происходит подобная ситуация.

public interface AuthenticationEntryPoint {
    void commence(HttpServletRequest request, HttpServletResponse response,
                  AuthenticationException authException) throws IOException, ServletException;
}
commence: Метод вызывается, когда пользователь пытается получить доступ к защищенному ресурсу без аутентификации.
Этот метод принимает HttpServletRequest, HttpServletResponse и AuthenticationException.
Разработчик может использовать эти параметры для выполнения необходимых действий,
таких как перенаправление пользователя на страницу входа или отправка пользователю сообщения об ошибке.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // Создаем объект BasicAuthenticationEntryPoint для обработки базовой аутентификации
        BasicAuthenticationEntryPoint authenticationEntryPoint = new BasicAuthenticationEntryPoint();

        // Устанавливаем имя области (Realm) для объекта BasicAuthenticationEntryPoint
        authenticationEntryPoint.setRealmName("Realm");

        // Настраиваем HTTP Security
        return httpSecurity
                // Включаем базовую аутентификацию
                .httpBasic(httpSecurityHttpBasicConfigurer ->
                        httpSecurityHttpBasicConfigurer.authenticationEntryPoint(authenticationEntryPoint)
                )

                // Устанавливаем правило для авторизации всех HTTP-запросов (все должны быть аутентифицированы)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry.anyRequest().authenticated()
                )

                // Настраиваем обработку исключений, в частности, указываем точку входа в систему
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
                        httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(authenticationEntryPoint)
                )

                // Собираем и возвращаем SecurityFilterChain
                .build();
    }


//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                // Конфигурация базовой аутентификации. В данном случае, закомментирована, возможно, не используется.
//                //.httpBasic(Customizer.withDefaults())
//
//                // Конфигурация разрешений для HTTP-запросов.
//                .authorizeHttpRequests(authorize -> authorize
//                        // Разрешаем доступ к URL, содержащим "/public/**", всем пользователям.
//                        .requestMatchers("/public/**").permitAll()
//                        // Запрещаем остальные HTTP-запросы, требуем аутентификации.
//                        .anyRequest().authenticated()
//                )
//
//                // Конфигурация формы входа.
//                .formLogin(Customizer.withDefaults())
//
//                // Отключение CSRF-защиты для упрощения. Используйте осторожно в реальных приложениях.
//                .csrf(AbstractHttpConfigurer::disable)
//
//                // Конфигурация обработки исключений в безопасности.
//                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> httpSecurityExceptionHandlingConfigurer
//                        // Конфигурация точки входа в систему при отсутствии аутентификации.
//                        .authenticationEntryPoint((request, response, authException) ->
//                                response.sendRedirect("http://localhost:8080/public/sign-in.html")
//                        )
//                );
//
//        // Возврат экземпляра SecurityFilterChain, построенного с использованием настроек http.
//        return http.build();
//    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route()
                .GET("/api/v4/greetings", request -> {
                    UserDetails userDetails = request.principal()
                            .map(Authentication.class::cast)
                            .map(Authentication::getPrincipal)
                            .map(UserDetails.class::cast)
                            .orElseThrow();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("greeting", "Hello %s!".formatted(userDetails.getUsername())));
                })
                .build();
    }

}
