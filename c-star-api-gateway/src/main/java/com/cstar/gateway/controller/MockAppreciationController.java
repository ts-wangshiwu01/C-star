package com.cstar.gateway.controller;

import com.cstar.gateway.dto.AppreciationView;
import com.cstar.gateway.dto.Category;
import com.cstar.gateway.dto.LeaderboardEntry;
import com.cstar.gateway.dto.MockEmployee;
import com.cstar.gateway.dto.PageResult;
import com.cstar.gateway.dto.PersonView;
import com.cstar.gateway.dto.QuotaView;
import com.cstar.gateway.dto.SeedAppreciation;
import com.cstar.gateway.dto.SendAppreciationRequest;
import com.cstar.gateway.dto.SendAppreciationResult;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock REST implementations of the C-Star appreciation path, used ONLY when
 * {@code cstar.mock.enabled=true} (dev default). It returns in-memory mock data
 * that mirrors the frontend's mock layer (c-star-frontend/src/api/mock/*) so the
 * frontend can run end-to-end against the real gateway WITHOUT core-service gRPC
 * or a database.
 *
 * <p>Endpoints (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code POST   /api/v1/appreciations}                       — send a flower</li>
 *   <li>{@code GET    /api/v1/appreciations/feed?page=&size=}       — company-wide feed</li>
 *   <li>{@code GET    /api/v1/appreciations/sent?page=&size=}       — recognitions I gave</li>
 *   <li>{@code GET    /api/v1/appreciations/received?page=&size=}   — recognitions I received</li>
 *   <li>{@code GET    /api/v1/leaderboards?scope=period|all_time}   — received-flow ranking</li>
 *   <li>{@code GET    /api/v1/quota}                                — my remaining quota</li>
 *   <li>{@code GET    /api/v1/categories}                           — preset categories</li>
 * </ul>
 *
 * <p>State is in memory: seed data is loaded once at startup from
 * {@code src/main/resources/mock/*.json}; a successful send mutates the in-memory
 * list so every read (feed/sent/received/quota/leaderboard) stays consistent.
 * State is lost on restart (accepted for dev).
 */
@Singleton
@Controller("/api/v1")
@Requires(property = "cstar.mock.enabled", value = "true")
public class MockAppreciationController {

    /** "我" — the mock current user (employeeId 1, 林晓). */
    static final long ME_ID = 1L;
    /** Period default quota (mirrors frontend DEFAULT_QUOTA). */
    static final long DEFAULT_QUOTA = 10L;

    // Appreciation error codes — must match c-star-frontend/src/types/api.ts
    // {AppreciationErrorCode}. The error body message carries the code name.
    private static final int APPR_INVALID_RECEIVER = 1;
    private static final int APPR_EMPTY_MESSAGE = 2;
    private static final int APPR_INVALID_CATEGORY = 3;
    private static final int APPR_INSUFFICIENT_QUOTA = 4;
    private static final int APPR_INTERNAL = 5;
    private static final int APPR_INVALID_FLOWER_COUNT = 7;

    private final JsonMapper jsonMapper;

    /** In-memory employees (employeeId, displayName, ssoId). */
    private final List<MockEmployee> employees = new CopyOnWriteArrayList<>();
    /** Preset categories. */
    private final List<Category> categories = new CopyOnWriteArrayList<>();
    /** Seed + runtime appreciations (the single mutable source of truth). */
    private final List<AppreciationView> appreciations = new CopyOnWriteArrayList<>();

    public MockAppreciationController(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @PostConstruct
    void loadSeedData() {
        try {
            employees.addAll(readEmployees());
            categories.addAll(readCategories());
            appreciations.addAll(readSeedAppreciations());
        } catch (Exception e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Failed to load mock seed data: "
                    + c.getClass().getName() + ": " + c.getMessage(), e);
        }
    }

    // ---------------------------------- Send ---------------------------------

    /** POST /api/v1/appreciations — send a red flower. Validation mirrors frontend mockSend. */
    @Post(value = "/appreciations", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<?> send(@Body SendAppreciationRequest body) {
        if (body == null || body.message() == null) {
            return badRequest(APPR_EMPTY_MESSAGE, "APPR_EMPTY_MESSAGE");
        }

        String receiverSsoId = body.receiverSsoId() == null ? null : body.receiverSsoId().trim();
        MockEmployee receiver = (receiverSsoId == null || receiverSsoId.isEmpty())
                ? null
                : findBySsoId(receiverSsoId);
        // CK-02 / CK-03: receiver exists and is not the giver (self-give rejected).
        if (receiver == null || receiver.employeeId() == ME_ID) {
            return badRequest(APPR_INVALID_RECEIVER, "APPR_INVALID_RECEIVER");
        }
        // CK-04: message non-empty.
        if (body.message().trim().isEmpty()) {
            return badRequest(APPR_EMPTY_MESSAGE, "APPR_EMPTY_MESSAGE");
        }
        // CK-05: flowerCount lower bound.
        if (body.flowerCount() < 1) {
            return badRequest(APPR_INVALID_FLOWER_COUNT, "APPR_INVALID_FLOWER_COUNT");
        }
        // CK-06: must not exceed remaining quota.
        if (body.flowerCount() > quotaView().remaining()) {
            return badRequest(APPR_INSUFFICIENT_QUOTA, "APPR_INSUFFICIENT_QUOTA");
        }
        // CK-07: category must be active.
        if (!isValidCategory(body.categoryId())) {
            return badRequest(APPR_INVALID_CATEGORY, "APPR_INVALID_CATEGORY");
        }

        long id = nextId();
        AppreciationView entry = new AppreciationView(
                id,
                person(ME_ID),
                new PersonView(receiver.employeeId(), receiver.displayName()),
                body.categoryId(),
                body.flowerCount(),
                body.message().trim(),
                Instant.now().toString());
        appreciations.add(0, entry); // newest first (mirrors mock unshift)

        return HttpResponse.ok(new SendAppreciationResult(id, quotaView().remaining()));
    }

    // ---------------------------------- Lists ---------------------------------

    /** GET /api/v1/appreciations/feed — company-wide, newest first. */
    @Get("/appreciations/feed")
    public PageResult<AppreciationView> feed(@QueryValue(defaultValue = "1") long page,
                                             @QueryValue(defaultValue = "10") long size) {
        return paginate(allSorted(), page, size);
    }

    /** GET /api/v1/appreciations/sent — recognitions I gave. */
    @Get("/appreciations/sent")
    public PageResult<AppreciationView> sent(@QueryValue(defaultValue = "1") long page,
                                             @QueryValue(defaultValue = "10") long size) {
        return paginate(allSorted().stream()
                .filter(a -> a.giver().employeeId() == ME_ID)
                .toList(), page, size);
    }

    /** GET /api/v1/appreciations/received — recognitions I received. */
    @Get("/appreciations/received")
    public PageResult<AppreciationView> received(@QueryValue(defaultValue = "1") long page,
                                                 @QueryValue(defaultValue = "10") long size) {
        return paginate(allSorted().stream()
                .filter(a -> a.receiver().employeeId() == ME_ID)
                .toList(), page, size);
    }

    /** GET /api/v1/leaderboards?scope=period|all_time. */
    @Get("/leaderboards")
    public List<LeaderboardEntry> leaderboards(@QueryValue(defaultValue = "period") String scope) {
        boolean period = "period".equals(scope);
        List<AppreciationView> list = allSorted().stream()
                .filter(a -> !period || isToday(a.sentAt()))
                .toList();

        Map<Long, Long> totals = new LinkedHashMap<>();
        for (AppreciationView a : list) {
            totals.merge(a.receiver().employeeId(), a.flowerCount(), Long::sum);
        }

        List<LeaderboardEntry> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> e : totals.entrySet()) {
            PersonView p = person(e.getKey());
            rows.add(new LeaderboardEntry(p.employeeId(), p.displayName(), e.getValue(), 0));
        }
        rows.sort(Comparator.comparingLong(LeaderboardEntry::total).reversed());
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardEntry r = rows.get(i);
            rows.set(i, new LeaderboardEntry(r.employeeId(), r.displayName(), r.total(), i + 1));
        }
        return rows;
    }

    // --------------------------------- Quota ---------------------------------

    /** GET /api/v1/quota. consumed = today's outgoing flowers from me. */
    @Get("/quota")
    public QuotaView quota() {
        return quotaView();
    }

    // ------------------------------- Categories ------------------------------

    /** GET /api/v1/categories — the 5 preset categories. */
    @Get("/categories")
    public List<Category> categories() {
        return List.copyOf(categories);
    }

    // ------------------------------- internals -------------------------------

    private QuotaView quotaView() {
        long consumed = allSorted().stream()
                .filter(a -> a.giver().employeeId() == ME_ID && isToday(a.sentAt()))
                .mapToLong(AppreciationView::flowerCount)
                .sum();
        long remaining = Math.max(0, DEFAULT_QUOTA - consumed);
        return new QuotaView(remaining, DEFAULT_QUOTA, consumed, todayStr());
    }

    private List<AppreciationView> allSorted() {
        List<AppreciationView> copy = new ArrayList<>(appreciations);
        copy.sort(Comparator.comparing(AppreciationView::sentAt).reversed());
        return copy;
    }

    private static PageResult<AppreciationView> paginate(List<AppreciationView> list,
                                                         long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, size);
        int start = (int) ((safePage - 1) * safeSize);
        List<AppreciationView> items = start >= list.size()
                ? List.of()
                : list.subList(start, Math.min(list.size(), start + (int) safeSize));
        return new PageResult<>(List.copyOf(items), safePage, safeSize, list.size());
    }

    private long nextId() {
        return appreciations.stream().mapToLong(AppreciationView::id).max().orElse(0) + 1;
    }

    private boolean isToday(String iso) {
        return iso != null && iso.length() >= 10 && iso.substring(0, 10).equals(todayStr());
    }

    private static String todayStr() {
        return Instant.now().toString().substring(0, 10);
    }

    private boolean isValidCategory(long id) {
        return categories.stream().anyMatch(c -> c.id() == id);
    }

    private MockEmployee findBySsoId(String ssoId) {
        return employees.stream()
                .filter(e -> e.ssoId().equalsIgnoreCase(ssoId))
                .findFirst()
                .orElse(null);
    }

    private PersonView person(long employeeId) {
        return employees.stream()
                .filter(e -> e.employeeId() == employeeId)
                .findFirst()
                .map(e -> new PersonView(e.employeeId(), e.displayName()))
                .orElse(new PersonView(employeeId, "某位同事"));
    }

    private static HttpResponse<?> badRequest(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error_code", code);
        body.put("error_message", message);
        return HttpResponse.<Map<String, Object>>status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ------------------------- JSON seed loading ----------------------------

    private List<MockEmployee> readEmployees() throws Exception {
        return new ArrayList<>(List.of(jsonMapper.readValue(resource("mock/employees.json"),
                MockEmployee[].class)));
    }

    private List<Category> readCategories() throws Exception {
        return new ArrayList<>(List.of(jsonMapper.readValue(resource("mock/categories.json"),
                Category[].class)));
    }

    private List<AppreciationView> readSeedAppreciations() throws Exception {
        SeedAppreciation[] seeds = jsonMapper.readValue(
                resource("mock/appreciations-seed.json"), SeedAppreciation[].class);
        List<AppreciationView> result = new ArrayList<>();
        for (SeedAppreciation s : seeds) {
            result.add(new AppreciationView(
                    s.id(),
                    person(s.giverId()),
                    person(s.receiverId()),
                    s.categoryId(),
                    s.flowerCount(),
                    s.message(),
                    atTimeIso(s.sentAt().daysAgo(), s.sentAt().hour(), s.sentAt().minute())));
        }
        return result;
    }

    private InputStream resource(String path) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Mock resource not found: " + path);
        }
        return in;
    }

    /**
     * Construct an ISO-8601 UTC timestamp from "N days ago at hour:minute".
     * Mirrors data.ts {@code atTime}: build a moment anchored to the given
     * wall-clock time N days back, then emit as ISO-8601 instant.
     */
    private static String atTimeIso(int daysAgo, int hour, int minute) {
        long nowEpochDay = Instant.now().getEpochSecond() / 86400L;
        long targetSecondEpochDay = nowEpochDay - daysAgo;
        long secondOfDay = hour * 3600L + minute * 60L;
        long epochSecond = targetSecondEpochDay * 86400L + secondOfDay;
        return Instant.ofEpochSecond(epochSecond).toString();
    }

    }