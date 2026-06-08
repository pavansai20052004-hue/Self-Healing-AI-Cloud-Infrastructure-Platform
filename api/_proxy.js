function createProxyHandler({ backendUrlEnv, backendPathPrefix = "" }) {
  return async function proxyHandler(req, res) {
    if (req.method === "OPTIONS") {
      res.status(204).end();
      return;
    }

    const baseUrl = process.env[backendUrlEnv];
    if (!baseUrl) {
      res.status(502).json({
        error: `Missing environment variable ${backendUrlEnv}`,
      });
      return;
    }

    const pathSegments = normalizePathSegments(req.query.path);
    const target = new URL(baseUrl);
    const basePath = target.pathname.replace(/\/$/, "");
    const prefixPath = normalizeBasePath(backendPathPrefix);
    const extraPath = pathSegments.join("/");
    target.pathname = `${basePath}${prefixPath}${extraPath ? `/${extraPath}` : ""}` || "/";
    target.search = new URL(req.url, "http://localhost").search;

    const headers = new Headers();
    Object.entries(req.headers).forEach(([key, value]) => {
      if (value == null) {
        return;
      }
      const normalizedKey = key.toLowerCase();
      if (["host", "content-length", "connection"].includes(normalizedKey)) {
        return;
      }
      if (Array.isArray(value)) {
        headers.set(key, value.join(", "));
      } else {
        headers.set(key, value);
      }
    });

    const init = {
      method: req.method,
      headers,
    };

    if (!["GET", "HEAD"].includes(req.method || "")) {
      init.body = typeof req.body === "string" ? req.body : JSON.stringify(req.body ?? {});
    }

    try {
      const upstream = await fetch(target, init);
      const body = Buffer.from(await upstream.arrayBuffer());

      res.status(upstream.status);
      upstream.headers.forEach((value, key) => {
        if (!["content-length", "transfer-encoding", "connection", "content-encoding"].includes(key.toLowerCase())) {
          res.setHeader(key, value);
        }
      });
      res.send(body);
    } catch (error) {
      res.status(502).json({
        error: `Failed to proxy request to ${backendUrlEnv}`,
        detail: error.message,
      });
    }
  };
}

function normalizePathSegments(pathValue) {
  if (Array.isArray(pathValue)) {
    return pathValue.filter(Boolean).map((segment) => String(segment));
  }
  if (pathValue == null || pathValue === "") {
    return [];
  }
  return [String(pathValue)];
}

function normalizeBasePath(pathValue) {
  if (pathValue == null || pathValue === "") {
    return "";
  }
  const segments = String(pathValue)
    .split("/")
    .filter(Boolean);
  return segments.length > 0 ? `/${segments.join("/")}` : "";
}

module.exports = {
  createProxyHandler,
};
