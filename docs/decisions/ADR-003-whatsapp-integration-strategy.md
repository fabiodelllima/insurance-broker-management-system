# ADR-003: WhatsApp Integration Strategy

| Field      | Value      |
| ---------- | ---------- |
| **Status** | Accepted   |
| **Date**   | 2026-03-13 |

## Context

The IBMS notification module (Sprint 6) requires WhatsApp integration for transactional
notifications to insurance brokers and their clients: policy activation confirmations, claim
status updates, renewal reminders, and quote acceptance alerts.

The system targets 50–100 users during MVP, generating an estimated 200–500 outbound messages
per month. Messages are exclusively transactional (utility and service categories in Meta's
taxonomy) — the system does not perform marketing campaigns or bulk broadcasts.

Three integration paths exist in the current market: Meta's Cloud API accessed directly,
third-party Business Solution Providers (BSPs) that wrap Meta's API with added tooling, and
unofficial self-hosted solutions that reverse-engineer the WhatsApp protocol.

## Decision

We integrate directly with the **Meta WhatsApp Cloud API** without an intermediary BSP.

### How it works

The Cloud API is hosted entirely on Meta's servers. The IBMS backend sends HTTP requests to
Meta's Graph API endpoints to deliver template messages and receives inbound messages via
webhooks. No WhatsApp infrastructure runs on our servers — Meta handles hosting, scaling,
security patches, and protocol upgrades.

Access requires a Meta Business Account, a verified WhatsApp Business Account (WABA), and at
least one registered phone number. Template messages (the outbound notifications IBMS sends)
must be pre-approved by Meta before use.

### Pricing model

Since July 2025, Meta charges per delivered template message rather than per conversation
window. Pricing depends on three factors: message category (marketing, utility, authentication,
service), recipient country code, and message volume.

For Brazil in 2026:

- **Marketing:** ~$0.0625 per message (not applicable to IBMS use case)
- **Utility:** significantly cheaper — confirmations, updates, reminders
- **Service:** free within 24-hour window after customer-initiated contact
- **Free tier:** first 1,000 service conversations per month at no cost

IBMS sends utility messages (policy/claim notifications) and responds to customer-initiated
service conversations. At MVP volumes of 200–500 messages/month, with a significant portion
falling within free service windows, the projected monthly cost is near zero — likely under
$10/month in Meta fees with no platform subscription.

### Integration architecture

The WhatsApp sender is implemented behind a `NotificationChannel` interface, consistent with
the existing `RefreshTokenStore` pattern of abstracting external dependencies. The architecture:

```plaintext
Domain Event (e.g., PolicyActivated)
  → Redis Streams producer
    → Notification consumer
      → NotificationRouter
        → WhatsAppChannel (Meta Cloud API HTTP client)
        → EmailChannel (SMTP)
```

The `WhatsAppChannel` implementation uses Spring's `RestClient` to call Meta's Graph API.
Resilience4j provides circuit breaker and retry with exponential backoff. Failed deliveries
after 3 retries move to a dead letter stream inspectable via admin endpoint.

Template messages are stored as configuration (not hardcoded) and mapped to domain events:

| Domain Event    | Template Category | Template Purpose               |
| --------------- | ----------------- | ------------------------------ |
| PolicyActivated | utility           | Policy activation confirmation |
| PolicyRenewed   | utility           | Renewal confirmation           |
| PolicyCancelled | utility           | Cancellation notice            |
| QuoteConverted  | utility           | Quote-to-policy confirmation   |
| ClaimOpened     | utility           | Claim receipt acknowledgment   |
| ClaimClosed     | utility           | Claim resolution notification  |

### Why direct API over BSP

BSPs (Zenvia, Take Blip, Twilio, WATI, 360dialog) add value through no-code chatbot builders,
shared team inboxes, campaign management dashboards, and dedicated support. For IBMS, none of
these features are needed:

- No chatbot — messages are one-way transactional notifications
- No shared inbox — the system sends programmatically, not through human agents
- No campaign management — no marketing broadcasts
- No dashboard — the IBMS admin panel provides its own notification history

BSPs charge $15–50/month per number as a platform fee, plus some add a 10–20% markup on Meta's
per-message fees. For a system that sends 200–500 utility messages/month, paying $15–50/month
for features that go unused is waste.

The direct API requires developer effort to build the HTTP client, webhook handler, and template
management — effort that already exists in the IBMS architecture (RestClient, event-driven
design, admin endpoints).

### Why not unofficial APIs

Solutions like Evolution API and Z-API use reverse-engineered WhatsApp protocols. They offer
zero API cost and self-hosting, but carry fundamental risks:

- **Terms of Service violation:** Meta actively detects and bans numbers using unofficial APIs
- **No reliability guarantee:** protocol changes can break integration without notice
- **No template system:** messages may be flagged as spam without Meta's template approval flow
- **Liability:** a commercial system serving insurance brokers cannot depend on unauthorized access

These are categorically rejected for any production use.

## Consequences

### Positive

- Zero platform subscription cost — only Meta's per-message fees apply
- Full control over integration code, retry logic, and error handling
- No vendor lock-in to a BSP — switching to a BSP later is additive, not migratory
- Architecture aligns with existing patterns (interface abstraction, Redis Streams, Resilience4j)
- Template approval process enforces message quality and compliance

### Negative

- Higher initial development effort compared to BSP plug-and-play (~2–3 days additional)
- No dedicated WhatsApp support — Meta's developer support is community-based
- Webhook infrastructure must be maintained (SSL endpoint, signature verification)
- Template approval requires manual submission and may take up to 24 hours

### Neutral

- Meta requires a verified business identity — the brokerage client must provide documentation
- Phone number must be dedicated to API use (cannot simultaneously use WhatsApp Business App)

## Conditions for Reassessment

Reassess this decision if any of the following conditions are met:

1. **Volume exceeds 5,000 messages/month** — BSP volume discounts and delivery optimization
   may outweigh platform fees
2. **Two-way conversational support is needed** — shared inbox and chatbot features become
   relevant, justifying BSP tooling
3. **Marketing campaigns are introduced** — campaign management, segmentation, and analytics
   provided by BSPs add genuine value
4. **Meta deprecates direct Cloud API access** — unlikely but would force BSP adoption
5. **Delivery reliability issues** — if Meta's direct API shows worse deliverability than BSP
   routes in the Brazilian market

## Alternatives Considered

| Alternative                  | Reason for Rejection                                                                 |
| ---------------------------- | ------------------------------------------------------------------------------------ |
| Zenvia / Take Blip (BR BSPs) | $30–100/month platform fee for features IBMS doesn't need                            |
| Twilio                       | Enterprise-grade but expensive markup; overkill for transactional volume             |
| WATI                         | Strong in BR market but oriented toward team inbox / chatbot use cases               |
| 360dialog                    | Lightweight BSP, closest to direct API — viable fallback if reassessment triggers    |
| Evolution API (self-hosted)  | Unofficial protocol; ToS violation; unreliable for commercial use                    |
| Z-API                        | Same unofficial protocol risks as Evolution API                                      |
| No WhatsApp (email only)     | WhatsApp penetration in Brazil (~120M daily users) makes it essential for engagement |

## References

- Meta WhatsApp Cloud API Documentation: <https://developers.facebook.com/docs/whatsapp/cloud-api>
- Meta WhatsApp Business Platform Pricing: <https://developers.facebook.com/docs/whatsapp/pricing>
- WhatsApp Business Policy: <https://www.whatsapp.com/legal/business-policy>
- Meta Business Help Center — Template Guidelines: <https://www.facebook.com/business/help/2055875911147364>
- Resilience4j Documentation: <https://resilience4j.readme.io/>
