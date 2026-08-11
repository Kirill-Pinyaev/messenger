import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["src/lib/**/*.test.js"],
    coverage: {
      provider: "istanbul",
      include: ["src/lib/**/*.js"],
      exclude: ["src/lib/**/*.test.js"],
      reporter: ["text", "lcov", "html"],
      reportsDirectory: "./coverage",
      thresholds: {
        lines: 60,
        functions: 60,
      },
    },
  },
});
