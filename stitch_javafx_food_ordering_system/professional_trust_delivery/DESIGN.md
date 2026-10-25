---
name: Professional Trust Delivery
colors:
  surface: '#f9f9ff'
  surface-dim: '#cfdaf2'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f0f3ff'
  surface-container: '#e7eeff'
  surface-container-high: '#dee8ff'
  surface-container-highest: '#d8e3fb'
  on-surface: '#111c2d'
  on-surface-variant: '#424656'
  inverse-surface: '#263143'
  inverse-on-surface: '#ecf1ff'
  outline: '#727687'
  outline-variant: '#c2c6d8'
  surface-tint: '#0054d6'
  primary: '#0050cb'
  on-primary: '#ffffff'
  primary-container: '#0066ff'
  on-primary-container: '#f8f7ff'
  inverse-primary: '#b3c5ff'
  secondary: '#5d5f5f'
  on-secondary: '#ffffff'
  secondary-container: '#dfe0e0'
  on-secondary-container: '#616363'
  tertiary: '#565a5b'
  on-tertiary: '#ffffff'
  tertiary-container: '#6f7274'
  on-tertiary-container: '#f6f8fa'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#001849'
  on-primary-fixed-variant: '#003fa4'
  secondary-fixed: '#e2e2e2'
  secondary-fixed-dim: '#c6c6c7'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#454747'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#f9f9ff'
  on-background: '#111c2d'
  surface-variant: '#d8e3fb'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 16px
  md: 24px
  lg: 32px
  xl: 48px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style
The brand personality is rooted in reliability, efficiency, and safety. Unlike the typical playful or high-energy food delivery apps, this design system prioritizes a **Corporate Modern** aesthetic to evoke the feeling of a premium service that handles logistics with precision.

The UI should feel airy and professional, utilizing generous white space to reduce cognitive load. The emotional response is one of "calm assurance"—the user should feel certain that their order is being handled by a competent, systematic service.

## Colors
The palette is intentionally restrained to maintain a professional atmosphere. 

- **Primary (#0066FF):** A vibrant, high-contrast blue used for key actions and progress indicators. It represents movement and digital reliability.
- **Surface / Secondary (#FFFFFF):** The primary canvas. Use pure white for cards and containers to maximize the "clean" aesthetic.
- **Background / Tertiary (#F8FAFC):** A very light, cool gray used for the application background to create a subtle distinction between the page and elevated card elements.
- **Text / Neutral (#1E293B):** A deep charcoal navy for all primary copy to ensure optimal legibility and a sophisticated alternative to pure black.

## Typography
The system uses **Inter** exclusively to leverage its systematic, utilitarian, and highly readable qualities. 

- **Hierarchy:** Use bold weights for headlines to create a clear structural anchor.
- **Readability:** Body text should maintain a 1.5x line height to ensure menus and ingredient lists are easy to scan.
- **Labels:** Small labels (like delivery times or price categories) should use the medium or semibold weight to remain distinct even at small sizes.

## Layout & Spacing
The layout follows a **Fixed Grid** model for desktop (centered content) and a **Fluid Grid** for mobile devices.

- **Grid:** 12 columns for desktop, 4 columns for mobile.
- **Rhythm:** Use an 8px base unit for all spatial relationships. 16px is the standard padding for cards and list items.
- **Mobile Reflow:** On mobile, horizontal margins should never drop below 16px. Content cards should span the full width of the grid minus the margins.

## Elevation & Depth
Hierarchy is defined through **Tonal Layers** supplemented by **Ambient Shadows**.

- **Level 0 (Background):** #F8FAFC.
- **Level 1 (Cards/Surface):** #FFFFFF with a 1px border (#E2E8F0) and a soft, diffused shadow.
- **Shadow Style:** Use low-opacity shadows (Alpha: 0.04 to 0.08) with a large blur radius (12px - 24px) to avoid a "heavy" look.
- **Interaction:** Upon hover or active state, the shadow should slightly increase in spread, and the border color can shift toward the primary blue.

## Shapes
In alignment with the request for 12-16px corners, the system adopts a **Rounded (Level 2)** shape language.

- **Standard Elements:** Buttons, input fields, and small cards use a 0.5rem (8px) radius.
- **Large Containers:** Food item cards and modal overlays use a 1rem (16px) radius to soften the professional look and make it feel more approachable.
- **Iconography:** Use icons with rounded terminals to match the UI's geometry.

## Components
- **Buttons:** Primary buttons are solid #0066FF with white text. Secondary buttons use a #0066FF outline with a white background. Padding should be generous (12px vertical, 24px horizontal).
- **Input Fields:** Use a 1px border (#CBD5E1) that transitions to #0066FF on focus. Labels should be placed above the field in `label-md` style.
- **Cards:** Food cards must feature a high-quality image at the top with a 16px radius, followed by title and price in a white container below.
- **Chips:** Used for food categories (e.g., "Vegan", "Fast Delivery"). Use a light gray background (#F1F5F9) with `label-sm` text.
- **Lists:** Order history and menu items should be separated by thin 1px horizontal lines (#F1F5F9) rather than shadows to keep the list views clean.
- **Progress Trackers:** For delivery tracking, use a solid #0066FF line with circular nodes to represent a linear, professional logistics flow.