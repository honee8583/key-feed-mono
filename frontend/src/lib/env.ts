export const env = {
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080'),
    appEnv: import.meta.env.VITE_APP_ENV || 'development',
} as const;
