# NrgyRent Dashboard

Angular 22 admin dashboard for the **nrgyrent** Spring Boot server (TRON energy rental platform).
Replaces the limited Metabase dashboard. **Mobile-first** (phones → tablets → desktop) and built
on **[PrimeNG 22](https://primeng.org)** with the Aura theme preset. The production build is
packaged into the server jar by the Maven build of `server/nrgyrent` and served from the same
origin under `/`.

## Folder structure

```
src/app/
├── core/            # Infrastructure: guards, interceptors, layout, generated API client
│   ├── api/         # GENERATED from openapi/nrgyrent-api.yaml (npm run generate-api) — gitignored
│   ├── guards/      # auth.guard.ts (route protection)
│   ├── interceptors/# http-error.interceptor.ts (global HTTP error handling -> toasts)
│   └── layout/      # app-layout (sidebar/drawer + topbar shell)
├── features/        # One subfolder per business feature (lazy-loaded)
│   ├── dashboard/   # the dashboard = a sequence of report tables
│   ├── orders/      # orders-table.component — Таблица "Заказы" (19 columns)
│   └── profit/      # profit-table.component — "Прибыль по пользователям" (12 columns)
└── shared/          # Reusable UI: page-header component, format-usdt pipe, ...
```

Each feature keeps its components/services/stores/utils together; services and signal-based
stores are added per feature as they grow.

## UI library (PrimeNG + Tailwind CSS)

- **PrimeNG 22** provides the components (`p-drawer`, `p-menu`, `p-button`, `p-card`,
  `p-table`, `p-tag`, `p-toast`); `primeicons` is loaded through `angular.json`.
- **Tailwind CSS 4** provides all visual styling. PrimeNG runs with `theme: 'none'`
  (`providePrimeNG({ theme: 'none' })` in `app.config.ts`) — no `@primeuix/themes`
  dependency, no PrimeNG theme CSS; components keep their structural `p-*` classes,
  which the app styles with Tailwind utilities (`@apply` in the component styles).
- Layout is mobile-first: on phones the nav is an off-canvas `p-drawer` opened by a
  hamburger `p-button`; from 992px a persistent sidebar is used instead. Tables scroll
  horizontally on narrow screens, stat cards stack 1 → 2 → 4 columns.
- HTTP errors surface as PrimeNG toasts via `core/interceptors/http-error.interceptor.ts`
  (`MessageService` + the `<p-toast>` in the layout).

## API contract (OpenAPI)

`openapi/nrgyrent-api.yaml` is the contract between the server and this client:

- `GET /api/v1/statistics/overall` — Общая статистика → `OverallStatisticsDto` (15 metrics)
- `GET /api/v1/orders` — Таблица "Заказы" → `OrderRowPage` (19 columns)
- `GET /api/v1/deposits` — Таблица "Депозиты" → `DepositRowPage` (12 columns)
- `GET /api/v1/profit-by-user` — "Прибыль по пользователям" → `UserProfitPage` (12 columns)
- `GET /api/v1/referral-system` — "Реферальная система" → `ReferralSystemRowPage` (6 columns)
- `GET /api/v1/referral-payouts` — "Выплаты по реф программам" → `ReferralPayoutRowPage` (3 columns)
- `GET /api/v1/withdrawals` — "Выводы Пользователей" → `WithdrawalRowPage` (10 columns)
- `GET /api/v1/aml-checks` — "AML проверки" → `AmlCheckRowPage` (14 columns)

Table endpoints use `page`/`size` pagination and return a `Page` envelope
(`content`/`page`/`size`/`totalElements`); the tables in the UI use PrimeNG Table lazy
mode with the paginator. The statistics block renders label/value pairs in a two-column
grid (single column on phones). Extend the spec as new server endpoints are implemented,
then regenerate:

```bash
npm run generate-api     # generates src/app/core/api (typescript-angular)
```

### Global filter

Every table endpoint also accepts the global dashboard filter as optional query params:
`userId`, `groupId`, `dateFrom`, `dateTo` (dates are inclusive, local `YYYY-MM-DD`).
The filter bar (`features/filter`) holds user/group selectors and a date range, pushes the
values into `core/services/dashboard-filter.service.ts`, and all tables reload on change
(via a `version` signal + Angular `effect`). Selector options come from
`GET /api/v1/filter/users` and `GET /api/v1/filter/groups`.

Server side (`server/nrgyrent`, package `org.ipan.nrgyrent.dashboard`):
`DashboardController` exposes the endpoints and `DashboardQueryService` returns the pages.
The SQL queries are not implemented yet — the methods are marked
`// TODO: implement queries` (plain SQL with parameters, not JPA).

Generated services are root-provided and configured via `provideApi(...)` in
`app.config.ts`; the API base path comes from `src/environments/environment.ts` (dev) /
`environment.prod.ts` (prod).

## Local development

```bash
npm install
npm run generate-api    # required once, and after every spec change
npm start               # ng serve on http://localhost:4200
```

`ng serve` proxies `/api` to `http://localhost:8080` (see `proxy.conf.json`), so run the
nrgyrent server locally on 8080 during development.

## Build

```bash
npm run build           # production build -> dist/dashboard/browser
npm run build:dev       # development build
```

## Maven integration (packaging into the server jar)

The `nrgyrent` pom runs this whole pipeline during `mvn package` via
[frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin):

1. installs Node/npm into `server/nrgyrent/target`,
2. `npm install`,
3. `npm run generate-api`,
4. `npm run generate-version <version> <out> <git-hash>` — writes `src/environments/version.ts`,
5. `npm run build`,
6. copies `dist/dashboard/browser` into `target/classes/static` (maven-resources-plugin).

The server's `SpaErrorController` is a 404-only fallback: browser-like requests to
unmatched non-`/api` routes get `index.html`, so client-side routes (e.g. `/orders/123`)
work from the jar — while every mapped endpoint (webhook callbacks, actuator) and
unmapped `/api/**` paths keep their normal behavior.

Skip the frontend build with: `mvn package -Dangular.build.skip=true`
