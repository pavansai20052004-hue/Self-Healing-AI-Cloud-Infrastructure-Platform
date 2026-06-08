const { createProxyHandler } = require("../_proxy");

module.exports = createProxyHandler({
  backendUrlEnv: "RENDER_HEALING_URL",
});
