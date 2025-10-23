# AI Frontend Prompt: Sales Invoice Detail & Allocation

**Generated:** 2025-10-16
**Project:** accounting_erp_rag
**Screen:** Sales Invoice Detail & Allocation (Single Page)
**Optimized for:** Vercel v0, Lovable.ai, Cursor AI, and similar AI code generation tools

---

## 🎯 What to Build

Generate a responsive single-page layout for the **Sales Invoice Detail & Allocation** experience inside the accounting ERP + RAG assistant platform. The page must surface invoice metadata, approval status, line items, attachments, audit trail, allocation guidance, and contextual AI insights in both Vietnamese and English.

Primary users:
- **Lan (Senior Accountant):** enters and maintains invoices, resolves allocations quickly.
- **Minh (Finance Director):** reviews status and SLA breaches, exports evidence.
- **Auditors:** read-only access to activity timeline and supporting docs.

Key goals:
1. Present authoritative invoice information with multi-currency support and compliance guardrails.
2. Provide allocation workspace with AI-assisted suggestions and escalation cues when approvals stall.
3. Maintain auditability via timeline, citations, and download packages.

---

## 🎨 Design System Snapshot

### Color Tokens (shadcn/ui + finance extensions)
```
const colors = {
  primary: '#0F3D3E',        // Deep Teal – trust anchor
  secondary: '#F5A623',      // Golden Amber – approvals & highlights
  accent: '#2ED1C2',         // Electric Aqua – AI & realtime signals
  surface: '#F7FAFC',        // Mist White – background
  surfaceDark: '#1A202C',    // Midnight Navy – dark mode base
  text: {
    default: '#1F2933',
    subdued: '#4A5568',
    inverse: '#F7FAFC',
  },
  semantic: {
    success: '#2F855A',
    warning: '#D69E2E',
    critical: '#C53030',
    info: '#3182CE',
  },
};
```

### Typography
- **Primary UI:** `Inter`, 16px body, support Vietnamese diacritics.
- **Narrative/assistant:** `Source Serif Pro` for AI-generated explanations.
- Type scale: Display 32/40, H1 26/32, H2 22/28, H3 18/24, Body 16/22, Dense 14/20, Caption 12/18.

### Spacing & Layout
- 12-column grid ≥1280px, 8-column tablet, 4-column mobile.
- Base spacing 4px; common steps 8, 12, 16, 24, 32.
- Drawer widths: 480px desktop, full-width overlay mobile.
- Motion: 150–300 ms transitions; respect reduced-motion preference.

---

## 🧩 Required Page Regions

1. **Header Band (sticky)**
   - Invoice status chip (color-coded) + SLA countdown and escalation banner when overdue.
   - Invoice meta in bilingual format: Invoice ID, Customer name, Issue/Due dates, Currency badge (e.g., VND ⇄ USD), Total + Outstanding amounts.
   - Primary actions: `Approve`, `Request Changes`, `Export PDF`, `Generate Evidence Pack`.

2. **Tabbed Workspace (main content)**
   - Tabs: `Summary`, `Line Items`, `Attachments`, `Audit Trail` (shadcn Tabs component).
   - Each tab retains scroll position; highlight active tab with teal underline.

3. **Summary Tab**
   - `SummaryCard` grid with totals, tax breakdown, payment schedule.
   - `ApprovalBanner` showing approver avatars, due-by timestamps, escalation CTA.
   - `AIInsightCard` presenting assistant recommendation (e.g., “Payment partially matched with Receipt #RC-2045”) with citations.

4. **Line Items Tab**
   - Ledger-style table (sticky header) with columns: Item, Account, Quantity, Unit Price, Tax %, Amount, Allocation Status.
   - Inline edit rows with keyboard shortcut hints (`Shift+Enter` to save line).
   - Multi-currency badge per row when different from base currency.

5. **Attachments Tab**
   - File cards with preview (PDF, image), description, upload timestamp, and download button.
   - Split view for drag/drop upload (development note: use placeholder logic).

6. **Audit Trail Tab**
   - Vertical timeline with timestamps, actor avatar, action summary, AI narrative overlays.
   - Provide `Copy Hash` button and CITED evidence details.

7. **Right Sidebar (persistent)**
   - `AllocationSuggestions` list with confidence badges (High/Medium/Low) and `Apply` buttons.
   - `Related Records` (receipts, disputes) with status pills.
   - `AssistantQuickActions` chips (e.g., “Explain overdue balance”).
   - Escalation alert when approval SLA breached.

8. **Footer Metrics Row (optional)**
   - Payment plan progress bar, outstanding aging buckets with colors from semantic set.

---

## 🔄 States & Interactions

- **Status Chips:** Pending (amber), Approved (teal), Rejected (critical), Escalated (amber with icon).
- **SLA Countdown:** Timer turns amber when <24h and red when overdue.
- **Inline Editing:** Activate row via `Enter`, confirm `Shift+Enter`, cancel `Esc`. Validation errors show inline with red border + helper text.
- **AI Insight:** Display citation list (document ID, page). Include `View Source` button.
- **Allocation Apply:** Clicking `Apply` animates transfer of suggestion into ledger row; show toast with undo option.
- **Attachment Upload:** Simulate progress bar; support re-ordering.
- **Timeline Filter:** Provide filter chips (Approvals, Edits, AI) with accessible toggles.

---

## 📱 Responsive Behavior

- **Desktop ≥1280px:** Tabs + sidebar side-by-side (8/4 split). Header fixed. Footer metrics visible.
- **Tablet 768–1279px:** Sidebar collapses to icon buttons; opens as drawer. Tabs occupy full width.
- **Mobile ≤767px:** Header condenses; tabs convert to accordion. Sidebar content becomes slide-over triggered by `Insight & Actions` button. Ensure approve/decline buttons remain accessible.

---

## ♿ Accessibility Mandates

- WCAG 2.1 AA targets; 100% keyboard operable.
- Visible focus states (teal outline) on all interactive elements including table rows and timeline items.
- Provide aria-live updates for SLA countdown and AI insight updates.
- All icons require aria-labels; bilingual labels must preserve meaning in VI/EN.
- Reduced-motion preference disables shimmer animations and uses instant fades.

---

## 📊 Sample Data (use in mock)

```
const invoice = {
  id: 'INV-2024-1098',
  status: 'Pending Approval',
  customer: {
    name_en: 'Saigon Logistics Co.',
    name_vn: 'Công ty Vận tải Sài Gòn',
    taxId: '0301234567',
  },
  currency: 'VND',
  total: 285_450_000,
  outstanding: 85_450_000,
  issueDate: '2024-09-05',
  dueDate: '2024-10-05',
  exchangeRate: 0.000041,
  slaHoursRemaining: 18,
};

const lineItems = [
  {
    id: 'LI-01',
    description: 'Freight forwarding service',
    account: '5111 - Service Revenue',
    qty: 1,
    unitPrice: 200_000_000,
    taxRate: 10,
    currency: 'VND',
    status: 'Matched',
  },
  {
    id: 'LI-02',
    description: 'Customs handling (USD)',
    account: '1311 - Accounts Receivable',
    qty: 1,
    unitPrice: 1_800,
    taxRate: 0,
    currency: 'USD',
    status: 'Pending Allocation',
  },
];

const allocationSuggestions = [
  { id: 'AL-01', text: 'Apply Receipt RC-2045 (80,000,000 VND) for partial settlement', confidence: 'high' },
  { id: 'AL-02', text: 'Recommend review dispute DP-889 due to unmatched rate', confidence: 'medium' },
];

const auditTrail = [
  { id: 1, timestamp: '2024-09-06 09:24', actor: 'Lan Nguyen', action: 'Created invoice draft', channel: 'UI', evidence: 'hash-123' },
  { id: 2, timestamp: '2024-09-06 09:31', actor: 'Automated Check', action: 'VAT compliance pass', channel: 'System', evidence: 'hash-456' },
  { id: 3, timestamp: '2024-09-07 14:02', actor: 'AI Assistant', action: 'Suggested allocation from RC-2045', channel: 'AI', evidence: 'hash-789' },
];
```

---

## ✅ Acceptance Checklist

- Layout includes header band, tabbed workspace, right sidebar, footer metrics.
- shadcn/ui primitives (Tabs, Card, Table, Badge, Sheet, Alert, Tooltip, Toast) extended per guidelines.
- All buttons and chips bilingual-ready (props for `label_en`/`label_vn`).
- States for Pending, Approved, Rejected, Escalated clearly reflected.
- Responsive behaviors implemented per breakpoints.
- Accessibility: keyboard navigation, aria attributes, reduced motion support.
- Provide clear comments for finance-specific behaviors (SLA countdown, currency badges, allocation apply).

---

## 🔁 Follow-up

After generation, handoff should include:
- Implementation notes for connecting to Supabase tokens.
- To-do markers where backend integrations (receipts, AI citations) are expected.
- Suggestions for QA to validate localization, accessibility, and audit downloads.

**Generated by:** BMAD UX Workflow (ux-spec) – Step 11
