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
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.security.GlobalSecurityConfig;
import com.tesshu.jpsonic.security.RESTRequestParameterProcessingFilter;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import jakarta.servlet.Filter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
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
@ExtendWith(NeedsHome.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class LoginControllerTest {

    private final List<Class<?>> loggingClasses = List.of(AuthorizationFilter.class, FilterChainProxy.class,
            CsrfFilter.class, LogoutFilter.class, HttpSessionRequestCache.class,
            RequestMatcherDelegatingAuthorizationManager.class, HttpSessionSecurityContextRepository.class,
            AnonymousAuthenticationFilter.class, ExceptionTranslationFilter.class, AuthorizationFilter.class,
            GlobalSecurityConfig.class);

    void setLogLevel(Level logLevel) {
        loggingClasses.forEach(clazz -> TestCaseUtils.setLogLevel(clazz, logLevel));
    }

    @Nested
    @EnableMethodSecurity(prePostEnabled = false)
    @Order(1)
    class UnitTest {

        private MockMvc mockMvc;

        @BeforeEach
        void setup() throws ExecutionException {
            mockMvc = MockMvcBuilders
                    .standaloneSetup(new LoginController(mock(SettingsService.class), mock(SecurityService.class)))
                    .build();
        }

        @Test
        void testGet() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/login.view"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals("login", modelAndView.getViewName());
        }
    }

    // Redundant tests for Springboot3 migration
    @Nested
    @EnableMethodSecurity(prePostEnabled = true)
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class IntegrationTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @BeforeEach
        void setup() throws ExecutionException {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        }

        @AfterEach
        void tearDown() {
            setLogLevel(Level.WARN);
        }

        @Test
        @Order(1)
        void testWithoutUser() throws Exception {
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @WithMockUser
        @Test
        @Order(2)
        void testWithDefaultMockUser() throws Exception {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(UsernamePasswordAuthenticationToken.class, auth.getClass());
            assertEquals("user", auth.getName());
            assertEquals("password", auth.getCredentials());
            assertEquals(1, auth.getAuthorities().size());
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
            assertNull(auth.getDetails());
        }

        @Test
        @Order(3)
        @WithMockUser(username = "admin", password = "pass", authorities = { "SETTINGS", "DOWNLOAD", "SHARE" })
        void testWithMockUser() throws Exception {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(UsernamePasswordAuthenticationToken.class, auth.getClass());
            assertEquals("admin", auth.getName());
            assertEquals("pass", auth.getCredentials());
            assertEquals(3, auth.getAuthorities().size());
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("SETTINGS")));
            assertNull(auth.getDetails());
        }

        @Test
        @Order(4)
        void testDaoUser() throws Exception {
            UserDao userDao = webApplicationContext.getBean(UserDao.class);
            User user = userDao.getUserByName("admin", false);
            assertTrue(user.isAdminRole());
        }

        @Test
        @Order(5)
        void testPathPattern() throws Exception {
            assertNotNull(webApplicationContext.getBean(LoginController.class));
            Map<String, RequestMappingHandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                    webApplicationContext, RequestMappingHandlerMapping.class, true, false);
            List<HandlerMapping> handlers = new ArrayList<>(matchingBeans.values());
            AnnotationAwareOrderComparator.sort(handlers);
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = matchingBeans.get("requestMappingHandlerMapping")
                    .getHandlerMethods();

            List<PathPattern> loginPathPatterns = handlerMethods.keySet().stream()
                    .map(RequestMappingInfo::getPathPatternsCondition).map(PathPatternsRequestCondition::getPatterns)
                    .filter(pathPattern -> StringUtils.containsIgnoreCase(pathPattern.toString(), "login"))
                    .flatMap(Collection::stream).sorted().toList();

            assertEquals(2, loginPathPatterns.size());
            assertEquals("/login.view", loginPathPatterns.get(0).getPatternString());
            assertEquals("/login", loginPathPatterns.get(1).getPatternString());
        }

        @Test
        @Order(6)
        void testFilterChain() throws Exception {
            FilterChainProxy filterChain = webApplicationContext.getBean(FilterChainProxy.class);
            List<Filter> filters = filterChain.getFilters("/login");

            assertEquals(DisableEncodeUrlFilter.class, filters.get(0).getClass());
            assertEquals(WebAsyncManagerIntegrationFilter.class, filters.get(1).getClass());
            assertEquals(SecurityContextHolderFilter.class, filters.get(2).getClass());
            assertEquals(HeaderWriterFilter.class, filters.get(3).getClass());
            assertEquals(CsrfFilter.class, filters.get(4).getClass());
            assertEquals(LogoutFilter.class, filters.get(5).getClass());
            assertEquals(RESTRequestParameterProcessingFilter.class, filters.get(6).getClass());
            assertEquals(UsernamePasswordAuthenticationFilter.class, filters.get(7).getClass());
            assertEquals(RequestCacheAwareFilter.class, filters.get(8).getClass());
            assertEquals(SecurityContextHolderAwareRequestFilter.class, filters.get(9).getClass());
            assertEquals(RememberMeAuthenticationFilter.class, filters.get(10).getClass());
            assertEquals(AnonymousAuthenticationFilter.class, filters.get(11).getClass());
            assertEquals(ExceptionTranslationFilter.class, filters.get(12).getClass());
            assertEquals(AuthorizationFilter.class, filters.get(13).getClass());
            assertEquals(14, filters.size());
        }

        @Test
        @Order(7)
        void testAnonymousGet() throws Exception {
            setLogLevel(Level.TRACE);
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/login.view"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals("login", modelAndView.getViewName());
        }
    }
}
