package com.campus.trade.controller;

import com.campus.trade.common.GlobalExceptionHandler;
import com.campus.trade.dto.admin.AuditItemRequest;
import com.campus.trade.dto.message.CreateMessageRequest;
import com.campus.trade.dto.share.CreateShareRequest;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.ItemMessage;
import com.campus.trade.entity.SharePost;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.CustomUserDetailsService;
import com.campus.trade.security.JwtAuthenticationFilter;
import com.campus.trade.security.JwtService;
import com.campus.trade.security.SecurityConfig;
import com.campus.trade.security.ShareGateFilter;
import com.campus.trade.security.ShareGateSessionService;
import com.campus.trade.service.AdminService;
import com.campus.trade.service.ItemService;
import com.campus.trade.service.MessageService;
import com.campus.trade.service.ShareService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = PublicItemAndSecurityIntegrationTest.TestApplication.class,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@AutoConfigureMockMvc
class PublicItemAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StubItemService.reset();
        StubMessageService.reset();
        StubShareService.reset();
        StubJwtService.reset();
        StubShareGateSessionService.reset();
    }

    @Test
    void shouldOnlyReturnOnSaleItemsForPublicList() throws Exception {
        Item item = new Item();
        item.setId(1L);
        item.setTitle("公开商品");
        item.setStatus(ItemService.ITEM_STATUS_ON_SALE);
        item.setPrice(new BigDecimal("12.50"));
        StubItemService.listResult = List.of(item);

        mockMvc.perform(get("/api/items")
                        .param("status", "PENDING_REVIEW")
                        .param("category", "book")
                        .param("keyword", "java")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value(ItemService.ITEM_STATUS_ON_SALE));

        Assertions.assertEquals(ItemService.ITEM_STATUS_ON_SALE, StubItemService.lastStatus);
        Assertions.assertEquals("book", StubItemService.lastCategory);
        Assertions.assertEquals("java", StubItemService.lastKeyword);
        Assertions.assertEquals(2, StubItemService.lastPage);
        Assertions.assertEquals(5, StubItemService.lastSize);
    }

    @Test
    void shouldAllowPublicItemDetailWithoutAuthentication() throws Exception {
        Item item = new Item();
        item.setId(9L);
        item.setTitle("详情商品");
        item.setStatus(ItemService.ITEM_STATUS_ON_SALE);
        StubItemService.detailResult = item;

        mockMvc.perform(get("/api/items/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.title").value("详情商品"));

        Assertions.assertNull(StubItemService.lastDetailUserId);
    }

    @Test
    void shouldPassAuthenticatedUserToItemDetail() throws Exception {
        Item item = new Item();
        item.setId(10L);
        item.setTitle("我的下架商品");
        item.setStatus(ItemService.ITEM_STATUS_OFF_SHELF);
        StubItemService.detailResult = item;
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/items/10")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));

        Assertions.assertEquals(1001L, StubItemService.lastDetailUserId);
    }

    @Test
    void shouldPassAnonymousUserToMessageListWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/items/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Assertions.assertNull(StubMessageService.lastListUserId);
        Assertions.assertEquals(1L, StubMessageService.lastListItemId);
    }

    @Test
    void shouldPassAuthenticatedUserToMessageList() throws Exception {
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/items/2/messages")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Assertions.assertEquals(1001L, StubMessageService.lastListUserId);
        Assertions.assertEquals(2L, StubMessageService.lastListItemId);
    }

    @Test
    void shouldRejectPostingItemMessageWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/items/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserLacksAdminRole() throws Exception {
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/admin/items")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void shouldRejectInvalidJwtForProtectedEndpointInsteadOfTreatingItAsAnonymous() throws Exception {
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(post("/api/items/1/messages")
                        .header("Authorization", "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void shouldRejectInvalidJwtForPublicEndpointInsteadOfIgnoringIt() throws Exception {
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/items")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void shouldRequireShareGateTokenForAuthenticatedShareList() throws Exception {
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/shares")
                        .servletPath("/api/shares")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Share gate verify required"));
    }

    @Test
    void shouldAllowShareListAfterPassingShareGateAndAuthentication() throws Exception {
        SharePost sharePost = new SharePost();
        sharePost.setId(3L);
        sharePost.setTitle("分享内容");
        StubShareService.listResult = List.of(sharePost);
        StubShareGateSessionService.validToken = "share-gate-ok";
        StubJwtService.validUserToken = "user-token";

        mockMvc.perform(get("/api/shares")
                        .servletPath("/api/shares")
                        .cookie(new Cookie(ShareGateSessionService.COOKIE_NAME, "share-gate-ok"))
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].title").value("分享内容"));

        Assertions.assertTrue(StubShareGateSessionService.checked);
        Assertions.assertEquals("user-token", StubJwtService.lastParsedToken);
        Assertions.assertEquals(0, StubShareService.lastPage);
        Assertions.assertEquals(20, StubShareService.lastSize);
    }

    @EnableAutoConfiguration
    @Import({
            AdminController.class,
            ItemController.class,
            MessageController.class,
            ShareController.class,
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            ShareGateFilter.class,
            GlobalExceptionHandler.class,
            TestBeans.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        AdminService adminService() {
            return new StubAdminService();
        }

        @Bean
        @Primary
        ItemService itemService() {
            return new StubItemService();
        }

        @Bean
        @Primary
        MessageService messageService() {
            return new StubMessageService();
        }

        @Bean
        @Primary
        ShareService shareService() {
            return new StubShareService();
        }

        @Bean
        @Primary
        JwtService jwtService() {
            return new StubJwtService();
        }

        @Bean
        @Primary
        ShareGateSessionService shareGateSessionService() {
            return new StubShareGateSessionService();
        }

        @Bean
        @Primary
        UserMapper userMapper() {
            return new UserMapper() {
                @Override
                public User findByUsername(String username) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public User findById(Long id) {
                    if (!Long.valueOf(1001L).equals(id)) {
                        return null;
                    }
                    User user = new User();
                    user.setId(1001L);
                    user.setUsername("tester");
                    user.setRole("USER");
                    user.setStatus("ACTIVE");
                    return user;
                }

                @Override
                public int insert(User user) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int updateAvatar(Long id, String avatarUrl) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int updateLoginInfo(Long id, String ip) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<String> listAllAvatarUrls() {
                    return Collections.emptyList();
                }

                @Override
                public List<User> listUsers(int limit, int offset) {
                    return Collections.emptyList();
                }
            };
        }

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                    return org.springframework.security.core.userdetails.User.withUsername(username)
                            .password("noop")
                            .roles("USER")
                            .build();
                }
            };
        }
    }

    static class StubAdminService extends AdminService {

        StubAdminService() {
            super(null, null, null, null, null, null);
        }

        public List<Item> listItems(String status, String category, String keyword, int page, int size) {
            return Collections.emptyList();
        }
    }

    static class StubItemService extends ItemService {

        static List<Item> listResult = Collections.emptyList();
        static Item detailResult;
        static String lastStatus;
        static String lastCategory;
        static String lastKeyword;
        static int lastPage;
        static int lastSize;
        static Long lastDetailUserId;

        StubItemService() {
            super(null, null, null, null);
        }

        static void reset() {
            listResult = Collections.emptyList();
            detailResult = null;
            lastStatus = null;
            lastCategory = null;
            lastKeyword = null;
            lastPage = -1;
            lastSize = -1;
            lastDetailUserId = null;
        }

        @Override
        public List<Item> list(String status, String category, String keyword, int page, int size) {
            lastStatus = status;
            lastCategory = category;
            lastKeyword = keyword;
            lastPage = page;
            lastSize = size;
            return listResult;
        }

        @Override
        public Item getById(Long userId, long id) {
            lastDetailUserId = userId;
            return detailResult;
        }
    }

    static class StubMessageService extends MessageService {

        static Long lastListUserId;
        static long lastListItemId;

        StubMessageService() {
            super(null, null);
        }

        static void reset() {
            lastListUserId = null;
            lastListItemId = -1L;
        }

        @Override
        public List<ItemMessage> listMessages(Long userId, long itemId) {
            lastListUserId = userId;
            lastListItemId = itemId;
            return Collections.emptyList();
        }

        @Override
        public ItemMessage postMessage(long fromUserId, long itemId, CreateMessageRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    static class StubShareService extends ShareService {

        static List<SharePost> listResult = Collections.emptyList();
        static int lastPage;
        static int lastSize;

        StubShareService() {
            super(null, null);
        }

        static void reset() {
            listResult = Collections.emptyList();
            lastPage = -1;
            lastSize = -1;
        }

        @Override
        public SharePost create(long userId, CreateShareRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SharePost> list(int page, int size) {
            lastPage = page;
            lastSize = size;
            return listResult;
        }

        @Override
        public SharePost getById(long id) {
            throw new UnsupportedOperationException();
        }
    }

    static class StubJwtService extends JwtService {

        static String validUserToken;
        static String lastParsedToken;

        StubJwtService() {
            super("a_secure_jwt_secret_with_more_than_32_chars", 3600);
        }

        static void reset() {
            validUserToken = null;
            lastParsedToken = null;
        }

        @Override
        public Claims parseClaims(String token) {
            lastParsedToken = token;
            if (!token.equals(validUserToken)) {
                throw new IllegalArgumentException("invalid token");
            }
            Claims claims = Jwts.claims();
            claims.setSubject("tester");
            claims.put("uid", 1001L);
            claims.put("role", "USER");
            return claims;
        }
    }

    static class StubShareGateSessionService extends ShareGateSessionService {

        static String validToken;
        static boolean checked;

        static void reset() {
            validToken = null;
            checked = false;
        }

        @Override
        public boolean hasValidSession(HttpServletRequest request) {
            checked = true;
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return false;
            }
            for (Cookie cookie : cookies) {
                if (Objects.equals(ShareGateSessionService.COOKIE_NAME, cookie.getName())
                        && Objects.equals(validToken, cookie.getValue())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void establishSession(HttpServletRequest request, HttpServletResponse response, long expireSeconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        }
    }
}
