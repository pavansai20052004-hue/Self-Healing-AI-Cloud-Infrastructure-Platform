const { createProxyHandler } = require("../_proxy");

module.exports = createProxyHandler({
  backendUrlEnv: "RENDER_AI_URL",
  routePrefix: "/api/ai",
});
