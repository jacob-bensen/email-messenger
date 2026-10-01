# Thymeleaf website

Templates: `src/main/resources/templates` (`chat.html`, `compose.html`, `conversation.html`, `landing.html`, `pricing.html`, `login/register/verify-email`, `account*.html`, `billing/`, `mailboxes/`, `password/`, `team/`, `digest/`, `admin/`, `legal.html`, `error.html`, `fragments/`).

## Routes (public = no login)
| Route | Controller | Notes |
|---|---|---|
| GET `/`, `/pricing`, `/demo` (public) | `PricingController`, `DemoController` | landing/pricing/demo conversation |
| GET `/login`, `/register`; POST `/register`; POST `/login` (Spring Security) | `AuthController` | |
| GET `/auth/google/start` | `OAuthStartController` | |
| GET/POST `/password/forgot`, `/password/reset`; GET `/verify-email`, POST `/verify-email/resend` | `PasswordResetController`, `EmailVerificationController` | |
| GET `/privacy` `/terms` `/refund`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `LegalController`, `SeoController` | |
| GET `/digest/opt-out` | `DigestController` | public |
| GET `/chats` (params `page,q,mailbox,filter`) | `ConversationController` | workspace list |
| GET/POST `/chats/new`; POST `/chats/refresh` | same | compose; manual poll |
| GET `/chats/{key}` , `/chats/{key}/pane` | same | marks read; pane = HTML fragment |
| POST `/chats/{key}/send` (`body,subject,asReply,attachments,mailbox`) | same | multipart |
| GET `/chats/events` | `ChatEventsController` | SSE new-mail |
| GET `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects |
| GET `/mailboxes`, `/new`; POST `/mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | |
| GET `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | |
| POST `/searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| GET `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`; POST `/account/appearance`, `/password`, `/email` | `AccountController` | |
| POST `/billing/checkout`, `/portal`, `/cancel-subscription`; GET `/billing/success`, `/cancel` (public), `/cancel-subscription`; POST `/billing/webhook` (public, CSRF-exempt) | `BillingController`, `BillingWebhookController` | |
| GET/POST `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| GET `/admin/revenue`; POST `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | admin only |
| PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | |

## Shared layer
Controllers delegate to `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, etc. Model attributes shared by all pages come from `NavModelAdvice` / `SiteModelAdvice`.
