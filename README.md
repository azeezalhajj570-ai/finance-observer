# Finance Observer - Android Financial Assistant

A passive mobile financial assistant that observes bank app notifications, payment app alerts, and SMS receipts to automatically track spending, detect subscriptions, and flag anomalies.

## Architecture

```
┌─────────────────────────────────────────────────┐
│              MOBILE FINANCIAL OBSERVER           │
│                                                  │
│  ┌──────────────┐    ┌──────────────────────┐   │
│  │ Notification  │    │     SMS Receiver     │   │
│  │   Listener    │    │   (BroadcastReceiver)│   │
│  └──────┬───────┘    └──────────┬───────────┘   │
│         │                       │                │
│         ▼                       ▼                │
│  ┌──────────────────────────────────────────┐   │
│  │          Parser Registry                  │   │
│  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌───────────┐  │   │
│  │  │Chase│ │Venmo│ │PayPal│ │ Generic   │  │   │
│  │  └─────┘ └─────┘ └─────┘ └───────────┘  │   │
│  └──────────────────┬───────────────────────┘   │
│                     │                            │
│                     ▼                            │
│  ┌──────────────────────────────────────────┐   │
│  │     Dedup + Normalize + Store (Room)     │   │
│  └──────────────────┬───────────────────────┘   │
│                     │                            │
│            ┌────────┼────────┐                  │
│            ▼        ▼        ▼                  │
│  ┌──────────┐ ┌────────┐ ┌──────────┐         │
│  │Subscription│ │Anomaly│ │ Cash Flow│         │
│  │  Radar    │ │Detector│ │ Forecast │         │
│  └──────────┘ └────────┘ └──────────┘         │
│                     │                            │
│                     ▼                            │
│  ┌──────────────────────────────────────────┐   │
│  │         Dashboard UI                      │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

## Features

### MVP (Current)
- **Notification Listener**: Captures payment notifications from bank apps, Venmo, PayPal, etc.
- **SMS Receiver**: Reads payment SMS receipts automatically
- **Parser Registry**: Pluggable parser system with 10+ built-in parsers (Chase, BofA, Venmo, PayPal, Google Pay, Apple Pay, Cash App, Zelle, Stripe, Square) + generic fallback
- **Deduplication**: Prevents duplicate transactions from notification + SMS
- **Subscription Radar**: Detects recurring charges automatically
- **Anomaly Detection**: Flags unusual spending patterns

### Planned
- Cash Flow Forecast
- Split Detection
- Real-time purchase intervention
- Price Protection

## Privacy

- **On-device processing**: All data stays on your phone
- **No cloud sync**: Financial data never leaves the device
- **Encrypted storage**: SQLCipher for database encryption (production)
- **No tracking**: Zero analytics or telemetry

## Building

```bash
./gradlew assembleDebug
```

## Testing

```bash
./gradlew test
```

## Parser Coverage

| App | Notifications | SMS | Priority |
|-----|--------------|-----|----------|
| Chase | ✅ | ✅ | 90 |
| Bank of America | ✅ | ✅ | 88 |
| Venmo | ✅ | ✅ | 85 |
| PayPal | ✅ | ✅ | 85 |
| Google Pay | ✅ | ✅ | 85 |
| Cash App | ✅ | ✅ | 85 |
| Apple Pay | ✅ | ✅ | 80 |
| Zelle | ✅ | ✅ | 80 |
| Stripe | ✅ | ✅ | 75 |
| Square | ✅ | ✅ | 75 |
| Generic (fallback) | ✅ | ✅ | 0 |

## License

MIT
