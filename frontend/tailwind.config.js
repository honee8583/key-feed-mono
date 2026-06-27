/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // Cohere design-system tokens (from Claude Design "디자인 시스템 구축")
      colors: {
        ink: '#212121',
        primary: '#17171c',
        'cohere-black': '#000000',
        'deep-green': '#003c33',
        'dark-navy': '#071829',
        canvas: '#ffffff',
        'soft-stone': '#eeece7',
        'pale-green': '#edfce9',
        'pale-blue': '#f1f5ff',
        hairline: '#d9d9dd',
        muted: '#93939f',
        'action-blue': '#1863dc',
        'focus-blue': '#4c6ee6',
        coral: '#ff7759',
        'coral-soft': '#ffad9b',
        'form-focus': '#9b60aa',
        'brand-error': '#b30000',
      },
      fontFamily: {
        // Display headlines/wordmark. Mono eyebrows/labels. Pretendard for KR body.
        display: ['"Space Grotesk"', '"Pretendard"', 'Inter', 'ui-sans-serif', 'sans-serif'],
        mono: ['"Space Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
        pretendard: ['"Pretendard"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        tightest: '-0.03em',
        'mono-label': '0.7px',
      },
    },
  },
  plugins: [],
}

