# Website — Thymeleaf pages

Server-rendered pages under `src/main/resources/templates/`. Every page
is a Thymeleaf template returned by a `@Controller` in
`com.emailmessenger.web`, `.auth`, `.billing`, `.team`, `.digest`, or
`.admin`. Static assets (CSS, JS, images, PWA files) live under
`src/main/resources/static/`.

Business logic MUST be delegated to the beans documented in
`BACKEND.md`. Controllers here should be thin: model-attribute wiring,
form parsing, redirect selection. Any rule shared with the JSON API
belongs in `service/` (or the topic package), not the controller.

## Public pages (permit-all)

| Path | Template | Controller |
| --- | --- | --- |
| `/` | `landing.html` | `PricingController#landing` |
| `/pricing` | `pricing.html` | `PricingController#pricing` |
| `/demo` | `demo/*` | `DemoController` |
| `/login` | `login.html` | Spring Security / `AuthController` |
| `/register` | `register.html` | `AuthController` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` |
| `/verify-email` | `verify-email.html` | `EmailVerificationController` |
| `/privacy`, `/terms`, `/refund` | `legal.html` fragments | `LegalController` |
| `/auth/google/start` | redirect | `OAuthStartController` |
| `/digest/opt-out` | `digest/*` | `DigestController` |
| `/robots.txt`, `/sitemap.xml` | plain text | `SeoController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline` | PWA shell | `PwaController` |
| `/billing/webhook` | (empty 200) | `BillingWebhookController` — Stripe signature, CSRF-exempt |

## Authenticated pages

| Path | Template | Controller | Services used |
| --- | --- | --- | --- |
| `/threads`, `/threads/{id}` | `conversation.html` | `ConversationController` | `ConversationService`, `IMTransformService`, `ReplyService` |
| `/chats`, `/chats/{key}` | `chat.html` | `ChatService` + rail templates | `ConversationListService`, `ConversationKeyService`, `SavedSearchCountService` |
| `/chats/events` (SSE) | — | `ChatEventsController` + `ChatEventStream` | listens to `NewMailEvent` from `email/` |
| `/compose` | `compose.html` | `ConversationController` | `ReplyService` |
| `/mailboxes`, `/mailboxes/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `/account`, `/account/**` | `account*.html` | `AccountController` | `AccountService`, `UserService`, `BillingService` |
| `/billing/**` | `billing/*` | `BillingController` | `BillingService`, `StripeCheckoutGateway`, `StripePortalGateway`, `PlanLimitService` |
| `/team`, `/team/invite/**` | `team/*` | `TeamInviteController` (+ others) | team package services |
| `/saved-searches`, `/saved-searches/**` | (fragments in `chat.html`) | `SavedSearchController` | `SavedSearchService` |
| `/admin/**` | `admin/*` | `AdminRevenueController` + admin package | admin package services; gated by `admin.emails` allowlist |

## Shared fragments and layout

`templates/fragments/` holds the layout, nav, live-refresh SSE glue
(`live-refresh.html`), banners, and the signature rail. `SiteModelAdvice`
and `NavModelAdvice` are `@ControllerAdvice` beans that push
site-config and nav model attributes into every request so fragments
don't have to be re-populated in each controller.

## Static assets

- `static/css/` — the IM look (bubbles, avatars, day separators, dark
  mode) and layout CSS.
- `static/js/` — small hand-rolled scripts, no framework. PWA registration
  in `sw.js`.
- `static/images/`, `static/icons/` — logos and PWA icons.

## What the mobile client SHOULDN'T do

The Flutter app must not scrape or embed website HTML — that path
duplicates business logic and skips the API contract. If a screen the
web has doesn't have a matching `/api/v1/*` endpoint yet, add the
endpoint (documenting it in `API.md` first) and then wire the mobile
screen to it.
