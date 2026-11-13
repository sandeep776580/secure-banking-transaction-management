/** @type {import('tailwindcss').Config} */
export default { content: ['./index.html', './src/**/*.{ts,tsx}'], theme: { extend: { colors: { ink: '#11263f', muted: '#708197', brand: '#2155d6', mint: '#16a879' }, boxShadow: { card: '0 14px 40px rgba(22, 41, 71, .07)' }, fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui'] } } }, plugins: [] };
