const { createProxyHandler } = require("../_proxy");

module.exports = createProxyHandler({
  backendUrlEnv: "RENDER_HEALING_URL",
  backendPathPrefix: "/api/v1/heal",
  routePrefix: "/api/healing",
});
