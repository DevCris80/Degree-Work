---
name: Verdant Acre
colors:
  surface: '#f3fced'
  surface-dim: '#d4ddcf'
  surface-bright: '#f3fced'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6e8'
  surface-container: '#e8f1e2'
  surface-container-high: '#e2ebdc'
  surface-container-highest: '#dce5d7'
  on-surface: '#161e15'
  on-surface-variant: '#40493a'
  inverse-surface: '#2a3329'
  inverse-on-surface: '#eaf4e5'
  outline: '#717a69'
  outline-variant: '#c0cab6'
  surface-tint: '#256d00'
  primary: '#246a00'
  on-primary: '#ffffff'
  primary-container: '#398516'
  on-primary-container: '#f8ffee'
  inverse-primary: '#89db63'
  secondary: '#49654a'
  on-secondary: '#ffffff'
  secondary-container: '#caebc8'
  on-secondary-container: '#4f6b50'
  tertiary: '#396700'
  on-tertiary: '#ffffff'
  tertiary-container: '#498300'
  on-tertiary-container: '#f9ffeb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a4f87c'
  primary-fixed-dim: '#89db63'
  on-primary-fixed: '#062100'
  on-primary-fixed-variant: '#1a5200'
  secondary-fixed: '#caebc8'
  secondary-fixed-dim: '#afcfad'
  on-secondary-fixed: '#06210b'
  on-secondary-fixed-variant: '#324d34'
  tertiary-fixed: '#b1f66d'
  tertiary-fixed-dim: '#96d954'
  on-tertiary-fixed: '#0e2000'
  on-tertiary-fixed-variant: '#2b5000'
  background: '#f3fced'
  on-background: '#161e15'
  surface-variant: '#dce5d7'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 34px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 26px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1.25rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is crafted for modern agricultural and farm management platforms. It bridges the tactical utility of rural fieldwork with the refined tactile charm of contemporary consumer software. The brand evokes vitality, organic balance, and grounded confidence. It makes complex agricultural workflows—such as parcel mapping, crop yields, livestock monitoring, and supply logistics—feel natural, intuitive, and visually refreshing.

The visual style blends **Modern Organic Tactile** design with high-contrast functional accessibility:
- **Warm Field-Ready Foundations:** Soft oat, linen, and warm cream surfaces replace sterile grays, keeping the eye relaxed under harsh direct sunlight.
- **Lush Botanical Gradients:** Radiant lime-to-forest greens provide focal energy for primary interactions, key statuses, and navigational anchors.
- **Pill Geometry & Soft Shapes:** Rounded card corners, pill-shaped action badges, and friendly contours evoke organic growth and approachable ergonomics.
- **Rural Field Ergonomics:** Generous tap targets (min 48px), prominent iconography, clear typography, and strong optical contrast ensure seamless operation with one hand outdoors.

## Colors

The palette balances lush chlorophyll greens with earthy neutrals and sun-warmed field backdrops.

- **Primary (`#4D9A2B`):** A fertile leaf green that serves as the visual core for active switches, primary indicators, and key focus states.
- **Secondary (`#203B23`):** Deep forest pine, providing deep contrast for high-priority active badges, top-level tags, and grounded UI structures.
- **Tertiary (`#82C341`):** Vivid sunlit lime, used in linear gradients alongside the primary green to generate luminous, dynamic call-to-action buttons and hero header surfaces.
- **Neutral (`#1C241B`):** Deep peat charcoal with an organic green undertone. Ensures WCAG AAA compliance on light backdrops while feeling softer than pure black.

### Supporting Semantic Surfaces & Tones
- **Canvas Base:** `#F6F7F0` (warm oat linen), pairing with `#FFFFFF` for elevated foreground cards.
- **Muted Green Tint:** `#E6F0DC` (fresh sprout tint) for subtle banner notifications, status tiles, and selected chip backgrounds.
- **Soil Accent:** `#A36F39` and `#D9822B` for crop, harvest, and storage contextual indicators.

## Typography

**Plus Jakarta Sans** provides geometric precision softened with open apertures and rounded terminals. This ensures exceptional legibility for farm hands checking irrigation or crop values while operating machinery or walking fields under daylight.

- **Headlines & Titles:** Set with weight 700 or 800 and subtle negative tracking (`-0.01em` to `-0.02em`) to produce a confident, modern editorial tone.
- **Body:** Open line heights (1.4–1.5x) eliminate visual crowding within dense data readouts like area hectares, crop yields, and sensor updates.
- **Labels & Badges:** Semi-bold to bold weights (`600` and `700`) maintain immediate recognizability across map markers, pills, and bottom navigation labels.

## Layout & Spacing

The layout is built on a mobile-first, fluid column structure designed to accommodate field maps, multi-zone dashboards, and fast form entry:

- **Mobile (Phone):** Single-column canvas with `margin-mobile: 1rem` and `gutter-mobile: 0.75rem`. Cards and map viewports span edge-to-margin for maximum visual area.
- **Tablet / Large Display:** Multi-column grid (4 to 8 columns) with `margin: 1.25rem` and `gutter: 1rem`, allowing split-screen zone maps and side-by-side livestock/crop detail panels.
- **Spacing Rhythm:** Standard 8pt linear rhythm. Micro spaces (`0.25rem`, `0.5rem`) cluster icons with labels; macro distances (`1rem`, `1.5rem`, `2rem`) frame independent sections and interactive form groups.

## Elevation & Depth

Visual hierarchy uses warm, diffuse ambient elevation and organic surface tiers rather than harsh drop shadows:

- **Surface Tiers:**
  - *Base Canvas:* Light oat-wash (`#F6F7F0`).
  - *Container Level 1 (Card Panels):* Solid pure white (`#FFFFFF`) with subtle soft perimeter rim (`border: 1px solid rgba(32, 59, 35, 0.05)`).
  - *Container Level 2 (Interactive Floating Modules):* Soft ambient blur with green-tinted cast (`box-shadow: 0 8px 24px -4px rgba(32, 59, 35, 0.08)`).
- **Hero Headers & Feature Panels:** Lush green gradient wash (linear gradient from `135deg`, `#2A5722` down to `#63A729`) providing deep, energetic atmospheric contrast at the top of forms or primary map viewports.
- **Floating Overlays:** Weather badges, map tool buttons, and floating quick actions leverage pure white fills with diffuse organic shadows (`0 4px 16px rgba(28, 36, 27, 0.10)`).

## Shapes

The shape system adopts a **Pill-shaped (Level 3)** design language, infusing UI components with friendly curves and ergonomic touch areas:

- **Pill Elements (`border-radius: 9999px`):** Used universally across primary CTA buttons, filter category chips, zone badges, search bars, and floating status bubbles.
- **Containers & Big Cards:** Large 24px (`rounded-xl` to `rounded-2xl`) corner radii provide an inviting frame for map viewports, crop stat blocks, and zone summary cards.
- **Text Inputs:** Soft rounded capsules (24px to 28px border radius) to maintain aesthetic cohesion with pill-shaped buttons.

## Components

### Buttons
- **Primary Action Buttons:** Pill-shaped (`rounded-full`), featuring a vibrant botanical linear gradient from `#2E5E22` to `#71B433` (or solid `#4D9A2B`). Text is pure white, bold 16px, with a minimum tap height of 52px. Hover/focus states bring an elevated green glow.
- **Secondary / Social Buttons:** Pill-shaped with off-white or soft warm tint (`#F0F3EB`), subtle border, and dark neutral typography (`#1C241B`).
- **Floating Action Buttons (FAB):** Circular (48px or 56px) white or dark forest capsules carrying bold navigation icons.

### Chips & Filter Tabs
- **Category Filter Chips:** Horizontal scrolling pill tags. Unselected: white or oat background with delicate 1px border (`#E3E7DE`), dark green icon, and text. Selected: dark evergreen fill (`#203B23`) with white text and vibrant green icon accents.
- **Status & Zone Badges:** Pill capsules with translucent light herbal backgrounds (`#E6F0DC`) and deep green text (`#203B23`).

### Form Inputs & Text Fields
- **Search & Text Inputs:** Generous 52px height, pill-shaped contour (`border-radius: 9999px`), light warm gray/oat fill (`#EFF2EB`), no harsh border when idle.
- **Focused State:** Thin, crisp accent boundary (`1.5px solid #4D9A2B`) with a soft sprout green outer glow.
- **Leading / Trailing Icons:** 20px stroked organic icons (envelopes, locks, search loupes, eye toggles) tinted in neutral leaf gray (`#6E7A6B`).

### Checkboxes & Switches
- **Checkboxes:** 22px rounded squares (6px radius) filled with primary leaf green (`#4D9A2B`) when checked, with a crisp white checkmark.
- **Toggle Switches:** Pill track with a soft bone thumb. Track turns vibrant lime-forest green on activation.

### Cards & Zone Modules
- **Overview & Field Cards:** Contained within 24px rounded white surfaces. Map zones feature clean boundary illustrations with overlay pill badges specifying zone name and crop type (e.g., "🍅 Tomatoes", "🌽 Corn").
- **Summary Banner:** Sprout-tinted background card (`#E6F0DC`) with green interactive hand icons, summary count chips, and rounded corners.

### Navigation
- **Bottom Navigation Bar:** Pure white elevated dock with a soft green top border shadow. Active tab displays an energetic circular or pill-shaped green badge with contrasting white icon and label.