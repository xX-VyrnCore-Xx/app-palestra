/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,jsx}", "./components/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        // Mirrors the Android app's brand palette (ui/theme/Color.kt) so the two apps read
        // as one product: orange as the energetic identity color, violet as the dark base.
        brand: {
          orange: "#FF7A1A",
          orangeDeep: "#E5590A",
          violet: {
            10: "#14101C",
            20: "#1E1828",
            90: "#F3EEFB",
          },
        },
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
