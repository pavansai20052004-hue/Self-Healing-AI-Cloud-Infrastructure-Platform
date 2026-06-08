const { createProxyHandler } = require("../_proxy");

module.exports = createProxyHandler({
  backendUrlEnv: "RENDER_MONITORING_URL",
  backendPathPrefix: "/api/v1",
  routePrefix: "/api/monitoring",
});
