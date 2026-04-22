import test from "node:test";
import assert from "node:assert/strict";

import { Code, ConnectError } from "@connectrpc/connect";

import { loginFeedback } from "./auth-ui.js";

test("loginFeedback suggests registration when username is missing", () => {
  const feedback = loginFeedback(new ConnectError("profile not found", Code.NotFound));

  assert.equal(feedback.message, "Такого ника не существует. Зарегистрируйтесь.");
  assert.equal(feedback.showRegisterPrompt, true);
});

test("loginFeedback reports wrong password for unauthenticated error", () => {
  const feedback = loginFeedback(new ConnectError("invalid credentials", Code.Unauthenticated));

  assert.equal(feedback.message, "Неправильный пароль.");
  assert.equal(feedback.showRegisterPrompt, false);
});
