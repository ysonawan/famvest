/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}"
  ],
  theme: {
    extend: {
      width: {
        '1/7': '14.2857143%' // ≈ 100 / 7
      },
      maxHeight: {
        'screen-content': 'calc(100vh - 280px)',
        'accounts': 'calc(100vh - 320px)',
      },
      animation: {
        'fade-in': 'fadeIn 0.2s ease-in-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: 0 },
          '100%': { opacity: 1 },
        },
      },
    }
  },
  plugins: [],
};

