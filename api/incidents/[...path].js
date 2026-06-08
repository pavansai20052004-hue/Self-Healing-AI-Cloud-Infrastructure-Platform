const { createProxyHandler } = require("../_proxy");

module.exports = createProxyHandler({
  backendUrlEnv: "RENDER_INCIDENT_URL",
  backendPathPrefix: "/api/v1/incidents",
});
