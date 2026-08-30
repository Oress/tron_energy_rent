# Angular Dashboard Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and package a mobile-first Angular dashboard with a shared global filter and independently paginated Orders and User Profit tables backed by one generated OpenAPI contract.

**Architecture:** An OpenAPI-first contract in the Angular sibling project generates Spring interfaces/models into Maven target sources and Angular services/models into `core/api`. Thin Spring adapters return empty, structurally valid pages through an explicit plain-SQL query-service seam, while standalone Angular features compose the filter and both PrimeNG tables. Maven builds the client, copies it to Spring Boot static resources, and packages it in the executable JAR without modifying existing webhook routes.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Maven, OpenAPI 3.0, OpenAPI Generator 7.25.0, Angular 22.1, TypeScript 6.0, PrimeNG 22.1, Tailwind CSS 4.3, npm, Node.js 24.

**Spec:** `docs/superpowers/specs/2026-08-30-angular-dashboard-foundation-design.md`

## Global Constraints

- All financial values cross the API boundary as decimal TRX; never expose raw SUN values.
- Orders has exactly 19 columns; User Profit has exactly 12 columns.
- Both reports use zero-based server pagination with page sizes 10, 20, and 50 in the UI and a backend maximum of 200.
- Global filter fields are optional `userId`, `groupId`, inclusive `dateFrom`, and inclusive `dateTo`; simultaneous user and group filters mean intersection.
- Filter changes make no request until Apply; Apply and Reset return both tables to page zero.
- Do not implement SQL or JPA report queries. Every placeholder query method must include exactly `//TODO: implement queries`.
- Do not add automated tests or test configuration.
- Do not modify `ItrxCallbackController`, `TrxxCallbackController`, `/api/itrx/callback`, or `/api/trxx/callback`.
- Do not treat existing untracked `.angular`, `dist`, `node_modules`, generated API files, or `version.ts` under `server/dashboard` as authoritative handwritten source.

---

## Planned File Structure

### Contract and build ownership

- `server/dashboard/openapi/nrgyrent-api.yaml`: the only dashboard API schema.
- `server/dashboard/openapitools.json`: Angular generator configuration.
- `server/dashboard/src/app/core/api/**`: generated Angular code; no handwritten edits.
- `server/nrgyrent/target/generated-sources/openapi/**`: generated Java code; never commit.
- `server/nrgyrent/pom.xml`: Java generation, Node/npm build, version generation, and resource copy lifecycle.

### Handwritten backend

- `src/main/java/org/ipan/nrgyrent/dashboard/DashboardController.java`: implements generated report and filter APIs.
- `src/main/java/org/ipan/nrgyrent/dashboard/DashboardQueryService.java`: plain-SQL seam with empty placeholder methods.
- `src/main/java/org/ipan/nrgyrent/dashboard/DashboardPageFactory.java`: constructs valid generated empty page models and calculates total pages.
- `src/main/java/org/ipan/nrgyrent/dashboard/DashboardSpaController.java`: forwards only Angular `/dashboard` routes to `index.html`.

### Handwritten frontend

- `server/dashboard/src/app/core`: application-wide guard, interceptor, layout, and API provider.
- `server/dashboard/src/app/features/dashboard`: lazy composition route.
- `server/dashboard/src/app/features/dashboard-filter`: draft/applied filter state and controls.
- `server/dashboard/src/app/features/orders`: Orders request state and table.
- `server/dashboard/src/app/features/user-profit`: User Profit request state and table.
- `server/dashboard/src/app/shared`: TRX formatting and request-state presentation.

---

### Task 1: Establish the OpenAPI Contract and Reproducible Build

**Files:**
- Create: `../dashboard/openapi/nrgyrent-api.yaml`
- Create: `../dashboard/openapitools.json`
- Create: `../dashboard/package.json`
- Create: `../dashboard/angular.json`
- Create: `../dashboard/tsconfig.json`
- Create: `../dashboard/tsconfig.app.json`
- Create: `../dashboard/.postcssrc.json`
- Create: `../dashboard/.prettierrc`
- Create: `../dashboard/.editorconfig`
- Create: `../dashboard/.gitignore`
- Create: `../dashboard/proxy.conf.json`
- Create: `../dashboard/scripts/generate-version.mjs`
- Create: `../dashboard/src/environments/environment.ts`
- Create: `../dashboard/src/environments/environment.prod.ts`
- Create: `../dashboard/src/environments/version.ts`
- Modify: `pom.xml`
- Generate: `../dashboard/package-lock.json`

**Interfaces:**
- Produces OpenAPI schemas `FilterOption`, `OrderRow`, `OrderPage`, `UserProfitRow`, `UserProfitPage`, and `ApiError`.
- Produces operations `getFilterUsers`, `getFilterGroups`, `getOrders`, and `getUserProfits` under `/api/v1/dashboard`.
- Produces Java interfaces `FiltersApi`, `OrdersApi`, and `UserProfitApi` in `org.ipan.nrgyrent.dashboard.api`.
- Produces Java models in `org.ipan.nrgyrent.dashboard.api.model` and Angular services/models under `src/app/core/api`.

- [ ] **Step 1: Write the single OpenAPI document**

Define OpenAPI 3.0.3 paths:

```yaml
/api/v1/dashboard/filters/users:
  get:
    tags: [Filters]
    operationId: getFilterUsers
/api/v1/dashboard/filters/groups:
  get:
    tags: [Filters]
    operationId: getFilterGroups
/api/v1/dashboard/orders:
  get:
    tags: [Orders]
    operationId: getOrders
/api/v1/dashboard/user-profits:
  get:
    tags: [UserProfit]
    operationId: getUserProfits
```

Both report operations reference shared optional query parameters for `page`, `size`, `userId`, `groupId`, `dateFrom`, and `dateTo`. Put every property in each schema's `required` list, including nullable properties, so Java and TypeScript retain the same property presence. Use `type: number` with `format: double` and descriptions explicitly saying “decimal TRX” for financial values.

- [ ] **Step 2: Configure both generators**

Configure the npm OpenAPI generator as `typescript-angular`, `ngVersion: 22.1.0`, `providedIn: root`, kebab-case files, interfaces enabled, and output `src/app/core/api`. Configure Maven's `openapi-generator-maven-plugin` 7.25.0 in `generate-sources` with generator `spring`, `interfaceOnly: true`, `useSpringBoot3: true`, `useTags: true`, `skipDefaultInterface: true`, `openApiNullable: false`, API package `org.ipan.nrgyrent.dashboard.api`, and model package `org.ipan.nrgyrent.dashboard.api.model`.

- [ ] **Step 3: Configure Angular and dependency scripts**

Pin Angular core packages to 22.1.4, CLI/build to 22.1.6, PrimeNG 22.1.0, PrimeIcons 8.0.0, Tailwind/PostCSS 4.3.3, TypeScript 6.0.x, and OpenAPI Generator CLI 2.41.0. Provide scripts `start`, `build`, `build:dev`, `generate-api`, and `generate-version`; do not add a test script.

- [ ] **Step 4: Configure Maven frontend packaging**

Add Maven properties for Node 24.19.0, npm 11.17.0, frontend plugin 1.15.4, OpenAPI Generator 7.25.0, `angular.build.skip`, and `angular.build.config`. Add ordered frontend executions for Node/npm installation, npm install, Angular client generation, version generation, and build. Add an execution-scoped `maven-resources-plugin` copy from `../dashboard/dist/dashboard/browser` to `${project.build.directory}/classes/static` so ordinary Maven resources are never skipped.

- [ ] **Step 5: Generate dependencies and both contracts**

Run:

```powershell
Set-Location ..\dashboard
npm install
npm run generate-api
Set-Location ..\nrgyrent
.\mvnw.cmd generate-sources -Dangular.build.skip=true
```

Expected: `package-lock.json`, Angular API sources, `target/generated-sources/openapi`, and no generator validation errors.

- [ ] **Step 6: Inspect contract parity**

Confirm `OrderRow` has 19 properties, `UserProfitRow` has 12, both page models have `content/page/size/totalElements/totalPages`, Java financial fields are `BigDecimal`, TypeScript financial fields are `number`, and all four operations share the agreed parameter names.

- [ ] **Step 7: Commit the contract and build foundation**

```powershell
git add server/dashboard server/nrgyrent/pom.xml
git commit -m "build: add generated dashboard contract pipeline"
```

Do not add `node_modules`, `.angular`, `dist`, `target`, or generated Java sources.

### Task 2: Implement the Backend Contract Adapters and Empty Query Seam

**Files:**
- Create: `src/main/java/org/ipan/nrgyrent/dashboard/DashboardController.java`
- Create: `src/main/java/org/ipan/nrgyrent/dashboard/DashboardQueryService.java`
- Create: `src/main/java/org/ipan/nrgyrent/dashboard/DashboardPageFactory.java`
- Create: `src/main/java/org/ipan/nrgyrent/dashboard/DashboardSpaController.java`

**Interfaces:**
- Consumes generated `FiltersApi`, `OrdersApi`, `UserProfitApi`, `FilterOption`, `OrderPage`, and `UserProfitPage`.
- Produces `DashboardQueryService#getOrders(int,int,Long,Long,LocalDate,LocalDate): OrderPage`.
- Produces `DashboardQueryService#getUserProfits(int,int,Long,Long,LocalDate,LocalDate): UserProfitPage`.
- Produces `DashboardQueryService#getFilterUsers(): List<FilterOption>` and `getFilterGroups(): List<FilterOption>`.

- [ ] **Step 1: Add generated-model page construction**

Implement `DashboardPageFactory` with `MIN_SIZE = 1`, `MAX_SIZE = 200`, `DEFAULT_SIZE = 20`, non-negative page normalization, and overloads that create empty `OrderPage` and `UserProfitPage` values with empty content, normalized page/size, `totalElements = 0`, and `totalPages = 0`.

- [ ] **Step 2: Add the explicit query placeholders**

Implement all four query-service methods. Each method must include its own exact line:

```java
//TODO: implement queries
```

The two report methods return the matching factory-created empty page. The two filter methods return `List.of()`. Do not inject `EntityManager`, repositories, JDBC classes, or data sources in this increment.

- [ ] **Step 3: Implement generated Spring interfaces**

Create a `@RestController` implementing all three generated interfaces. Delegate every method to `DashboardQueryService`, applying OpenAPI defaults of page 0 and size 20 when generated nullable arguments are absent. Do not duplicate endpoint path annotations already present on generated interfaces.

- [ ] **Step 4: Add narrowly scoped SPA navigation forwarding**

Forward only `/dashboard` and `/dashboard/**` to `/index.html`. Do not add a catch-all `/**` mapping. Root static serving remains Spring Boot's standard index behavior, while `/api/**` and `/actuator/**` are untouched.

- [ ] **Step 5: Compile and audit mappings**

Run:

```powershell
.\mvnw.cmd compile -Dangular.build.skip=true
rg -n '@PostMapping\("/api/(itrx|trxx)/callback"\)' src\main\java
rg -n '//TODO: implement queries' src\main\java\org\ipan\nrgyrent\dashboard
```

Expected: compilation succeeds, both original callback lines remain, and exactly four query placeholder markers exist.

- [ ] **Step 6: Commit backend adapters**

```powershell
git add server/nrgyrent/src/main/java/org/ipan/nrgyrent/dashboard
git commit -m "feat: add dashboard API placeholders"
```

### Task 3: Build the Angular Shell, Core Infrastructure, and Shared UI

**Files:**
- Create: `../dashboard/src/index.html`
- Create: `../dashboard/src/main.ts`
- Create: `../dashboard/src/styles.css`
- Create: `../dashboard/src/styles.scss`
- Create: `../dashboard/src/app/app.ts`
- Create: `../dashboard/src/app/app.config.ts`
- Create: `../dashboard/src/app/app.routes.ts`
- Create: `../dashboard/src/app/core/guards/auth.guard.ts`
- Create: `../dashboard/src/app/core/interceptors/http-error.interceptor.ts`
- Create: `../dashboard/src/app/core/layout/app-layout.component.ts`
- Create: `../dashboard/src/app/core/layout/app-layout.html`
- Create: `../dashboard/src/app/core/layout/app-layout.scss`
- Create: `../dashboard/src/app/core/providers/provide-dashboard-api.ts`
- Create: `../dashboard/src/app/shared/pipes/trx-amount.pipe.ts`
- Create: `../dashboard/src/app/shared/components/request-state/request-state.component.ts`
- Create: `../dashboard/src/app/shared/components/request-state/request-state.html`
- Create: `../dashboard/src/app/features/dashboard/dashboard.component.ts`
- Create: `../dashboard/src/app/features/dashboard/dashboard.html`
- Create: `../dashboard/src/app/features/dashboard/dashboard.scss`
- Create: `../dashboard/src/app/features/dashboard/dashboard.routes.ts`

**Interfaces:**
- Produces `authGuard: CanActivateFn`, which intentionally returns `true` until server authentication exists.
- Produces `provideDashboardApi(): EnvironmentProviders` with same-origin production base path.
- Produces `TrxAmountPipe`, accepting number/null/undefined and rendering decimal TRX.
- Produces request-state inputs `loading`, `error`, and retry output.

- [ ] **Step 1: Configure application providers**

Bootstrap the standalone root with router, `provideHttpClient(withInterceptors(...))`, async animations, PrimeNG Aura theme, and generated API configuration. The interceptor must preserve the original HTTP error and must not convert API failures into successful responses.

- [ ] **Step 2: Implement routes and honest guard placeholder**

Route the guarded layout to a lazy `/dashboard` feature and redirect root/unknown client paths to it. Document in the guard source that client guards are not a security boundary and that the current server has no active authentication mechanism.

Create the initial dashboard feature as a minimal routed page containing its
heading and explanatory empty foundation copy. Task 5 replaces that body with
the filter and two report sections; keeping the route real here makes the shell
independently compilable.

- [ ] **Step 3: Implement the mobile-first shell**

Build a compact header with product title, current section, and version metadata, followed by a constrained content area. Use Tailwind utilities for layout and a small component stylesheet only for shell-specific behavior. Avoid desktop-only fixed widths.

- [ ] **Step 4: Add shared request and TRX presentation**

Format values with up to six fractional digits and a `TRX` suffix; render absent values as an em dash. Implement accessible loading, empty, and error/retry presentation that report features can reuse.

- [ ] **Step 5: Compile the shell**

Run `npm run build` from `server/dashboard`.

Expected: the production build succeeds before report features are added and outputs `dist/dashboard/browser/index.html`.

- [ ] **Step 6: Commit core and shared UI**

```powershell
git add server/dashboard/src server/dashboard/angular.json
git commit -m "feat: add mobile dashboard shell"
```

### Task 4: Implement the Applied Global Filter

**Files:**
- Create: `../dashboard/src/app/features/dashboard-filter/dashboard-filter.model.ts`
- Create: `../dashboard/src/app/features/dashboard-filter/dashboard-filter.service.ts`
- Create: `../dashboard/src/app/features/dashboard-filter/dashboard-filter.component.ts`
- Create: `../dashboard/src/app/features/dashboard-filter/dashboard-filter.html`
- Create: `../dashboard/src/app/features/dashboard-filter/dashboard-filter.scss`

**Interfaces:**
- Produces `DashboardFilter { userId: number | null; groupId: number | null; dateFrom: string | null; dateTo: string | null }`.
- Produces read-only `appliedFilter` and monotonically increasing `revision` signals.
- Produces `apply(filter)` and `reset()` methods.
- Consumes generated `FiltersService#getFilterUsers()` and `getFilterGroups()`.

- [ ] **Step 1: Implement draft versus applied state**

Keep control values local to the component and applied values in the service. Clone applied objects rather than mutating them, increment `revision` exactly once per Apply or Reset, and initialize with all-null filters.

- [ ] **Step 2: Implement selector loading and date validation**

Load user and group options independently so one failure does not disable the other selector. Use searchable PrimeNG selects, native ISO date values, and reject Apply when `dateTo < dateFrom` with an inline Russian validation message.

- [ ] **Step 3: Implement responsive controls**

Stack all controls and full-width buttons on small screens; switch to a compact grid at tablet widths. Apply sends both selected IDs when both are present. Reset clears all controls and immediately applies the empty filter.

- [ ] **Step 4: Build and commit the filter**

Run `npm run build`, then commit only handwritten feature files.

```powershell
git add server/dashboard/src/app/features/dashboard-filter
git commit -m "feat: add dashboard global filter"
```

### Task 5: Implement Both Independently Paginated Report Tables

**Files:**
- Create: `../dashboard/src/app/features/orders/orders-table.component.ts`
- Create: `../dashboard/src/app/features/orders/orders-table.html`
- Create: `../dashboard/src/app/features/orders/orders-table.scss`
- Create: `../dashboard/src/app/features/user-profit/user-profit-table.component.ts`
- Create: `../dashboard/src/app/features/user-profit/user-profit-table.html`
- Create: `../dashboard/src/app/features/user-profit/user-profit-table.scss`
- Modify: `../dashboard/src/app/features/dashboard/dashboard.component.ts`
- Modify: `../dashboard/src/app/features/dashboard/dashboard.html`
- Modify: `../dashboard/src/app/features/dashboard/dashboard.scss`

**Interfaces:**
- Orders consumes generated `OrdersService#getOrders(page,size,userId,groupId,dateFrom,dateTo)` and `OrderRow`.
- User Profit consumes generated `UserProfitService#getUserProfits(page,size,userId,groupId,dateFrom,dateTo)` and `UserProfitRow`.
- Both consume `DashboardFilterService.appliedFilter/revision` and expose independent page, size, total, loading, error, and rows signals.

- [ ] **Step 1: Implement common request behavior in each feature**

On initialization and each applied-filter revision, set the feature's `first` offset to zero and request page zero. On paginator events, calculate `page = floor(first / rows)` and request only that table. On error, preserve an error state and stop loading. A retry repeats the current page with the current applied filter.

- [ ] **Step 2: Render the 19-column Orders table**

Bind every OpenAPI field once and in the approved order. Use the TRX pipe for commission, income, referral deductions, referral remainder, and profit; date formatting for `date`; icons plus accessible labels for wallet activation; monospaced truncation with full-value title text for recipient, order number, and transaction.

- [ ] **Step 3: Render the 12-column User Profit table**

Bind every OpenAPI field once and in the approved order. Use the TRX pipe for profit, available balance, income, ITRX commission, referral deductions, and manual adjustments; format `joinedAt` as date/time.

- [ ] **Step 4: Apply mobile table behavior**

Place each semantic PrimeNG table in an overflow container, set a content minimum width, keep the first identifying column sticky, use compact cells, and preserve every column at all widths. Configure lazy pagination with `[10, 20, 50]`, current-page summary, striped rows, and the shared loading/empty/error states.

- [ ] **Step 5: Compose the dashboard sequence**

Render the global filter, Orders, and User Profit sections in that exact order. Use section spacing and headings rather than tabs so both reports remain part of one vertical dashboard sequence.

- [ ] **Step 6: Generate and build from a clean source boundary**

Run:

```powershell
Set-Location ..\dashboard
npm run generate-api
npm run build
```

Expected: no handwritten import reaches a missing generated type, all templates compile, and production assets are emitted.

- [ ] **Step 7: Commit both reports**

```powershell
git add server/dashboard/src/app/features server/dashboard/src/app/shared
git commit -m "feat: add paginated dashboard reports"
```

### Task 6: Verify End-to-End Packaging and Safety Boundaries

**Files:**
- Modify only if verification exposes a defect in files created by Tasks 1–5.

**Interfaces:**
- Verifies the complete Maven-to-JAR delivery path and all contract invariants.

- [ ] **Step 1: Run contract and frontend verification**

Run `npm run generate-api` and `npm run build`. Inspect generated models and count the Orders/User Profit properties against 19/12.

- [ ] **Step 2: Run the full Maven package lifecycle**

Run:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: OpenAPI Java generation, Node/npm installation, Angular client generation, version generation, Angular production build, resource copy, Java compilation, and JAR packaging all succeed in lifecycle order.

- [ ] **Step 3: Inspect the executable JAR**

Run:

```powershell
jar tf target\nrgyrent-0.0.1-SNAPSHOT.jar | Select-String 'BOOT-INF/classes/static/(index.html|main-|styles-)'
```

Expected: `index.html` and hashed main/style assets are packaged below `BOOT-INF/classes/static`.

- [ ] **Step 4: Audit route and query constraints**

Confirm callback files have no diff, both callback mappings still exist, dashboard mappings exist only below `/api/v1/dashboard`, the SPA controller has no catch-all mapping, there are exactly four `//TODO: implement queries` lines, and the dashboard package contains no SQL strings, JPA query annotations, repositories, or JDBC dependencies.

- [ ] **Step 5: Review working tree scope**

Run `git status --short` and `git diff --check`. Ensure cache/build directories are ignored, no user-owned unrelated files were staged, and generated Java target sources remain untracked/ignored.

- [ ] **Step 6: Commit verification fixes if needed**

If verification required source fixes, commit only those fixes with:

```powershell
git add server/dashboard/src server/dashboard/openapi server/dashboard/package.json server/dashboard/package-lock.json server/nrgyrent/pom.xml server/nrgyrent/src/main/java/org/ipan/nrgyrent/dashboard
git commit -m "fix: complete dashboard packaging verification"
```

If no source changes were required, do not create an empty commit.
