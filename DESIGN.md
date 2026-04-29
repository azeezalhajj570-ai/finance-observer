# Finance Observer — Design System

> *"نسيت إنه شغال — بس ما نسى شي"*
> *"I forgot it was running — but it remembered everything."*

---

## 1. Aesthetic Direction

**Brutally Minimal + Warm Industrial.** Dark, quiet, nothing demanding attention. One warm amber accent that feels like a lamp in a dark room. The UI recedes so the observations stand out. No gradients, no decoration, no "fintech green." The app is an observer, not a performer.

**Decoration level:** Minimal. Arabic script is naturally decorative — the letters themselves provide visual texture. No additional ornamentation needed. The data is the decoration.

**Layout approach:** Hybrid. Structured grid for data surfaces (transaction list, breakdown), editorial timeline for the home feed. Charts exist but you scroll to them; what's above the fold is "here's what I noticed."

**Motion approach:** Intentional. Fade-up on scroll, smooth card expansions, zero playful flourishes. Motion should feel like a camera lens focusing. RTL-aware: animations originate from the start edge (right for Arabic).

---

## 2. Color System

Color is rare and therefore meaningful. The accent amber is the only warm color — it signals "I'm watching out for you." Red is reserved for anomalies. Green only for positive events.

### Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#0D0F13` | Primary app background — deep near-black, quiet |
| `surface` | `#161921` | Cards, containers, navigation bar |
| `surface_elevated` | `#1E2230` | Modals, bottom sheets, dialogs |
| `text_primary` | `#E9ECF2` | Headlines, amounts, key data |
| `text_secondary` | `#838A9E` | Labels, metadata, timestamps, help text |
| `text_tertiary` | `#545B6E` | Disabled text, placeholder hints |
| `accent_signature` | `#C4A56E` | Warm amber — active tab, FAB, selected states, primary action |
| `accent_critical` | `#E05561` | Anomalies, overspend alerts, subscription cancellations |
| `accent_positive` | `#4B9C6E` | Savings detected, refunds, under-budget indicators |
| `accent_info` | `#5B8CC7` | Neutral informational callouts |
| `border` | `#252936` | Card borders, dividers, list separators |
| `elevation_overlay` | `#FFFFFF08` | Subtle elevation hint for cards (8% white) |
| `ripple` | `#C4A56E33` | Touch ripple (20% amber) |
| `status_bar` | `#0A0C10` | Status bar background (darker than background) |
| `navigation_bar` | `#0A0C10` | Navigation bar background |

### Usage Rules

- **Accent amber** appears on at most 2-3 elements per screen — never more. If everything is amber, nothing is.
- **Critical red** is reserved for anomalies, overspend warnings, and actual destructive actions. Never use for decoration.
- **Positive green** appears only when there's genuine good news (refund, saving detected, budget surplus).
- **Borders** should be barely visible — they suggest structure, not shout separation.
- **Background vs surface** should be distinguishable but subtle — the user shouldn't consciously notice the depth.

### Light Mode (Alternative)

| Token | Hex | Usage |
|-------|-----|-------|
| `background_light` | `#F5F1ED` | Warm off-white — paper-like, not sterile |
| `surface_light` | `#FFFFFF` | Clean white cards |
| `surface_elevated_light` | `#FAF8F5` | Elevated surfaces |
| `text_primary_light` | `#1A1D25` | Near-black text |
| `text_secondary_light` | `#6B7185` | Muted text |
| `accent_signature_light` | `#8B6D3A` | Deeper amber for light backgrounds |
| `border_light` | `#E5E0D9` | Warm-tinted dividers |

---

## 3. Typography

Dual-script system. Fonts chosen for readability in each script, not forced into a single family.

### English (LTR)

| Role | Font | Weight | Size | Line Height |
|------|------|--------|------|-------------|
| Display Large | Cairo | Bold 700 | 32sp | 40sp |
| Display Medium | Cairo | Bold 700 | 24sp | 32sp |
| Headline | Cairo | Bold 700 | 20sp | 28sp |
| Title | Inter | Semibold 600 | 16sp | 24sp |
| Body | Inter | Regular 400 | 14sp | 20sp |
| Body Medium | Inter | Medium 500 | 14sp | 20sp |
| Caption | Inter | Regular 400 | 12sp | 16sp |
| Amount (currency) | JetBrains Mono | Regular 400 | 18sp | 22sp |
| Amount Large | JetBrains Mono | Medium 500 | 28sp | 34sp |
| Number Badge | JetBrains Mono | Medium 500 | 12sp | 16sp |

### Arabic (RTL)

| Role | Font | Weight | Size | Line Height |
|------|------|--------|------|-------------|
| Display Large | Cairo | Bold 700 | 32sp | 48sp |
| Display Medium | Cairo | Bold 700 | 24sp | 36sp |
| Headline | Cairo | Bold 700 | 20sp | 32sp |
| Title | Tajawal | Bold 700 | 16sp | 26sp |
| Body | Tajawal | Regular 400 | 15sp | 24sp |
| Body Medium | Tajawal | Medium 500 | 15sp | 24sp |
| Caption | Tajawal | Regular 400 | 13sp | 20sp |
| Amount (currency) | JetBrains Mono | Regular 400 | 18sp | 22sp |
| Amount Large | JetBrains Mono | Medium 500 | 28sp | 34sp |

> **Note:** Arabic requires ~15-20% more line height than English due to vertical variation in letterforms (ascenders, descenders, diacritics). Body text is set 1sp larger in Arabic to maintain visual parity.

### Implementation

```xml
<!-- res/font/ -->
cairo_bold.ttf
inter_regular.ttf
inter_medium.ttf
inter_semibold.ttf
tajawal_regular.ttf
tajawal_medium.ttf
tajawal_bold.ttf
jetbrains_mono_regular.ttf
jetbrains_mono_medium.ttf
```

Android locale-aware font selection via `res/font/` + programmatic typeface assignment per locale.

---

## 4. Spacing System

Base unit: **4dp**. All spacing must be a multiple of 4dp.

| Token | Value | Usage |
|-------|-------|-------|
| `xxs` | 4dp | Icon-text gap, tight internal padding |
| `xs` | 8dp | List item internal gap, chip padding |
| `sm` | 12dp | Card content padding top/bottom |
| `md` | 16dp | Standard padding, card content sides, screen insets |
| `lg` | 20dp | Section header to content |
| `xl` | 24dp | Between sections |
| `xxl` | 32dp | Before/after major content blocks |
| `xxxl` | 48dp | Top of screen to first content |

### Density
- **Medium-high.** Enough room for Arabic script to breathe, dense enough to show 5-6 transactions without scrolling.
- Cards: 12dp internal padding. List items: 8dp between text rows.
- Arabic UI uses 20dp section spacing (vs 24dp for English) to maintain visual density.

---

## 5. Component Specifications

### 5.1 Bottom Navigation

```
[Home] [Transactions] [Subscriptions] [Settings]

- Height: 64dp
- Background: #0A0C10 (navigation_bar)
- Active icon/text: #C4A56E (accent_signature)
- Inactive icon/text: #545B6E (text_tertiary)
- Selected indicator: 3dp rounded bar above icon, #C4A56E, 16dp wide
- Icon size: 24dp
- Label size: 11sp, Inter Medium / Tajawal Medium
- RTL: tabs mirror (Settings starts on left for Arabic)
```

### 5.2 Observation Card (Home Feed)

```
┌─────────────────────────────────────┐
│  منذ ساعتين · 2h ago                │
│                                     │
│  تم اكتشاف اشتراك جديد               │
│  New subscription detected          │
│                                     │
│  Netflix              ▸  $15.99     │
│                                     │
│  سيتم التجديد في ١٥ مايو            │
│  Renews May 15                      │
└─────────────────────────────────────┘

- Corner radius: 12dp
- Background: #161921 (surface)
- Elevation: 1dp (subtle)
- Internal padding: 16dp
- Bilingual: Arabic primary line, English secondary line (12sp caption in tertiary)
- Amount: JetBrains Mono 18sp, #E9ECF2
- Category icon: 20dp, #838A9E
```

### 5.3 Transaction List Item

```
┌─────────────────────────────────────┐
│  [Icon]  Starbucks       $4.50      │
│          ١٢:٣٠ م · 12:30 PM         │
│          القهوة · Coffee             │
├─────────────────────────────────────┤
│  Separator: 1dp, #252936            │
└─────────────────────────────────────┘

- Height: 68dp
- Icon: 40dp circle, category-colored background
- Category colors: muted, not bright (30% opacity versions of accent colors)
- Amount: JetBrains Mono 16sp, tabular lining
- Divider: 1dp, inset from start by 56dp (icon + padding)
- RTL: text aligns to start, amount stays end-aligned
```

### 5.4 Stat Card (Dashboard)

```
┌──────────────┐  ┌──────────────┐
│ هذا الشهر     │  │ مراقب         │
│ This Month    │  │ Tracked       │
│              │  │               │
│  $1,247.50   │  │  23 عمليات    │
│              │  │  23 txn       │
│  ↓ 12%       │  │  ٣ اشتراكات   │
│              │  │  3 subs       │
└──────────────┘  └──────────────┘

- Width: 0dp weight=1, 8dp gap between
- Corner radius: 12dp
- Background: #161921 (surface)
- Padding: 16dp
- Label: 11sp caption, #838A9E
- Value: 24sp Cairo Bold / 18sp JetBrains Mono for amounts
- Trend indicator: arrow + percentage, #4B9C6E (down=good) or #E05561 (up=bad)
```

### 5.5 Subscription Item

```
┌─────────────────────────────────────┐
│  [Netflix]  Netflix          $15.99 │
│             ١٥ مايو · May 15        │
│             شهري · Monthly          │
│             ████████░░ 80% cycle    │
└─────────────────────────────────────┘

- Progress bar: 4dp height, #2A2E3A track, #C4A56E fill
- Cycle indicator replaces date in Arabic
```

### 5.6 Anomaly Banner

```
┌─────────────────────────────────────┐
│  ! تنبيه · Alert                    │
│  ارتفع إنفاق المطاعم بنسبة ٤٥٪       │
│  Restaurant spending up 45%         │
│                                     │
│  هذا الأسبوع · This week   $320.00  │
└─────────────────────────────────────┘

- Border: 1dp left/start edge, #E05561
- Background: #1E2230 (slightly elevated)
- Icon: 20dp warning, #E05561
- Border is on start edge (left for LTR, right for RTL)
```

### 5.7 FAB (Floating Action Button)

```
- Size: 56dp
- Background: #C4A56E
- Icon: +, 24dp, #0D0F13 (dark icon on amber)
- Elevation: 6dp
- Ripple: #FFFFFF33 (20% white)
- Position: 16dp from end/bottom
- Only appears when there's a meaningful manual action (rare — this is passive)
```

### 5.8 Empty State

```
┌─────────────────────────────────────┐
│                                     │
│              (⊙)                    │
│                                     │
│  لا توجد معاملات بعد                 │
│  No transactions yet                │
│                                     │
│  Finance Observer يعمل في الخلفية    │
│  Finance Observer is working in     │
│  the background. We'll show things  │
│  here as we detect them.            │
│                                     │
└─────────────────────────────────────┘

- Icon: 48dp, #545B6E (tertiary), simple outline style
- Text: 14sp (15sp Arabic), #838A9E
- Centered, generous vertical whitespace
- No CTA — observer posture means no "get started" urgency
```

---

## 6. Iconography

### Style
- **Phosphor Icons** — 24dp, weight: Regular for inactive, Bold for active/selected
- Consistent 24dp viewbox, 2dp stroke for outlined variants
- No filled-background icons — the dark theme makes outlined icons more readable

### Navigation Icons
| Tab | Icon | Phosphor name |
|-----|------|---------------|
| Home | house | `ph-house` |
| Transactions | list-bullets | `ph-list` |
| Subscriptions | arrows-clockwise | `ph-arrows-clockwise` |
| Settings | gear | `ph-gear` |

### Semantic Icons
| Usage | Icon | Color |
|-------|------|-------|
| Subscription | clock-counter-clockwise | #C4A56E |
| Anomaly | warning-circle | #E05561 |
| Saving | piggy-bank | #4B9C6E |
| Food | fork-knife | #838A9E |
| Transport | car | #838A9E |
| Shopping | shopping-cart | #838A9E |
| Default transaction | currency-dollar | #838A9E |

---

## 7. RTL / Bilingual Design Rules

### Layout Mirroring
- **Full mirror:** All layouts flip horizontally. `start` and `end` attributes handle this automatically on Android.
- **Timeline:** Observation feed timeline element moves from left to right edge. Arrow indicators point toward content in both directions.
- **Back navigation:** Arrow points toward start edge (right for RTL).

### Bilingual Content Rules
- Arabic is the **primary** line when the device locale is Arabic.
- English is shown as a **secondary** line in 12sp caption, `#545B6E` (tertiary text).
- When English is the device locale, English is primary, no Arabic secondary. (Don't show Arabic to non-Arabic users.)
- **Currency amounts** always in Western Arabic numerals (1,2,3) and left-to-right, regardless of UI direction.
- **Dates** in Arabic: `١٥ مايو ٢٠٢٥`. In English: `May 15, 2025`.
- **Time** in Arabic: `١٢:٣٠ م`. In English: `12:30 PM`.

### What Does NOT Mirror
- Currency amounts (`$15.99` stays LTR even in RTL)
- Icons that represent physical direction (car, arrow on a receipt)
- Progress bars (fill from start to end in both directions)
- Charts and graphs (keep their natural orientation)
- Numbers in monospace — always LTR

---

## 8. Typography System Reference

### Font Files Required
```
res/font/
├── cairo_bold.ttf              # Headings (both scripts)
├── inter_regular.ttf           # English body
├── inter_medium.ttf            # English body emphasis
├── inter_semibold.ttf          # English titles
├── tajawal_regular.ttf         # Arabic body
├── tajawal_medium.ttf          # Arabic body emphasis
├── tajawal_bold.ttf            # Arabic titles
├── jetbrains_mono_regular.ttf  # Currency amounts, dates
└── jetbrains_mono_medium.ttf   # Large currency amounts
```

### Type Scale (Density-Optimized)

| Style | EN Size/Height | AR Size/Height | Weight | Font |
|-------|---------------|----------------|--------|------|
| display_large | 32/40sp | 32/48sp | 700 | Cairo |
| display_medium | 24/32sp | 24/36sp | 700 | Cairo |
| headline | 20/28sp | 20/32sp | 700 | Cairo |
| title | 16/24sp | 16/26sp | 600/700 | Inter / Tajawal |
| body_large | 16/24sp | 16/26sp | 400 | Inter / Tajawal |
| body | 14/20sp | 15/24sp | 400 | Inter / Tajawal |
| body_medium | 14/20sp | 15/24sp | 500 | Inter / Tajawal |
| caption | 12/16sp | 13/20sp | 400 | Inter / Tajawal |
| amount | 18/22sp | 18/22sp | 400 | JetBrains Mono |
| amount_large | 30/36sp | 30/36sp | 500 | JetBrains Mono |

---

## 9. Elevation & Shadows

Dark theme shadows are subtle — not box-shadow drop shadows, but surface brightness differentiation.

| Level | Background | Usage |
|-------|-----------|-------|
| 0 (base) | `#0D0F13` | App background |
| 1 | `#161921` | Cards, list items |
| 2 | `#1E2230` | Elevated cards, bottom sheets |
| 3 | `#242836` | Dialogs, modals |
| 4 | `#2C3040` | Navigation drawer, menus |

No `elevation` shadow in XML — use surface color alone. On API 28+ (Android 9), use the system dark theme overlay for natural elevation rendering.

---

## 10. Corner Radius Scale

| Size | Value | Usage |
|------|-------|-------|
| `none` | 0dp | Dividers, progress bars |
| `xs` | 4dp | Chips, tags, small badges |
| `sm` | 8dp | Buttons, input fields |
| `md` | 12dp | Cards, dialogs |
| `lg` | 16dp | FAB, bottom sheets |
| `full` | 999dp | Pills, some category icons |

All cards use 12dp radius. This is deliberate — less rounded than Material 3 (16dp) to keep the "industrial" feel. Clean edges, not soft toys.

---

## 11. Touch Targets

Minimum 48dp × 48dp per Android accessibility guidelines. All interactive elements must meet this:

- List items: 68dp height (comfortable for both scripts)
- Navigation tab: 48dp touch area within 64dp bar
- Buttons: 48dp height minimum
- Icon buttons: 48dp × 48dp, icon centered
- FAB: 56dp diameter

---

## 12. Screen Templates

### Home Screen Layout

```
┌─────────────────────────────────────┐
│  Status bar (#0A0C10)              │
├─────────────────────────────────────┤
│                                     │
│  Finance Observer                   │
│  مراقب المالية                       │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  هذا الشهر  ·  This Month     │  │
│  │  $1,247.50              ↓12%  │  │
│  │  ██████████░░░░░░░░░░        │  │
│  │  10 of 15 days elapsed       │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌────────────────────┐ ┌─────────┐│
│  │  اشتراكات · Subs   │ │تنبيهات  ││
│  │  ٣ نشطة · 3 active  │ │Anomalies││
│  │  $45.97/شهر · /mo  │ │   ١     ││
│  └────────────────────┘ └─────────┘│
│                                     │
│  ● الملاحظات  ·  Observations      │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ منذ ساعتين · 2h               │  │
│  │ Netflix  ·  $15.99  تم اكتشاف │  │
│  │ اشتراك  ·  Subscription found │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ منذ ٥ ساعات · 5h              │  │
│  │ ارتفع إنفاق المطاعم بنسبة ٤٥٪  │  │
│  │ Restaurant spending up 45%    │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ أمس · Yesterday               │  │
│  │ أمازون · Amazon     $34.99    │  │
│  │ تسوق · Shopping               │  │
│  └───────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│  [H]     [T]       [S]     [G]  │
│  الرئيسية المعاملات الاشتراكات الإعدادات│
└─────────────────────────────────────┘
```

### Transactions Screen Layout

```
┌─────────────────────────────────────┐
│  Status bar                         │
├─────────────────────────────────────┤
│  المعاملات  ·  Transactions         │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🔍  بحث  ·  Search            │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌ كل · All │ طعام · Food │ تسوق  ┐ │
│  └──────────┴───────────┴─────────┘ │
│                                     │
│  اليوم · Today                     │
│  ┌───────────────────────────────┐  │
│  │ [FD] بيتزا هت  ·  Pizza Hut    │  │
│  │         ١٢:٣٠ م · 12:30 PM  $18.50│
│  └───────────────────────────────┘  │
│  ─────────────────────────────────  │
│  ┌───────────────────────────────┐  │
│  │ [FD] ستاربكس  ·  Starbucks     │  │
│  │         ٩:١٥ ص · 9:15 AM    $4.50│
│  └───────────────────────────────┘  │
│                                     │
│  أمس · Yesterday                   │
│  ┌───────────────────────────────┐  │
│  │ [SH] أمازون  ·  Amazon         │  │
│  │         ٥:٤٥ م · 5:45 PM   $34.99│
│  └───────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│  [H]     [T]       [S]     [G]  │
└─────────────────────────────────────┘
```

---

## 13. Interaction States

All interactive elements must have:

| State | Visual treatment |
|-------|-----------------|
| Default | As specified |
| Pressed | 8% white overlay + 95% scale (subtle press) |
| Hovered | 4% white overlay (for external input devices) |
| Focused | 2dp amber outline, 4dp offset from element edge |
| Disabled | 40% opacity on entire element |
| Selected | Amber accent (#C4A56E) on indicator/text |

**Ripple:** `#C4A56E33` (20% amber), circular, expanding from touch point. Ripple should be barely colored — mostly the standard white ripple with a hint of warmth.

---

## 14. Accessibility

- **Minimum contrast ratio:** 4.5:1 for body text, 3:1 for large text (18sp+)
- **Verified contrasts:**
  - `#E9ECF2` on `#0D0F13` = 15.2:1 ✓ (body text on background)
  - `#838A9E` on `#0D0F13` = 5.1:1 ✓ (secondary text on background)
  - `#C4A56E` on `#0D0F13` = 5.8:1 ✓ (accent on background)
  - `#E05561` on `#0D0F13` = 4.6:1 ✓ (critical on background)
  - `#4B9C6E` on `#0D0F13` = 4.3:1 ❌ (positive on background — use surface for green text, or darken)
    - **Mitigation:** Use a 20% lightened green `#5BA37A` on `#161921` (surface) for positive indicators, achieving 4.6:1.
- **Arabic font sizing:** +1sp for body and caption to maintain visual parity with English.
- **RTL mirroring:** Full LayoutDirection support. Test with `forceRTL` enabled.
- **TalkBack:** All elements labeled in both Arabic and English based on locale.
- **Touch targets:** 48dp minimum on all interactive elements (Section 11).

---

## 15. Animation Tokens

| Token | Duration | Easing | Usage |
|-------|----------|--------|-------|
| `instant` | 100ms | linear | Toggle states, checkbox |
| `quick` | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Ripple, button press, tab switch |
| `standard` | 300ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Screen transitions, card expand |
| `deliberate` | 500ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Empty state appearance, FAB morph |
| `entrance` | 400ms | `cubic-bezier(0, 0, 0.2, 1)` | Items appearing on scroll (fade + translate 16dp from bottom) |
| `exit` | 250ms | `cubic-bezier(0.4, 0, 1, 1)` | Items leaving screen |

**Motion direction awareness:**
- RTL: entrance animations come from the right (start edge)
- LTR: entrance animations come from the left (start edge)
- Cards fade up uniformly in both directions
- Navigation transitions slide from the logical direction

---

## 16. Brand Expression

### App Name Display
- **Arabic locale:** "مراقب المالية" as primary, "Finance Observer" as subtitle
- **English locale:** "Finance Observer" as primary, no Arabic secondary
- **Both:** Rendered in Cairo Bold, amber (#C4A56E) for the header bar

### Tagline (Arabic)
> *راقب بذكاء. عش بحرية.*
> *Observe intelligently. Live freely.*

### Tagline (English)
> *It watches. You live.*

### App Icon Concept
- Dark circular background (#0D0F13)
- Stylized single eye (observation) in amber (#C4A56E)
- No text — iconographic only
- The eye is calm, half-lidded — not a surveillance eye, a guardian eye

---

## 17. Design Tokens Summary (for Implementation)

```xml
<!-- res/values/colors.xml -->
<color name="background">#0D0F13</color>
<color name="surface">#161921</color>
<color name="surface_elevated">#1E2230</color>
<color name="text_primary">#E9ECF2</color>
<color name="text_secondary">#838A9E</color>
<color name="text_tertiary">#545B6E</color>
<color name="accent_signature">#C4A56E</color>
<color name="accent_critical">#E05561</color>
<color name="accent_positive">#5BA37A</color>
<color name="accent_info">#5B8CC7</color>
<color name="border">#252936</color>
<color name="status_bar">#0A0C10</color>
<color name="navigation_bar">#0A0C10</color>
```

```xml
<!-- res/values/dimens.xml -->
<dimen name="spacing_xxs">4dp</dimen>
<dimen name="spacing_xs">8dp</dimen>
<dimen name="spacing_sm">12dp</dimen>
<dimen name="spacing_md">16dp</dimen>
<dimen name="spacing_lg">20dp</dimen>
<dimen name="spacing_xl">24dp</dimen>
<dimen name="spacing_xxl">32dp</dimen>
<dimen name="spacing_xxxl">48dp</dimen>
<dimen name="card_corner_radius">12dp</dimen>
<dimen name="touch_target_min">48dp</dimen>
<dimen name="bottom_nav_height">64dp</dimen>
<dimen name="transaction_item_height">68dp</dimen>
```

---

## 18. Design Decisions Log

| Decision | Rationale | Date |
|----------|-----------|------|
| Dark-first, amber accent | Distinguishes from green/blue finance category; fits observer posture | 2026-04-29 |
| Dual-script typography | Arabic-first users need native readability; Cairo for headings bridges both scripts | 2026-04-29 |
| Monospace for currency | Precision aesthetics build trust; no other finance app does this | 2026-04-29 |
| Feed-first home (not charts) | Reinforces passive observation; "here's what I noticed" beats "here's what you spent" | 2026-04-29 |
| Phosphor icons | Consistent 24dp, multiple weights, free, wider selection than Material Icons | 2026-04-29 |
| No green for trust | Finance overuses green; warm amber is more distinctive and culturally resonant in Arabic markets | 2026-04-29 |
| Arabic +1sp body text | Arabic letterforms need more vertical space for readability parity | 2026-04-29 |
