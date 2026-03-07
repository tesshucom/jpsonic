/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.infrastructure.NeedsHome;
import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.persistence.core.repository.UserDao;
import com.tesshu.jpsonic.security.GlobalSecurityConfig;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

@SpringBootTest
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@NeedsHome
@SuppressWarnings("PMD.TooManyStaticImports")
class LoginControllerTest {

    private final List<Class<?>> loggingClasses = List
        .of(AuthorizationFilter.class, FilterChainProxy.class, CsrfFilter.class, LogoutFilter.class,
                HttpSessionRequestCache.class, RequestMatcherDelegatingAuthorizationManager.class,
                HttpSessionSecurityContextRepository.class, AnonymousAuthenticationFilter.class,
                ExceptionTranslationFilter.class, AuthorizationFilter.class,
                GlobalSecurityConfig.class);

    void setLogLevel(Level logLevel) {
        loggingClasses.forEach(clazz -> TestCaseUtils.setLogLevel(clazz, logLevel));
    }

    @Order(1)
    @EnableMethodSecurity(prePostEnabled = false)
    @Nested
    class UnitTest {

        private MockMvc mockMvc;

        @BeforeEach
        void setup() throws ExecutionException {
            mockMvc = MockMvcBuilders
                .standaloneSetup(new LoginController(mock(SettingsService.class),
                        mock(SecurityService.class)))
                .build();
        }

        @Test
        void testGet() throws Exception {
            MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/login.view"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals("login", modelAndView.getViewName());
        }
    }

    // Redundant tests for Springboot3 migration
    @EnableMethodSecurity(prePostEnabled = true)
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class IntegrationTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @BeforeEach
        void setup() throws ExecutionException {
            mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        }

        @AfterEach
        void tearDown() {
            setLogLevel(Level.WARN);
        }

        @Order(1)
        @Test
        void testWithoutUser() throws Exception {
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Order(2)
        @WithMockUser
        @Test
        void testWithDefaultMockUser() throws Exception {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(UsernamePasswordAuthenticationToken.class, auth.getClass());
            assertEquals("user", auth.getName());
            assertEquals("password", auth.getCredentials());
            assertEquals(1, auth.getAuthorities().size());
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
            assertNull(auth.getDetails());
        }

        @Order(3)
        @WithMockUser(username = "admin", password = "pass", authorities = { "SETTINGS", "DOWNLOAD",
                "SHARE" })
        @Test
        void testWithMockUser() throws Exception {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(UsernamePasswordAuthenticationToken.class, auth.getClass());
            assertEquals("admin", auth.getName());
            assertEquals("pass", auth.getCredentials());
            assertEquals(3, auth.getAuthorities().size());
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("SETTINGS")));
            assertNull(auth.getDetails());
        }

        @Order(4)
        @Test
        void testDaoUser() throws Exception {
            UserDao userDao = webApplicationContext.getBean(UserDao.class);
            User user = userDao.getUserByName("admin", false);
            assertTrue(user.isAdminRole());
        }

        @Order(5)
        @Test
        void testPathPattern() throws Exception {
            assertNotNull(webApplicationContext.getBean(LoginController.class));
            Map<String, RequestMappingHandlerMapping> matchingBeans = BeanFactoryUtils
                .beansOfTypeIncludingAncestors(webApplicationContext,
                        RequestMappingHandlerMapping.class, true, false);
            List<HandlerMapping> handlers = new ArrayList<>(matchingBeans.values());
            AnnotationAwareOrderComparator.sort(handlers);
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = matchingBeans
                .get("requestMappingHandlerMapping")
                .getHandlerMethods();

            List<PathPattern> loginPathPatterns = handlerMethods
                .keySet()
                .stream()
                .map(RequestMappingInfo::getPathPatternsCondition)
                .map(PathPatternsRequestCondition::getPatterns)
                .filter(pathPattern -> pathPattern
                    .toString()
                    .toLowerCase(Locale.ROOT)
                    .contains("login"))
                .flatMap(Collection::stream)
                .sorted()
                .toList();

            assertEquals(2, loginPathPatterns.size());
            assertEquals("/login.view", loginPathPatterns.get(0).getPatternString());
            assertEquals("/login", loginPathPatterns.get(1).getPatternString());
        }

        @Order(7)
        @Test
        void testAnonymousGet() throws Exception {
            setLogLevel(Level.TRACE);
            MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/login.view"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals("login", modelAndView.getViewName());
        }
    }
}
