import test from "node:test";
import assert from "node:assert/strict";

import { buildGrpcWebOptions } from "./api.js";

test("buildGrpcWebOptions uses binary grpc-web encoding", () => {
  const interceptor = () => {};
  const options = buildGrpcWebOptions("http://localhost:8082", [interceptor]);

  assert.equal(options.baseUrl, "http://localhost:8082");
  assert.equal(options.useBinaryFormat, true);
  assert.equal(options.interceptors.length, 1);
  assert.equal(options.interceptors[0], interceptor);
});
